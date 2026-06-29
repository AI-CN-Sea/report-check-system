package com.reportcheck.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("check_result")
public class CheckResult {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long checkTaskId;
    private Long sourceReportId;
    private Long targetReportId;
    private BigDecimal cosineSimilarity;
    private BigDecimal simhashSimilarity;
    private BigDecimal finalSimilarity;
    private String riskLevel;
    private LocalDateTime createdTime;
}
