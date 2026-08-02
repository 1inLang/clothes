package com.lsp.clothes.mapper;

import com.lsp.clothes.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 琳琅
* @description 针对表【user(用户表)】的数据库操作Mapper
* @createDate 2026-07-28 13:13:25
* @Entity com.lsp.clothes.entity.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {

}




