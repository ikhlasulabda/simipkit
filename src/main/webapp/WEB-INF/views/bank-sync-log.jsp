<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Bank Sync Log - SIMIPKIT</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:ital,wght@0,100..800;1,100..800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="<c:url value='/resources/css/style.css'/>">
</head>
<body>
    <header>
        <a href="<c:url value='/'/>" class="brand">SIMIPKIT</a>
        <ul class="nav-links">
            <li><a href="<c:url value='/'/>">Dashboard</a></li>
            <li><a href="<c:url value='/clients'/>">Klien</a></li>
            <li><a href="<c:url value='/documents/bulk-upload'/>">Bulk Upload KYC</a></li>
            <li><a href="<c:url value='/reports'/>">Laporan</a></li>
            <li><a href="<c:url value='/bank-sync-log'/>" class="active">Bank Sync Log</a></li>
            <c:if test="${sessionScope.role == 'admin'}">
                <li><a href="<c:url value='/user-management'/>">User Management</a></li>
                <li><a href="<c:url value='/report-template-upload'/>">Template Laporan</a></li>
            </c:if>
            <li><a href="<c:url value='/audit-log'/>">Audit Log</a></li>
        </ul>
        <div class="user-info">
            <span><c:out value="${sessionScope.user.username}"/></span>
            <span class="role-badge"><c:out value="${sessionScope.role}"/></span>
            <a href="<c:url value='/logout'/>" class="btn btn-sm btn-secondary">Logout</a>
        </div>
    </header>

    <div class="container">
        <div class="page-header">
            <div>
                <h1>Histori Integrasi Bank Feed</h1>
                <p>Log sinkronisasi transaksi otomatis dari sistem bank mitra</p>
            </div>
        </div>

        <div class="card">
            <div class="card-title">Event Transaksi Diterima</div>
            <div class="table-search-bar" style="display: flex; justify-content: space-between; align-items: center; gap: 12px;">
                <input type="text" id="search-bank-sync" class="table-search-input" style="max-width: 500px;"
                       placeholder="Cari di raw JSON... (nomor rekening, referenceNumber, kode instrumen, dsb)"
                       oninput="searchBankSyncPayload(this)">
                <c:if test="${sessionScope.role == 'admin'}">
                    <a href="<c:url value='/bank-sync-log/delete-all'/>"
                       class="btn btn-sm btn-danger btn-confirm-action"
                       data-title="Hapus Semua Log Bank Sync"
                       data-message="Hapus SELURUH log Bank Sync? Riwayat integrasi bank yang dihapus tidak bisa dipulihkan.">Hapus Semua Log</a>
                </c:if>
            </div>
            <div class="table-scroll-container table-scroll-bank-sync">
                <table id="tbl-bank-sync-log">
                    <colgroup>
                        <col style="width: 65px;">
                        <col style="width: 90px;">
                        <col style="width: 100px;">
                        <col style="width: 160px;">
                        <col style="width: 140px;">
                        <col>
                        <col style="width: 110px;">
                    </colgroup>
                    <thead>
                        <tr>
                            <th>ID Event</th>
                            <th>Tipe Event</th>
                            <th>Status</th>
                            <th>Rekonsiliasi</th>
                            <th>Waktu Diproses</th>
                            <th>Payload JSON Raw</th>
                            <th class="text-center">Aksi</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="e" items="${events}">
                            <tr class="log-row" data-event-id="<c:out value='${e.id}'/>">
                                <td class="mono"><c:out value="${e.id}"/></td>
                                <td>
                                    <span class="badge ${e.event_badge_class}" title="<c:out value='${e.event_full_title}'/>">
                                        <c:out value="${e.event_badge}"/>
                                    </span>
                                </td>
                                <td>
                                    <span class="badge ${e.status == 'PROCESSED' ? 'badge-status-processed' : 'badge-status-received'}">
                                        <c:out value="${e.status}"/>
                                    </span>
                                </td>
                                <td class="col-recon-cell">
                                    <c:choose>
                                        <c:when test="${e.reconciliation_status == 'SYNCED'}">
                                            <span class="badge badge-recon-synced" title="Tersinkronisasi pada <c:out value='${e.synced_at}'/>">Tersinkronisasi</span>
                                        </c:when>
                                        <c:when test="${e.reconciliation_status == 'MATCHED'}">
                                            <button type="button" class="btn badge badge-recon-matched" onclick="openMatchModal(${e.id})">Sync ke DB</button>
                                        </c:when>
                                        <c:when test="${e.reconciliation_status == 'FAILED'}">
                                            <span class="badge badge-recon-failed" onclick="openMatchModal(${e.id})" title="Klik untuk coba lagi">Gagal (Coba lagi)</span>
                                        </c:when>
                                        <c:otherwise>
                                            <button type="button" class="btn badge badge-recon-unmatched" onclick="openMatchModal(${e.id})">Cocokkan Rekening</button>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="mono" style="font-size: 11px;"><c:out value="${e.processed_at}"/></td>
                                <td class="col-payload-raw" title="<c:out value='${e.payload_raw}'/>">
                                    <div class="payload-preview mono"><c:out value="${e.payload_raw}"/></div>
                                    <code class="json-raw-source" style="display: none;"><c:out value="${e.payload_raw}"/></code>
                                </td>
                                <td class="col-actions text-center">
                                    <button type="button" class="btn-action-compact" onclick="openJsonModal(this, ${e.id})">
                                        <span>Lihat JSON</span>
                                    </button>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty events}">
                            <tr>
                                <td colspan="7" class="text-center">Belum ada log sinkronisasi bank terikat.</td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</body>
<script src="<c:url value='/resources/js/table-search.js'/>"></script>
<script src="<c:url value='/resources/js/confirm-modal.js'/>"></script>
<script src="<c:url value='/resources/js/json-expand.js'/>"></script>
<script src="<c:url value='/resources/js/bank-sync-reconciliation.js'/>"></script>
<script src="<c:url value='/resources/js/idle-timer.js'/>" data-logout-url="<c:url value='/logout?reason=timeout'/>"></script>
</html>
