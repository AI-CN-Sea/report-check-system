package com.reportcheck.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckTaskVO {

    private Long id;
    private Long experimentTaskId;
    private String experimentTaskTitle;
    private String algorithm;
    private Integer reportCount;
    private Integer parsedReportCount;
    private Integer resultCount;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdTime;
}
