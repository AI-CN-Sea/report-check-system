package com.reportcheck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseRequest {

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    @NotBlank(message = "课程编码不能为空")
    private String courseCode;

    @NotNull(message = "任课教师不能为空")
    private Long teacherId;

    private String description;
}
