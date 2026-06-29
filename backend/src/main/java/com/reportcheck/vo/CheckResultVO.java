package com.reportcheck.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CheckResultVO {

    private Long id;
    private Long checkTaskId;
    private String sourceStudentName;
    private String sourceReportName;
    private String targetStudentName;
    private String targetReportName;
    private BigDecimal cosineSimilarity;
    private BigDecimal simhashSimilarity;
    private BigDecimal finalSimilarity;
    private String riskLevel;
    private LocalDateTime createdTime;
}
