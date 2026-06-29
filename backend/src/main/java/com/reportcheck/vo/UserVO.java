package com.reportcheck.vo;

import lombok.Data;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String roleCode;
    private String roleName;
    private String studentNo;
    private String teacherNo;
    private String phone;
    private String email;
    private Integer status;
}
