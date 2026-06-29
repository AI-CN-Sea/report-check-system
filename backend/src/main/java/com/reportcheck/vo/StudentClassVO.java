package com.reportcheck.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentClassVO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private Long classId;
    private String className;
    private LocalDateTime createdTime;
}
