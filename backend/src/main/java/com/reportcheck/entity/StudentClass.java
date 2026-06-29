package com.reportcheck.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_class")
public class StudentClass {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long classId;
    private LocalDateTime createdTime;
}
