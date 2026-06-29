package com.reportcheck.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("check_task")
public class CheckTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long experimentTaskId;
    private String algorithm;
    private Integer reportCount;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long createdBy;
    private LocalDateTime createdTime;
}
