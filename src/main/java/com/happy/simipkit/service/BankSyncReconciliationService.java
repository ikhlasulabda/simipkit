package com.happy.simipkit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.happy.simipkit.exception.InsufficientFundsException;
import com.happy.simipkit.model.Client;
import com.happy.simipkit.model.PortfolioAsset;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BankSyncReconciliationService {

    private static final Logger logger = LogManager.getLogger(BankSyncReconciliationService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ClientService clientService;
    private final PortfolioService portfolioService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public BankSyncReconciliationService(JdbcTemplate jdbcTemplate,
                                         ClientService clientService,
                                         PortfolioService portfolioService,
                                         AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.clientService = clientService;
        this.portfolioService = portfolioService;
        this.auditLogService = auditLogService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Match bank sync event ke data client berdasarkan nomor rekening di raw JSON.
     * Menggunakan manual JSON tree parsing (readTree) tanpa polymorphic deserialization.
     */
    public Map<String, Object> matchEvent(int eventId, Integer userId, String ipAddress) throws Exception {
        String selectSql = "SELECT id, event_type, payload_raw, status, reconciliation_status, matched_client_id, matched_client_id_secondary FROM bank_sync_events WHERE id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql, eventId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Event ID " + eventId + " tidak ditemukan.");
        }

        Map<String, Object> eventRow = rows.get(0);
        String payloadRaw = (String) eventRow.get("payload_raw");

        JsonNode rootNode = objectMapper.readTree(payloadRaw);
        String eventClass = rootNode.has("@class") ? rootNode.get("@class").asText() : "";

        Client clientPrimary = null;
        Client clientSecondary = null;
        String reqPrimary = null;
        String reqSecondary = null;

        if (eventClass.contains("SaldoUpdateEvent") || eventClass.contains("SettlementEvent")) {
            if (rootNode.has("nomorRekening")) {
                reqPrimary = rootNode.get("nomorRekening").asText();
                clientPrimary = clientService.getClientByNomorRekening(reqPrimary);
            }
        } else if (eventClass.contains("TransferConfirmationEvent")) {
            if (rootNode.has("nomorRekeningPengirim")) {
                reqPrimary = rootNode.get("nomorRekeningPengirim").asText();
                clientPrimary = clientService.getClientByNomorRekening(reqPrimary);
            }
            if (rootNode.has("nomorRekeningTujuan")) {
                reqSecondary = rootNode.get("nomorRekeningTujuan").asText();
                clientSecondary = clientService.getClientByNomorRekening(reqSecondary);
            }
        } else {
            // Fallback generic check
            if (rootNode.has("nomorRekening")) {
                reqPrimary = rootNode.get("nomorRekening").asText();
                clientPrimary = clientService.getClientByNomorRekening(reqPrimary);
            }
        }

        boolean matched = (clientPrimary != null || clientSecondary != null);
        String newReconStatus = matched ? "MATCHED" : "FAILED";
        String primaryId = clientPrimary != null ? clientPrimary.getId() : null;
        String secondaryId = clientSecondary != null ? clientSecondary.getId() : null;

        String updateSql = "UPDATE bank_sync_events SET reconciliation_status = ?, matched_client_id = ?, matched_client_id_secondary = ? WHERE id = ?";
        jdbcTemplate.update(updateSql, newReconStatus, primaryId, secondaryId, eventId);

        auditLogService.logAction(userId, "BANK_SYNC_MATCH_ATTEMPT", ipAddress,
                "Match attempt event ID: " + eventId + " -> Status: " + newReconStatus +
                (matched ? " (Primary: " + primaryId + ", Secondary: " + secondaryId + ")" : " (Rekening tidak ditemukan)"));

        Map<String, Object> result = new HashMap<>();
        result.put("eventId", eventId);
        result.put("eventClass", eventClass);
        result.put("reconciliationStatus", newReconStatus);

        if (clientPrimary != null) {
            Map<String, Object> pMap = buildClientSummaryMap(clientPrimary);
            result.put("clientPrimary", pMap);
        } else if (reqPrimary != null) {
            result.put("unmatchedRekeningPrimary", reqPrimary);
        }

        if (clientSecondary != null) {
            Map<String, Object> sMap = buildClientSummaryMap(clientSecondary);
            result.put("clientSecondary", sMap);
        } else if (reqSecondary != null) {
            result.put("unmatchedRekeningSecondary", reqSecondary);
        }

        // Preview dampak khusus
        if (eventClass.contains("SaldoUpdateEvent") && rootNode.has("saldoBaru")) {
            result.put("previewMessage", "Saldo RDN akan diperbarui menjadi " + rootNode.get("saldoBaru").asDouble());
        } else if (eventClass.contains("SettlementEvent")) {
            String kode = rootNode.has("kodeInstrumen") ? rootNode.get("kodeInstrumen").asText() : "-";
            double unit = rootNode.has("jumlahUnit") ? rootNode.get("jumlahUnit").asDouble() : 0;
            double harga = rootNode.has("hargaSettlement") ? rootNode.get("hargaSettlement").asDouble() : 0;
            double totalBiaya = unit * harga;

            result.put("kodeInstrumen", kode);
            result.put("jumlahUnit", unit);
            result.put("hargaSettlement", harga);
            result.put("totalBiayaSettlement", totalBiaya);

            if (clientPrimary != null) {
                double saldoRdn = clientPrimary.getSaldoRdn();
                result.put("saldoRdnSaatIni", saldoRdn);
                result.put("isSaldoCukup", saldoRdn >= totalBiaya);
                result.put("previewMessage", "Akan menambahkan " + unit + " unit " + kode +
                        " ke portofolio & memotong saldo RDN sebesar Rp " + totalBiaya);

                List<PortfolioAsset> assets = portfolioService.getAssetsByClientId(clientPrimary.getId());
                PortfolioAsset existing = assets.stream().filter(a -> kode.equalsIgnoreCase(a.getNamaInstrumen())).findFirst().orElse(null);
                if (existing != null) {
                    result.put("existingAssetJumlah", existing.getJumlah());
                    result.put("existingAssetNilai", existing.getNilai());
                }
            } else {
                result.put("previewMessage", "Akan menambahkan / meng-update " + unit + " unit " + kode + " (total biaya Rp " + totalBiaya + ") ke portofolio");
            }
        } else if (eventClass.contains("TransferConfirmationEvent")) {
            double jumlah = rootNode.has("jumlahTransfer") ? rootNode.get("jumlahTransfer").asDouble() : 0;
            String statusTf = rootNode.has("statusTransfer") ? rootNode.get("statusTransfer").asText() : "SUCCESS";
            result.put("jumlahTransfer", jumlah);
            result.put("statusTransfer", statusTf);
        }

        return result;
    }

    /**
     * Eksekusi sinkronisasi finansial / portofolio untuk event yang berstatus MATCHED.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncEvent(int eventId, Integer userId, String ipAddress) throws Exception {
        String selectSql = "SELECT id, payload_raw, reconciliation_status, matched_client_id, matched_client_id_secondary FROM bank_sync_events WHERE id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql, eventId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Event ID " + eventId + " tidak ditemukan.");
        }

        Map<String, Object> eventRow = rows.get(0);
        String currentStatus = (String) eventRow.get("reconciliation_status");

        if ("SYNCED".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Event ID " + eventId + " sudah tersinkronisasi sebelumnya.");
        }

        if (!"MATCHED".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Event ID " + eventId + " belum dicocokkan (status saat ini: " + currentStatus + ").");
        }

        String payloadRaw = (String) eventRow.get("payload_raw");
        String primaryClientId = (String) eventRow.get("matched_client_id");
        String secondaryClientId = (String) eventRow.get("matched_client_id_secondary");

        JsonNode rootNode = objectMapper.readTree(payloadRaw);
        String eventClass = rootNode.has("@class") ? rootNode.get("@class").asText() : "";
        String syncLogDetail = "";

        if (eventClass.contains("SaldoUpdateEvent")) {
            if (primaryClientId == null) {
                throw new IllegalStateException("Klien utama tidak ditemukan untuk SaldoUpdateEvent.");
            }
            double saldoBaru = rootNode.has("saldoBaru") ? rootNode.get("saldoBaru").asDouble() : 0.0;
            String updateClientSql = "UPDATE clients SET saldo_rdn = ? WHERE id = ?";
            jdbcTemplate.update(updateClientSql, saldoBaru, primaryClientId);
            syncLogDetail = "Saldo RDN client " + primaryClientId + " diperbarui menjadi Rp " + saldoBaru;

        } else if (eventClass.contains("TransferConfirmationEvent")) {
            String statusTransfer = rootNode.has("statusTransfer") ? rootNode.get("statusTransfer").asText() : "SUCCESS";
            double jumlahTransfer = rootNode.has("jumlahTransfer") ? rootNode.get("jumlahTransfer").asDouble() : 0.0;

            if ("SUCCESS".equalsIgnoreCase(statusTransfer)) {
                // Pengirim (primary): saldo_rdn berkurang
                if (primaryClientId != null) {
                    String deductSql = "UPDATE clients SET saldo_rdn = saldo_rdn - ? WHERE id = ?";
                    jdbcTemplate.update(deductSql, jumlahTransfer, primaryClientId);
                }
                // Penerima/tujuan (secondary): saldo_rdn bertambah
                if (secondaryClientId != null) {
                    String addSql = "UPDATE clients SET saldo_rdn = saldo_rdn + ? WHERE id = ?";
                    jdbcTemplate.update(addSql, jumlahTransfer, secondaryClientId);
                }
                syncLogDetail = "Transfer SUCCESS processed: Rp " + jumlahTransfer +
                        " (Pengirim: " + (primaryClientId != null ? primaryClientId : "Luar") +
                        ", Tujuan: " + (secondaryClientId != null ? secondaryClientId : "Luar") + ")";
            } else {
                syncLogDetail = "Transfer status " + statusTransfer + ": Tidak ada perubahan saldo RDN.";
            }

        } else if (eventClass.contains("SettlementEvent")) {
            if (primaryClientId == null) {
                throw new IllegalStateException("Klien utama tidak ditemukan untuk SettlementEvent.");
            }
            String kodeInstrumen = rootNode.has("kodeInstrumen") ? rootNode.get("kodeInstrumen").asText() : "UNKNOWN";
            double jumlahUnit = rootNode.has("jumlahUnit") ? rootNode.get("jumlahUnit").asDouble() : 0.0;
            double hargaSettlement = rootNode.has("hargaSettlement") ? rootNode.get("hargaSettlement").asDouble() : 0.0;
            double totalBiaya = jumlahUnit * hargaSettlement;

            Client client = clientService.getClientById(primaryClientId);
            if (client == null) {
                throw new IllegalStateException("Data klien dengan ID " + primaryClientId + " tidak ditemukan.");
            }

            if (client.getSaldoRdn() < totalBiaya) {
                auditLogService.logAction(userId, "BANK_SYNC_INSUFFICIENT_FUNDS", ipAddress,
                        "Gagal sync SettlementEvent ID " + eventId + " (client: " + primaryClientId + "): Saldo RDN (Rp " +
                        client.getSaldoRdn() + ") tidak mencukupi untuk biaya settlement (Rp " + totalBiaya + ").");

                throw new InsufficientFundsException("Saldo RDN saat ini (Rp " +
                        String.format("%,.0f", client.getSaldoRdn()).replace(',', '.') +
                        ") tidak mencukupi untuk transaksi settlement (Dibutuhkan: Rp " +
                        String.format("%,.0f", totalBiaya).replace(',', '.') + ").");
            }

            // 1. Potong saldo RDN client
            String deductRdnSql = "UPDATE clients SET saldo_rdn = saldo_rdn - ? WHERE id = ?";
            jdbcTemplate.update(deductRdnSql, totalBiaya, primaryClientId);

            // 2. Insert/Update portfolio asset
            List<PortfolioAsset> assets = portfolioService.getAssetsByClientId(primaryClientId);
            PortfolioAsset existingAsset = assets.stream()
                    .filter(a -> kodeInstrumen.equalsIgnoreCase(a.getNamaInstrumen()))
                    .findFirst()
                    .orElse(null);

            if (existingAsset != null) {
                double newJumlah = existingAsset.getJumlah() + jumlahUnit;
                double newNilai = newJumlah * hargaSettlement;
                String updateAssetSql = "UPDATE portfolio_assets SET jumlah = ?, nilai = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                jdbcTemplate.update(updateAssetSql, newJumlah, newNilai, existingAsset.getId());
            } else {
                PortfolioAsset newAsset = new PortfolioAsset();
                newAsset.setClientId(primaryClientId);
                newAsset.setJenisInstrumen("Saham");
                newAsset.setNamaInstrumen(kodeInstrumen);
                newAsset.setJumlah(jumlahUnit);
                newAsset.setNilai(jumlahUnit * hargaSettlement);
                newAsset.setAllocationPercent(0.0);
                portfolioService.addAsset(newAsset);
            }

            recalculateAssetAllocations(primaryClientId);
            syncLogDetail = "Settlement processed: " + jumlahUnit + " unit " + kodeInstrumen +
                    " untuk client " + primaryClientId + " (saldo RDN dipotong Rp " + totalBiaya + ")";
        }

        String updateEventSql = "UPDATE bank_sync_events SET reconciliation_status = 'SYNCED', synced_at = CURRENT_TIMESTAMP, synced_by = ? WHERE id = ?";
        jdbcTemplate.update(updateEventSql, userId, eventId);

        auditLogService.logAction(userId, "BANK_SYNC_RECONCILED", ipAddress,
                "Bank sync event ID " + eventId + " reconciled: " + syncLogDetail);

        Map<String, Object> result = new HashMap<>();
        result.put("eventId", eventId);
        result.put("reconciliationStatus", "SYNCED");
        result.put("detail", syncLogDetail);
        return result;
    }

    private Map<String, Object> buildClientSummaryMap(Client client) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", client.getId());
        map.put("nama", client.getNama());
        map.put("nik", client.getNik());
        map.put("statusKyc", client.getStatusKyc());
        map.put("nomorRekening", client.getNomorRekening());
        map.put("saldoRdn", client.getSaldoRdn());
        return map;
    }

    private void recalculateAssetAllocations(String clientId) {
        List<PortfolioAsset> assets = portfolioService.getAssetsByClientId(clientId);
        double totalNilai = assets.stream().mapToDouble(PortfolioAsset::getNilai).sum();
        if (totalNilai > 0) {
            for (PortfolioAsset a : assets) {
                double percent = (a.getNilai() / totalNilai) * 100.0;
                String updatePercentSql = "UPDATE portfolio_assets SET allocation_percent = ? WHERE id = ?";
                jdbcTemplate.update(updatePercentSql, percent, a.getId());
            }
        }
    }
}
