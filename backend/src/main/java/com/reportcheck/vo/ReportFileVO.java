package com.reportcheck.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportFileVO {

    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private String originalName;
    private String fileType;
    private Long fileSize;
    private Integer wordCount;
    private Integer parseStatus;
    private String parseMessage;
    private LocalDateTime uploadTime;
}
