package com.travelapp.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelapp.user.entity.User;

/**
 * BaseMapper already provides common methods like selectById/insert/updateById/selectOne/selectCount,
 * so simple queries don't need SQL; for complex queries, build a LambdaQueryWrapper in the service layer.
 * Only add a method here plus the corresponding XML or @Select annotation when a custom SQL query is actually needed.
 */
public interface UserMapper extends BaseMapper<User> {
}
