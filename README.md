# SIMIPKIT

Integrated Client Investment & Portfolio Management System

> **Notice:** This application is designed specifically for controlled Vulnerability Assessment & Penetration Testing labs. It intentionally incorporates legacy dependency versions and security flaws for training purposes.

---

## Technical Stack

- **Java 11**, **Spring Framework 5.3.16** (MVC, JDBC)
- **Apache Tomcat 9**, **MariaDB / MySQL**
- **Maven**, **Docker / Docker Compose**

---

## Project Scope & Modules

SIMIPKIT provides a full-featured financial portfolio management platform comprising:

- **Client & KYC Management**: CRUD client identity data, single and bulk ZIP document uploads.
- **Portfolio Management**: Asset tracking, allocation calculations, and valuation monitoring.
- **Report Generator**: Immutable report summary snapshots, custom XML template parser, and PDF report export.
- **Bank Sync Engine**: Ingestion and processing of transactional data feeds from partner banks.
- **User & Access Control**: Role-based access control (Admin / Staff), PBKDF2 password hashing, IP rate limiting, and 7-minute inactivity auto-logout.
- **Audit & Security Logging**: Centralized activity log tracking across user actions and system events.

---

## Folder Structure

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
│       ├── CurrencyUtil.java
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
        ├── css/
        │   └── style.css
        └── js/
            ├── table-search.js
            ├── confirm-modal.js
            └── idle-timer.js
```

---

## Required Deployment Files

Before deployment, ensure the following configuration files are present in the project root:

1. `pom.xml` (Maven build configuration)
2. `Dockerfile` (Container image configuration for Tomcat 9 & JDK 11)
3. `docker-compose.yml` (Services definition for app and database)
4. `.env` (Copied from `.env.example` to define database credentials)
5. `src/main/resources/schema.sql` (Database initialization script)

---

## Step-by-Step Setup Guide

### Method 1: Docker Compose Deployment (Recommended)

1. **Clone & Prepare Environment File:**
   ```bash
   cp .env.example .env
   ```
2. **Build and Launch Containers:**
   ```bash
   docker compose up -d --build
   ```
3. **Access Application:**
   Open `http://localhost:8080` in your web browser.

---

### Method 2: Manual Setup on Ubuntu

1. **Install Prerequisites:**
   ```bash
   sudo apt update
   sudo apt install -y openjdk-11-jdk maven tomcat9 mariadb-server
   ```
2. **Configure Database:**
   ```sql
   CREATE DATABASE simipkit;
   CREATE USER 'simipkit_app'@'localhost' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON simipkit.* TO 'simipkit_app'@'localhost';
   FLUSH PRIVILEGES;
   ```
3. **Build Application WAR:**
   ```bash
   mvn clean package
   ```
4. **Deploy to Tomcat:**
   ```bash
   sudo cp target/simipkit.war /var/lib/tomcat9/webapps/ROOT.war
   sudo systemctl restart tomcat9
   ```
5. **Set Environment Variables (Optional):**
   Export `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` if using custom database settings.

---

## Default Credentials & Initial Access

- **Admin Account**: `admin` / `admin123`
- **Staff Account**: `staff` / `staff123`
- **Application URL**: `http://localhost:8080`

---

## License & Usage

Internal authorized laboratory and penetration testing use only.
