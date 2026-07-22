# SIMIPKIT

Sistem Informasi Manajemen Investasi & Portofolio Klien Terintegrasi

Aplikasi web untuk mengelola data klien, dokumen KYC, portofolio investasi, laporan, dan sinkronisasi data dari sistem bank partner.

> **Catatan:** Project ini dibuat untuk keperluan lab Vulnerability Assessment & Penetration Testing yang terkontrol. Beberapa dependency sengaja menggunakan versi lama yang vulnerable untuk keperluan pembelajaran keamanan aplikasi. Lihat `important.md` untuk detail.

## Tech Stack

- Java 11
- Spring Framework 5.3.16 (MVC, JDBC)
- Apache Tomcat 9
- MariaDB / MySQL
- Maven

## Struktur Project

```
simipkit/
├── pom.xml
├── important.md
├── README.md
├── .env.example
├── .gitignore
├── Dockerfile
├── docker-compose.yml
│
├── src/main/java/com/happy/simipkit/
│   ├── config/
│   │   └── AppConfig.java
│   │
│   ├── controller/
│   │   ├── HomeController.java
│   │   ├── LoginController.java
│   │   ├── ClientController.java
│   │   ├── ClientDocumentController.java
│   │   ├── DocumentBulkController.java
│   │   ├── PortfolioController.java
│   │   ├── PortfolioReportController.java
│   │   ├── ReportController.java
│   │   ├── ReportTemplateController.java
│   │   ├── BankSyncController.java
│   │   ├── BankSyncLogController.java
│   │   ├── UserManagementController.java
│   │   └── AuditLogController.java
│   │
│   ├── model/
│   │   ├── Client.java
│   │   ├── ClientDocument.java
│   │   ├── PortfolioAsset.java
│   │   ├── PortfolioReportSummary.java
│   │   ├── ReportTemplate.java
│   │   ├── User.java
│   │   ├── AuditLogEntry.java
│   │   └── banksync/
│   │       ├── BankTransactionEvent.java
│   │       ├── SaldoUpdateEvent.java
│   │       ├── TransferConfirmationEvent.java
│   │       └── SettlementEvent.java
│   │
│   ├── security/
│   │   ├── PasswordHasher.java
│   │   ├── AuthenticationFilter.java
│   │   └── RoleAuthorizationFilter.java
│   │
│   ├── service/
│   │   ├── ClientService.java
│   │   ├── DocumentBulkService.java
│   │   ├── PortfolioService.java
│   │   ├── ReportService.java
│   │   ├── BankSyncService.java
│   │   ├── UserService.java
│   │   └── AuditLogService.java
│   │
│   └── util/
│       └── FileNamingUtil.java
│
├── src/main/resources/
│   ├── log4j2.xml
│   └── schema.sql
│
└── src/main/webapp/
    ├── WEB-INF/
    │   ├── web.xml
    │   └── views/
    │       ├── login.jsp
    │       ├── dashboard.jsp
    │       ├── client-list.jsp
    │       ├── client-form.jsp
    │       ├── client-detail.jsp
    │       ├── document-upload.jsp
    │       ├── document-bulk-upload.jsp
    │       ├── portfolio-list.jsp
    │       ├── portfolio-form.jsp
    │       ├── report-list.jsp
    │       ├── report-generate.jsp
    │       ├── report-template-upload.jsp
    │       ├── bank-sync-log.jsp
    │       ├── user-management.jsp
    │       ├── user-form.jsp
    │       └── audit-log.jsp
    │
    └── resources/
        └── css/
            └── style.css
```

## Modul

| Modul | Fungsi |
|---|---|
| Client & KYC Management | CRUD data klien, upload dokumen identitas (single & bulk ZIP) |
| Portfolio Management | Input dan tracking aset investasi klien |
| Report Generator | Export laporan portofolio, import custom template XML |
| Bank/Partner Data Sync | Terima feed data transaksi dari sistem bank partner |
| Admin & Access Control | Login, manajemen role (staff/admin) |
| Audit & Activity Log | Pencatatan aktivitas pengguna |

## Cara Menjalankan

### Mode 1 - Manual Deploy (Tomcat langsung)

Prasyarat: JDK 11, Maven, Tomcat 9, MariaDB/MySQL sudah terpasang.

```bash
mvn clean package
# copy target/simipkit.war ke folder webapps/ Tomcat
```

Environment variable (opsional, ada fallback default ke `localhost`):

```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=simipkit
DB_USER=simipkit_app
DB_PASSWORD=
```

### Mode 2 - Docker Compose

```bash
cp .env.example .env
# isi .env sesuai kebutuhan
docker compose up -d --build
```

Aplikasi dapat diakses di `http://localhost:8080`.

## Environment Variables

Lihat `.env.example` untuk daftar lengkap variabel yang dibutuhkan.

## Lisensi

Internal / lab use only.
