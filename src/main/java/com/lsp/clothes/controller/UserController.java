package com.lsp.clothes.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsp.clothes.common.BaseResponse;
import com.lsp.clothes.common.DeleteRequest;
import com.lsp.clothes.common.ResultUtils;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.exception.ThrowUtils;
import com.lsp.clothes.model.dto.user.*;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.LoginUserVO;
import com.lsp.clothes.model.vo.UserVO;
import com.lsp.clothes.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    /**用户注册
     * @param userRegisterRequest 用户注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest==null, ErrorCode.PARAMS_ERROR);
        String userAccount=userRegisterRequest.getUserAccount();
        String userPassword=userRegisterRequest.getUserPassword();
        String checkPassword=userRegisterRequest.getCheckPassword();
        System.out.println(userAccount);
        System.out.println(userPassword);
        long result=userService.userRegister(userAccount,userPassword,checkPassword);
        return ResultUtils.success(result);
    }
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }
    /**
     * 获取当前登录用户
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUserVO(HttpServletRequest request) {
        User loginUser=userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }
    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        ThrowUtils.throwIf(request==null,ErrorCode.PARAMS_ERROR);
        boolean result=userService.userLogout(request);
        return ResultUtils.success(result);
    }
    /**增加用户
     * @param userAddRequest 用户添加请求
     * @return 添加结果
     */
    @SaCheckRole("admin")
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user=new User();
        BeanUtils.copyProperties(userAddRequest,user);
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        List<String> roleCodes = resolveRoleCodes(userAddRequest.getRoleCodes(),
                userAddRequest.getUserRole(), "user");
        return ResultUtils.success(userService.createUser(user, roleCodes));
    }
    /*
    获取用户列表
     */
    @SaCheckRole("admin")
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO> > listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current=userQueryRequest.getCurrent();
        long pageSize=userQueryRequest.getPageSize();
        Page<User> userPage=userService.page(new Page<>(current,pageSize),userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage=new Page<>(current,pageSize);
        List<UserVO> userVOList=userService.getUserVOList(userPage.getRecords());
        userVOPage.setTotal(userPage.getTotal());

        userVOPage.setCurrent(userPage.getCurrent());
        userVOPage.setSize(userPage.getSize());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
    /*更新用户
     */
    @PostMapping("/update")
    @SaCheckRole("admin")
    public BaseResponse<Long> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if(userUpdateRequest==null){
            ThrowUtils.throwIf(userUpdateRequest==null, ErrorCode.PARAMS_ERROR);
        }
        User user=new User();
        BeanUtils.copyProperties(userUpdateRequest,user);
        List<String> roleCodes = resolveRoleCodes(userUpdateRequest.getRoleCodes(),
                userUpdateRequest.getUserRole(), null);
        userService.updateUser(user, roleCodes);
        return ResultUtils.success(user.getId());
    }
    /*删除用户
     */
    @PostMapping("/delete")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(
                deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR
        );
        Long userId = deleteRequest.getId();
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在或已删除");
        boolean result = userService.removeById(userId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除用户失败");
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @SaCheckRole("admin")
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    private List<String> resolveRoleCodes(List<String> roleCodes, String userRole,
                                          String defaultRole) {
        if (roleCodes != null) return roleCodes;
        if (userRole != null && !userRole.isBlank()) return List.of(userRole);
        return defaultRole == null ? null : List.of(defaultRole);
    }

}
