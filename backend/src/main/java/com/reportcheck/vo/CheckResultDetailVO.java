package com.reportcheck.vo;

import com.reportcheck.entity.SimilarSentence;
import lombok.Data;

import java.util.List;

@Data
public class CheckResultDetailVO {

    private CheckResultVO result;
    private List<SimilarSentence> similarSentences;
    private Integer similarSentenceCount;
    private String detailMessage;
}
