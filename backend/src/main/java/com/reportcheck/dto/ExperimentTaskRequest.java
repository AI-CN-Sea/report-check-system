package com.reportcheck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExperimentTaskRequest {

    @NotNull(message = "课程不能为空")
    private Long courseId;

    @NotNull(message = "班级不能为空")
    private Long classId;

    @NotBlank(message = "任务标题不能为空")
    private String title;

    private String description;
    private LocalDateTime deadline;
    private Integer status = 1;
    private Long createdBy;
}
