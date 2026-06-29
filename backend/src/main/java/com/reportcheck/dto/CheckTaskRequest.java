package com.reportcheck.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckTaskRequest {

    @NotNull(message = "实验任务不能为空")
    private Long experimentTaskId;

    private Long createdBy;
}
