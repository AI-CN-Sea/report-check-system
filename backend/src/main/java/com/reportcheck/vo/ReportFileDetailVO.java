package com.reportcheck.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReportFileDetailVO extends ReportFileVO {

    private String parsedText;
}
