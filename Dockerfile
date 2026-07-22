# Multi-stage build for SIMIPKIT
FROM maven:3.8.6-openjdk-11 AS build
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build WAR package
RUN mvn clean package -DskipTests

# Runtime stage using Tomcat 9 JDK 11
FROM tomcat:9.0-jdk11-openjdk-slim
WORKDIR /usr/local/tomcat

# Remove default ROOT webapp and copy built WAR as ROOT.war
RUN rm -rf webapps/ROOT webapps/ROOT.war
COPY --from=build /app/target/simipkit.war webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
