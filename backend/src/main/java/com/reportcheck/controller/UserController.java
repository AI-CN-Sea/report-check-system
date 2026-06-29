package com.reportcheck.controller;

import com.reportcheck.common.Result;
import com.reportcheck.dto.PasswordResetRequest;
import com.reportcheck.dto.UserRequest;
import com.reportcheck.entity.SysRole;
import com.reportcheck.service.UserService;
import com.reportcheck.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<List<UserVO>> listUsers() {
        return Result.ok(userService.listUsers());
    }

    @GetMapping("/roles")
    public Result<List<SysRole>> listRoles() {
        return Result.ok(userService.listRoles());
    }

    @PostMapping
    public Result<UserVO> createUser(@Valid @RequestBody UserRequest request) {
        return Result.ok(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public Result<UserVO> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return Result.ok(userService.updateUser(id, request));
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }
}
