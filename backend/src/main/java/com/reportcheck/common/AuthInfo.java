package com.reportcheck.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthInfo {

    private Long userId;
    private String username;
    private String roleCode;
}
