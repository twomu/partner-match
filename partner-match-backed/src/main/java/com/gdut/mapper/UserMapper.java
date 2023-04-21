package com.gdut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdut.pojo.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper extends BaseMapper<User> {
    List<User> findByCondition(User user);

    int deleteByIds(@Param("ids") int[] ids);

    List<User> selectPages(int first,int pageSize);

}
