package com.lsp.clothes.service.Impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.lsp.clothes.exception.BusinessException;
import com.lsp.clothes.exception.ErrorCode;
import com.lsp.clothes.model.dto.user.UserQueryRequest;
import com.lsp.clothes.model.enums.UserRoleEnum;
import com.lsp.clothes.model.entity.SysRole;
import com.lsp.clothes.model.entity.UserRole;
import com.lsp.clothes.mapper.SysRoleMapper;
import com.lsp.clothes.mapper.UserRoleMapper;
import com.lsp.clothes.model.vo.LoginUserVO;
import com.lsp.clothes.model.vo.UserVO;
import com.lsp.clothes.service.UserService;
import com.lsp.clothes.model.entity.User;
import com.lsp.clothes.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

/**
* @author 琳琅
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2026-07-28 13:13:25
*/
@Service
@Slf4j
public class UserServiceImpl extends CrudRepository<UserMapper, User>
    implements UserService {

    @jakarta.annotation.Resource
    private UserRoleMapper userRoleMapper;

    @jakarta.annotation.Resource
    private SysRoleMapper sysRoleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createUser(User user, List<String> roleCodes) {
        if (!this.save(user)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加用户失败");
        }
        assignRoles(user.getId(), roleCodes);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(User user, List<String> roleCodes) {
        if (!this.updateById(user)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新用户失败");
        }
        if (roleCodes != null) assignRoles(user.getId(), roleCodes);
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        if (userId == null) return List.of();
        return userRoleMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public boolean hasRole(Long userId, String roleCode) {
        return getRoleCodes(userId).contains(roleCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long userRegister(String userAccount, String userPassword, String checkPassword) {

        if(StrUtil.hasBlank(userAccount,userPassword,checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        if(userAccount.length()<6){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号过短");
        }
        if(userPassword.length()<8 || checkPassword.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }
        if(!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入密码不一致");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account",userAccount);
        long count=this.baseMapper.selectCount(queryWrapper);
        if(count>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号已存在");
        }
        String encryptPassword =getEncryptPassword(userPassword);
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(userAccount);
        createUser(user, List.of(UserRoleEnum.USER.getValue()));
        return user.getId();
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 从 Sa-Token 获取当前登录用户 ID
        try {
            long userId = StpUtil.getLoginIdAsLong();
            // 从数据库查询用户（建议加缓存）
            User user = this.getById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户不存在");
            }
            return user;
        } catch (NotLoginException e) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
    }

    /**
     * 获取脱敏类的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        loginUserVO.setId(String.valueOf(user.getId()));
        List<String> roles = getRoleCodes(user.getId());
        loginUserVO.setUserRoles(roles);
        loginUserVO.setUserRole(roles.isEmpty() ? null : roles.get(0));
        return loginUserVO;
    }

    /**
     * 获得脱敏后的用户信息
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        userVO.setId(String.valueOf(user.getId()));
        List<String> roles = getRoleCodes(user.getId());
        userVO.setUserRoles(roles);
        userVO.setUserRole(roles.isEmpty() ? null : roles.get(0));
        return userVO;
    }

    /**
     * 获取脱敏后的用户列表
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        try {
            StpUtil.logout();   // 注销当前会话
            return true;
        } catch (NotLoginException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        if(StrUtil.hasBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        if(userAccount.length()<6){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号过短");
        }
        if(userPassword.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }

        String encryptPassword =getEncryptPassword(userPassword);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account",userAccount);
        queryWrapper.eq("user_password",encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
       if(user==null){
           throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户不存在或密码错误");
       }
        if (user.getUserStatus() == null || user.getUserStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已停用");
        }
        // 4. 保存用户的登录态

        StpUtil.login(user.getId());

        String tokenName = StpUtil.getTokenName();
        String tokenValue = StpUtil.getTokenValue();
        // 记录用户登录态到 Sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的信息过期时间一致
        return this.getLoginUserVO(user);
    }




    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.apply(StrUtil.isNotBlank(userRole),
                "EXISTS (SELECT 1 FROM user_role ur INNER JOIN sys_role r ON r.id = ur.role_id "
                        + "WHERE ur.user_id = `user`.id AND r.role_code = {0} "
                        + "AND r.status = 1 AND r.is_delete = 0)", userRole);
        if (StrUtil.isNotBlank(userAccount) && userAccount.equals(userName)) {
            queryWrapper.and(wrapper -> wrapper
                    .like("user_account", userAccount)
                    .or()
                    .like("user_name", userName));
        } else {
            queryWrapper.like(StrUtil.isNotBlank(userAccount), "user_account", userAccount);
            queryWrapper.like(StrUtil.isNotBlank(userName), "user_name", userName);
        }
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "user_profile", userProfile);

        Map<String, String> sortFieldMap = Map.of(
                "id", "id",
                "userAccount", "user_account",
                "userName", "user_name",
                "createTime", "create_time",
                "updateTime", "update_time"
        );
        String sortColumn = StrUtil.isNotBlank(sortField) ? sortFieldMap.get(sortField) : null;
        queryWrapper.orderBy(sortColumn != null, "ascend".equals(sortOrder), sortColumn);
        return queryWrapper;
    }

    /**
     * 获取加密后的密码
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 加盐，混淆密码
        final String SALT = "yupi";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    private void assignRoles(Long userId, List<String> roleCodes) {
        List<String> normalized = roleCodes == null ? List.of() : roleCodes.stream()
                .filter(StrUtil::isNotBlank).map(String::trim)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户至少需要一个角色");
        }
        List<SysRole> roles = sysRoleMapper.selectList(new QueryWrapper<SysRole>()
                .in("role_code", normalized).eq("status", 1));
        if (roles.size() != normalized.size()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "包含不存在或已停用的角色");
        }
        userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", userId));
        for (SysRole role : roles) {
            UserRole relation = new UserRole();
            relation.setUserId(userId);
            relation.setRoleId(role.getId());
            if (userRoleMapper.insert(relation) != 1) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "分配用户角色失败");
            }
        }
    }

}




