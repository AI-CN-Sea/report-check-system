package com.reportcheck.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("similar_sentence")
public class SimilarSentence {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long checkResultId;
    private String sourceSentence;
    private String targetSentence;
    private BigDecimal sentenceSimilarity;
    private LocalDateTime createdTime;
}
