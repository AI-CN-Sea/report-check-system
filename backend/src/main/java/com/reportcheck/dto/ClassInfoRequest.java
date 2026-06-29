package com.reportcheck.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassInfoRequest {

    @NotBlank(message = "班级名称不能为空")
    private String className;

    private String grade;
    private String major;
}
