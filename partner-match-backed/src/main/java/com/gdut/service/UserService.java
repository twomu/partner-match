package com.gdut.service;

import com.gdut.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gdut.pojo.requestBody.UserRegisterRequest;
import com.gdut.pojo.vo.UserVO;
import io.swagger.models.auth.In;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Li
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2023-03-19 16:06:50
*/
public interface UserService extends IService<User> {

    Long userRegister(UserRegisterRequest registerRequest);

    User userLongin(String userAccount, String password, HttpServletRequest request);

    List<User> searchByCondition(User conditionUser);

    Boolean deleteByIds(int[] ids);

    List<User> searchByTags(List<String> tagNames);

    User getLoginUser(HttpServletRequest request);

    Boolean isAdmin(HttpServletRequest request);

    Boolean isAdmin(User user);

    Integer updateUser(User user,User loginUser);

    List<User> recommendUsers(int currentPage,int pageSize);


    List<UserVO> matchUsers(Integer num, User loginUser);
}
