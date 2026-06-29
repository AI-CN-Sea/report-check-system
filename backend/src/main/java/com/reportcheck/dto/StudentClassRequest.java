package com.reportcheck.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentClassRequest {

    @NotNull(message = "学生不能为空")
    private Long studentId;

    @NotNull(message = "班级不能为空")
    private Long classId;
}
