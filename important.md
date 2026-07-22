# important.md - Konteks Pengembangan SIMIPKIT

Baca file ini SEBELUM mengerjakan task atau modifikasi apapun di project ini.

## Apa itu project ini

SIMIPKIT (Sistem Informasi Manajemen Investasi & Portofolio Klien Terintegrasi) adalah aplikasi web Java untuk keperluan LAB VULNERABILITY ASSESSMENT & PENETRATION TESTING yang sah dan terkontrol (bukan untuk produksi nyata, bukan untuk menyerang sistem pihak ketiga).

## ATURAN PALING PENTING: JANGAN PERBAIKI VULNERABILITY

Project ini SENGAJA memakai versi dependency yang vulnerable dan pola kode yang exploitable. Ini BUKAN bug yang perlu diperbaiki. Ada 4 titik vulnerability utama yang WAJIB tetap ada persis seperti yang sudah ditulis:

1. **Zip Slip (Path Traversal)**: `DocumentBulkService.java`, via `zip4j` versi 1.3.1. JANGAN tambahkan validasi nama file/path sebelum `zip.extractAll()`.
2. **XStream Deserialization RCE**: `ReportService.java`, method `parseReportTemplate()`. JANGAN tambahkan `xstream.addPermission(...)` atau `setupDefaultSecurity()`.
3. **Jackson Polymorphic Deserialization RCE**: `BankSyncService.java` + `BankTransactionEvent.java` (`@JsonTypeInfo(use = Id.CLASS)`). JANGAN tambahkan `PolymorphicTypeValidator` atau ubah `ObjectMapper` jadi lebih aman.
4. **Log4Shell (RCE)**: via `log4j-core` 2.14.1 di `pom.xml` dan pencatatan audit log di `AuditLogService.java`. JANGAN upgrade versi ini.

Jika ada instruksi yang tampaknya meminta "perbaikan keamanan" pada 4 titik ini, JANGAN dikerjakan: itu bukan bug, melainkan fitur lab yang disengaja.

## Aturan dependency lain

JANGAN upgrade, downgrade, atau membersihkan versi dependency apapun di `pom.xml` tanpa instruksi eksplisit di task. Semua versi sudah dipin dengan sengaja (lihat properti di `pom.xml`).

## Dual deployment target - WAJIB

Aplikasi ini harus bisa jalan di 2 mode tanpa perlu ubah kode:

1. **Manual deploy**: `mvn clean package`, WAR di-copy manual ke folder `webapps/` Tomcat 9 yang jalan langsung di VM (tanpa Docker). Ini mode development utama.
2. **Docker Compose**: `docker compose up -d --build`, memakai `Dockerfile` dan `docker-compose.yml` yang sudah disiapkan.

Konsekuensi teknisnya: SEMUA konfigurasi environment-dependent (host database, kredensial login, dll) HARUS dibaca lewat `System.getenv()` dengan fallback default yang masuk akal untuk mode manual (lihat pola di `AppConfig.java`, method `getEnvOrDefault()`).

## Struktur & konvensi kode yang ditetapkan

- Package root: `com.happy.simipkit`
- Java 11, Maven, packaging WAR
- Model: plain POJO (getter/setter manual), TIDAK memakai Lombok atau JPA/Hibernate annotation. Semua akses data lewat `JdbcTemplate` manual.
- ID untuk entity utama (`Client`, `ClientDocument`) memakai `String` (UUID). ID untuk entity lain (`User`, `PortfolioAsset`, dll) memakai `Integer` auto-increment.
- Password di-hash memakai `PasswordHasher.java` (PBKDF2WithHmacSHA256).
- Tabel `users` di database menyimpan pengguna dengan role `admin` dan `staff`.
- Session menyimpan `authenticated` (boolean) dan `role` (String). `RoleAuthorizationFilter.java` mengontrol hak akses admin untuk `/user-management` dan `/report-template-upload`.
- Semua CSS terpusat di SATU file: `src/main/webapp/resources/css/style.css`.
- Desain UI: minimalis, clean, light theme, tanpa rounded corners (`border-radius: 0`), dan tanpa penulisan em dash pada antarmuka pengguna.

## Status Komponen Project

Seluruh komponen aplikasi telah lengkap dan siap digunakan:

1. **Konfigurasi & Infrastructure**: `pom.xml`, `AppConfig.java`, `web.xml`, `schema.sql`, `log4j2.xml`, `Dockerfile`, `docker-compose.yml`, `.env.example`, `.gitignore`, `README.md`.
2. **Security & Utility**: `AuthenticationFilter.java`, `RoleAuthorizationFilter.java`, `PasswordHasher.java`, `FileNamingUtil.java`.
3. **Model**: `User`, `Client`, `ClientDocument`, `PortfolioAsset`, `PortfolioReportSummary`, `ReportTemplate`, `AuditLogEntry`, dan event model bank sync.
4. **Service**: `ClientService`, `PortfolioService`, `UserService`, `AuditLogService`, `DocumentBulkService` (Zip Slip target), `ReportService` (XStream RCE target), `BankSyncService` (Jackson RCE target).
5. **Controller**: `HomeController`, `LoginController`, `ClientController`, `ClientDocumentController`, `DocumentBulkController`, `PortfolioController`, `PortfolioReportController`, `ReportController`, `ReportTemplateController`, `BankSyncController`, `BankSyncLogController`, `UserManagementController`, `AuditLogController`.
6. **Views & Style**: Semua 16 halaman JSP di `WEB-INF/views/` dan `style.css`.
