package com.lsp.clothes.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.lsp.clothes.model.dto.user.UserQueryRequest;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.model.vo.LoginUserVO;
import com.lsp.clothes.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * @author 琳琅
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2026-07-28 13:13:25
 */
public interface UserService extends IRepository<User> {
    long createUser(User user, List<String> roleCodes);
    void updateUser(User user, List<String> roleCodes);
    List<String> getRoleCodes(Long userId);
    boolean hasRole(Long userId, String roleCode);
    /**用户注册
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @return 新用户id
     */
    long userRegister(String userAccount, String userPassword,String checkPassword);
    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获得脱敏后的登录用户信息
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获得脱敏后的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获得脱敏后的用户信息列表
     *
     * @param userList
     * @return 脱敏后的用户列表
     */
    List<UserVO> getUserVOList(List<User> userList);
    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);
    /**
     * 获取用户查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);




    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);



    /**
     * 获取加密后的密码
     *
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);
}
