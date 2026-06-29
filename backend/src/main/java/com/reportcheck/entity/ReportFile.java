package com.reportcheck.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report_file")
public class ReportFile {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long studentId;
    private String originalName;
    private String storedName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String parsedText;
    private Integer wordCount;
    private Integer parseStatus;
    private String parseMessage;
    private LocalDateTime uploadTime;
}
