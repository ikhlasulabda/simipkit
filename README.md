# SIMIPKIT

**Integrated Client Investment & Portfolio Management System**

A Java web application for client onboarding, KYC management, portfolio tracking, PDF reporting, and partner bank data sync, built on classic Spring MVC + JSP + JdbcTemplate (no Spring Boot, no ORM).

> **Lab Context:** Built for a controlled Vulnerability Assessment & Penetration Testing (VAPT) environment. It intentionally retains vulnerable dependency versions and exploitable code patterns for security training. See [`detailvuln.md`](./detailvuln.md) for the full list of intentional vulnerabilities. **Do not deploy on a public-facing network.**

---

## Features

- **Client & KYC Management** — CRUD, single & bulk ZIP document upload with automatic DB cataloging, searchable listings
- **Portfolio Management** — multi-instrument tracking (Saham, Reksadana, Obligasi, Deposito, Pasar Uang), live allocation %
- **Reporting** — immutable report snapshots, PDF export (iText), admin-managed dynamic XML templates with secure parsing and automatic fallback
- **Bank Sync Log** — partner transaction feed ingestion, expandable raw JSON payload viewer
- **Analytics Dashboard** — live Chart.js visualizations (AUM trend, allocation, top clients, KYC status, doc types)
- **Access Control** — role-based (Admin/Staff), PBKDF2 password hashing, session timeout, POST-only state-changing actions
- **Audit Logging** — centralized, searchable activity log

## Tech Stack

Java 11 · Spring Framework 5.3.16 (MVC, JDBC) · JSP/JSTL · Apache Tomcat 9 · MariaDB · Maven · iText 5.5.13.3 · Chart.js · Docker

---

## Getting Started

### Prerequisites
- Ubuntu 24.04 LTS (manual deploy) or Docker + Docker Compose
- JDK 11, Maven 3.8+, MariaDB 10.x

### Method 1 — Manual Deployment on Ubuntu

**1. Install Maven**
```bash
sudo apt update
sudo apt install -y maven
mvn -version
```

**2. Install MariaDB**
```bash
sudo apt install -y mariadb-server
sudo systemctl enable --now mariadb
sudo mysql_secure_installation
```

**3. Create database and user**
```bash
sudo mysql
```
```sql
CREATE DATABASE simipkit CHARACTER SET utf8mb4;
CREATE USER 'simipkit_app'@'localhost' IDENTIFIED BY '<your_password>';
GRANT ALL PRIVILEGES ON simipkit.* TO 'simipkit_app'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```
Schema is applied automatically on first app startup (no manual import needed).

**4. Clone and build**
```bash
git clone https://github.com/ikhlasulabda/simipkit.git
cd simipkit
mvn clean package
```

**5. Configure environment variables**

Create `/etc/simipkit.env`:
```dotenv
DB_HOST=localhost
DB_PORT=3306
DB_NAME=simipkit
DB_USER=simipkit_app
DB_PASSWORD=<your_password>
```
```bash
sudo chown root:tomcat /etc/simipkit.env
sudo chmod 640 /etc/simipkit.env
sudo systemctl edit tomcat.service
```
```ini
[Service]
EnvironmentFile=/etc/simipkit.env
```
```bash
sudo systemctl daemon-reload
sudo systemctl restart tomcat
```

**6. Deploy the WAR**
```bash
sudo cp target/simipkit.war /opt/tomcat/webapps/
sudo chown tomcat:tomcat /opt/tomcat/webapps/simipkit.war
tail -n 50 /opt/tomcat/logs/catalina.out
```
Confirm `Database schema initialized successfully` and `Deployment ... has finished` in the log.

**7. Create the first admin account**

No default credentials are seeded.
```bash
java -cp target/classes com.happy.simipkit.security.PasswordHasher <your_password>
mysql -u simipkit_app -p simipkit
```
```sql
INSERT INTO users (username, password_hash, role, is_active)
VALUES ('<your_username>', '<hash_from_previous_step>', 'admin', 1);
```

**8. Access**
```
http://<your-server-ip>:8080/simipkit/login
```

### Method 2 — Docker Compose

```bash
cp .env.example .env
docker compose up -d --build
```
Available at `http://localhost:8080`. No default user is seeded, create the first admin the same way as Step 7 above, run inside the app container.

### Environment Variables

| Variable | Description | Default (manual mode) |
|---|---|---|
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `3306` |
| `DB_NAME` | Database name | `simipkit` |
| `DB_USER` | Database application user | `simipkit_app` |
| `DB_PASSWORD` | Database application password | *(none — must be set)* |

---

## Project Structure

```
simipkit/
├── pom.xml
├── important.md
├── detailvuln.md
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
│   │   ├── AuditLogController.java
│   │   ├── BankSyncController.java
│   │   ├── BankSyncLogController.java
│   │   ├── ClientController.java
│   │   ├── ClientDocumentController.java
│   │   ├── DocumentBulkController.java
│   │   ├── HomeController.java
│   │   ├── LoginController.java
│   │   ├── PortfolioController.java
│   │   ├── PortfolioReportController.java
│   │   ├── ReportController.java
│   │   ├── ReportTemplateController.java
│   │   └── UserManagementController.java
│   │
│   ├── model/
│   │   ├── AuditLogEntry.java
│   │   ├── Client.java
│   │   ├── ClientDocument.java
│   │   ├── PortfolioAsset.java
│   │   ├── PortfolioReportSummary.java
│   │   ├── ReportLayoutConfig.java
│   │   ├── ReportTemplate.java
│   │   ├── User.java
│   │   └── banksync/
│   │       ├── BankTransactionEvent.java
│   │       ├── SaldoUpdateEvent.java
│   │       ├── SettlementEvent.java
│   │       └── TransferConfirmationEvent.java
│   │
│   ├── security/
│   │   ├── AuthenticationFilter.java
│   │   ├── PasswordHasher.java
│   │   └── RoleAuthorizationFilter.java
│   │
│   ├── service/
│   │   ├── AuditLogService.java
│   │   ├── BankSyncService.java
│   │   ├── ClientService.java
│   │   ├── DocumentBulkService.java
│   │   ├── PortfolioService.java
│   │   ├── ReportService.java
│   │   └── UserService.java
│   │
│   └── util/
│       ├── CurrencyUtil.java
│       ├── FileNamingUtil.java
│       └── JsStringUtil.java
│
├── src/main/resources/
│   ├── log4j2.xml
│   └── schema.sql
│
└── src/main/webapp/
    ├── WEB-INF/
    │   ├── web.xml
    │   └── views/
    │       ├── audit-log.jsp
    │       ├── bank-sync-log.jsp
    │       ├── client-detail.jsp
    │       ├── client-form.jsp
    │       ├── client-list.jsp
    │       ├── dashboard.jsp
    │       ├── document-bulk-upload.jsp
    │       ├── document-upload.jsp
    │       ├── login.jsp
    │       ├── portfolio-form.jsp
    │       ├── portfolio-list.jsp
    │       ├── report-generate.jsp
    │       ├── report-list.jsp
    │       ├── report-template-upload.jsp
    │       ├── user-form.jsp
    │       └── user-management.jsp
    │
    └── resources/
        ├── css/
        │   └── style.css
        └── js/
            ├── confirm-modal.js
            ├── dashboard-charts.js
            ├── idle-timer.js
            ├── json-expand.js
            ├── table-search.js
            └── template-modal.js
```

---

## License

Internal / authorized laboratory use only.