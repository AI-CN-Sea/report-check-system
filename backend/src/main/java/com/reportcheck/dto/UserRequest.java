package com.reportcheck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @NotNull(message = "角色不能为空")
    private Long roleId;

    private String studentNo;
    private String teacherNo;
    private String phone;
    private String email;
    private Integer status = 1;
}
