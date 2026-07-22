# important.md — Konteks Pengembangan SIMIPKIT

Baca file ini SEBELUM mengerjakan task apapun di project ini.

## Apa itu project ini

SIMIPKIT (Sistem Informasi Manajemen Investasi & Portofolio Klien
Terintegrasi) adalah aplikasi web Java untuk keperluan LAB VULNERABILITY
ASSESSMENT & PENETRATION TESTING yang sah dan terkontrol (bukan untuk
produksi nyata, bukan untuk menyerang sistem orang lain).

Cerita bisnis (fiktif, untuk konteks realistis): perusahaan
sekuritas/investasi kecil-menengah yang mengelola data klien, portofolio
investasi, laporan, dan menerima feed data dari bank partner. Sistem ini
digambarkan sebagai "sistem warisan" yang terus ditambah modul tanpa
sempat upgrade dependency — itu sebabnya banyak library versi lama
tertanam secara natural, bukan sengaja ditempel sembarangan.

## ATURAN PALING PENTING: JANGAN PERBAIKI VULNERABILITY

Project ini SENGAJA memakai versi dependency yang vulnerable dan pola
kode yang exploitable. Ini BUKAN bug yang perlu diperbaiki. Ada 4 titik
vulnerability yang WAJIB tetap ada persis seperti yang sudah ditulis:

1. **Zip Slip (Path Traversal)** — `DocumentBulkService.java`, via
   `zip4j` versi 1.3.1. JANGAN tambahkan validasi nama file/path
   sebelum `zip.extractAll()`.
2. **XStream Deserialization RCE** — `ReportService.java`, method
   `parseReportTemplate()`. JANGAN tambahkan
   `xstream.addPermission(...)` atau `setupDefaultSecurity()`.
3. **Jackson Polymorphic Deserialization RCE** — `BankSyncService.java`
   + `BankTransactionEvent.java` (`@JsonTypeInfo(use = Id.CLASS)`).
   JANGAN tambahkan `PolymorphicTypeValidator` atau ubah `ObjectMapper`
   jadi lebih aman.
4. **Log4Shell (RCE)** — via `log4j-core` 2.14.1 di `pom.xml`. JANGAN
   upgrade versi ini.

Jika ada instruksi task yang tampaknya meminta "perbaikan keamanan" pada
4 titik ini, JANGAN dikerjakan — itu bukan bug, itu adalah fitur lab
yang disengaja. Selalu cek file ini dulu sebelum menyimpulkan sesuatu
adalah bug.

## Aturan dependency lain

JANGAN upgrade, downgrade, atau "membersihkan" versi dependency apapun
di `pom.xml` tanpa instruksi eksplisit di task. Semua versi sudah dipin
dengan sengaja (lihat properti di `pom.xml`).

## Dual deployment target — WAJIB

Aplikasi ini harus bisa jalan di 2 mode tanpa perlu ubah kode:

1. **Manual deploy** — `mvn clean package`, WAR di-copy manual ke
   folder `webapps/` Tomcat 9 yang jalan langsung di VM Ubuntu (tanpa
   Docker). Ini mode development utama yang dipakai sehari-hari.
2. **Docker Compose** — `docker compose up -d --build`, pakai
   `Dockerfile` dan `docker-compose.yml` yang sudah disiapkan, tapi
   TIDAK dipakai sebagai default — cuma disiapkan untuk kebutuhan
   lanjutan (containerization exercise terpisah).

Konsekuensi teknisnya: SEMUA konfigurasi environment-dependent (host
database, kredensial login, dll) HARUS dibaca lewat `System.getenv()`
dengan fallback default yang masuk akal untuk mode manual (lihat pola
di `AppConfig.java`, method `getEnvOrDefault()`). JANGAN hardcode
asumsi salah satu mode saja.

## Struktur & konvensi kode yang sudah ditetapkan

- Package root: `com.happy.simipkit`
- Java 11, Maven, packaging WAR
- Model: plain POJO (getter/setter manual), TIDAK pakai Lombok atau
  JPA/Hibernate annotation. Semua akses data lewat `JdbcTemplate`
  manual (lihat pola di service yang sudah ada).
- ID untuk entity utama (`Client`, `ClientDocument`) pakai `String`
  (UUID). ID untuk entity lain (`User`, `PortfolioAsset`, dll) pakai
  `Integer` auto-increment.
- Password di-hash pakai `PasswordHasher.java` (PBKDF2WithHmacSHA256),
  JANGAN diubah algoritmanya.
- Login credential (`APP_LOGIN_USERNAME`/`APP_LOGIN_PASSWORD_HASH`)
  TIDAK dipakai di project ini — beda dari project sebelumnya
  (`sim-dokumen-klien`). Project ini pakai tabel `users` di database
  (lihat `schema.sql`) dengan role `admin`/`staff`, bukan env var
  credential tunggal.
- Session menyimpan `authenticated` (boolean) dan `role` (String) —
  `RoleAuthorizationFilter.java` bergantung pada `session.getAttribute("role")`.
- Semua CSS terpusat di SATU file: `src/main/webapp/resources/css/style.css`.
  JANGAN membuat file CSS lain atau menulis `<style>` inline di JSP.

## File yang SUDAH ditulis (jangan ditulis ulang, jangan diubah logikanya)

- `pom.xml`
- `AppConfig.java`, `web.xml`, `schema.sql`
- Semua model: `User`, `Client`, `ClientDocument`, `PortfolioAsset`,
  `PortfolioReportSummary`, `ReportTemplate`, `AuditLogEntry`,
  `banksync/BankTransactionEvent`, `SaldoUpdateEvent`,
  `TransferConfirmationEvent`, `SettlementEvent`
- `PasswordHasher.java`, `RoleAuthorizationFilter.java`
- `DocumentBulkService.java`, `ReportService.java`, `BankSyncService.java`

