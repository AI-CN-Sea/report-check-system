package com.reportcheck.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "report-check")
public class ReportCheckProperties {

    private String uploadDir;
    private String jwtSecret;
    private Long jwtExpireMinutes;
}
