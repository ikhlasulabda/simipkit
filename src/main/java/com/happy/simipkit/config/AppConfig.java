package com.happy.simipkit.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import javax.sql.DataSource;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.happy.simipkit")
public class AppConfig implements WebMvcConfigurer {

    private static final Logger logger = LogManager.getLogger(AppConfig.class);

    /**
     * DataSource bean. Mendukung 2 mode deployment tanpa perlu ubah kode:
     * 1. Manual deploy (Tomcat langsung di VM, tanpa Docker) -> env var tidak
     * di-set,
     * fallback ke "localhost" (asumsi MariaDB/MySQL jalan di VM yang sama).
     * 2. Docker Compose -> env var di-inject dari .env, host diarahkan ke nama
     * service database ("db").
     */
    @Bean
    public DataSource dataSource() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        String dbHost = getEnvOrDefault("DB_HOST", "localhost");
        String dbPort = getEnvOrDefault("DB_PORT", "3306");
        String dbName = getEnvOrDefault("DB_NAME", "simipkit");
        String dbUser = getEnvOrDefault("DB_USER", "simipkit_app");
        String dbPassword = getEnvOrDefault("DB_PASSWORD", "");

        String jdbcUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(dbUser);
        dataSource.setPassword(dbPassword);

        logger.info("DataSource configured -> host: {}, port: {}, database: {}", dbHost, dbPort, dbName);
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            logger.info("Initializing database schema from schema.sql...");
            try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
                if (is != null) {
                    byte[] bytes = is.readAllBytes();
                    String sql = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                    for (String statement : sql.split(";")) {
                        if (!statement.trim().isEmpty()) {
                            jdbcTemplate.execute(statement.trim());
                        }
                    }
                    logger.info("Database schema initialized successfully.");
                } else {
                    logger.warn("schema.sql not found in classpath. Skipping schema initialization.");
                }
            }
        } catch (Exception e) {
            logger.error("Failed to initialize database schema: {}", e.getMessage(), e);
        }
        return jdbcTemplate;
    }

    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setViewClass(JstlView.class);
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    @Bean(name = "multipartResolver")
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        // 50MB - lebih besar dari project sebelumnya karena ada fitur bulk upload ZIP
        resolver.setMaxUploadSize(52428800);
        resolver.setDefaultEncoding("UTF-8");
        return resolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
    }

    /**
     * Helper: ambil environment variable, fallback ke default value jika
     * tidak di-set atau kosong. Dipakai konsisten untuk semua config yang
     * perlu berbeda antara mode manual deploy vs Docker.
     */
    private String getEnvOrDefault(String envKey, String defaultValue) {
        String value = System.getenv(envKey);
        if (value == null || value.trim().isEmpty()) {
            logger.warn("Environment variable {} is not set. Falling back to default: '{}'", envKey, defaultValue);
            return defaultValue;
        }
        return value;
    }
}