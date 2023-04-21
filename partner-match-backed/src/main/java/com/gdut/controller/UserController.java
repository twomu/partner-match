package com.gdut.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gdut.common.ErrorCode;
import com.gdut.exception.BusinessException;
import com.gdut.globalConstants.GlobalConstants;
import com.gdut.pojo.User;
import com.gdut.pojo.requestBody.UserLoginRequest;
import com.gdut.pojo.requestBody.UserRegisterRequest;
import com.gdut.pojo.result.BaseResponse;
import com.gdut.pojo.vo.UserListVo;
import com.gdut.pojo.vo.UserVO;
import com.gdut.service.UserService;
import com.gdut.common.util.SafetyUserUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    private static final String USER_LONGIN="useLonginState";

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    /**
     * 注册用户
     * @param userRequestBody 请求注册输入的用户信息
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRequestBody){
        if(userRequestBody.getUserAccount()==null) throw new BusinessException(ErrorCode.NULL_ERROR);
        Long userId= userService.userRegister(userRequestBody);
        if(userId==null) throw new BusinessException(ErrorCode.SYSTEM_ERROR,"注册失败");
        return new BaseResponse<>(ErrorCode.SUCCESS,userId);
    }

    /**
     * 用户登录
     * @param userRequestBody 请求登录的用户信息
     * @param request 请求的http对象
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> userLongin(@RequestBody UserLoginRequest userRequestBody, HttpServletRequest request){
        if(userRequestBody.getUserAccount()==null || userRequestBody.getUserPassword()=="") {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        System.out.println("userRequestBody = " + userRequestBody + ", request = " + request);
        String userAccount=userRequestBody.getUserAccount();
        String password=userRequestBody.getUserPassword();
        User user = userService.userLongin(userAccount, password, request);
        return new BaseResponse<User>(ErrorCode.SUCCESS,user);
    }

    /**
     * 查询用户
     * @param username
     * @return
     */
    @GetMapping
    public List<User> searchUserByUsername(@RequestParam String username){
        if(!StringUtils.isNotBlank(username)) new BusinessException(ErrorCode.PARAMS_ERROR);
        QueryWrapper<User> query=new QueryWrapper<>();
        query.like("username",username);
        return userService.list(query);
    }

    /**
     * 删除用户
     * @param id
     * @return
     */
    @DeleteMapping("/delete")
    public BaseResponse deleteById(Long id){
        if(id<=0) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        boolean handleTag = userService.removeById(id);
        if(handleTag) return new BaseResponse(ErrorCode.SUCCESS);
        return new BaseResponse(ErrorCode.SYSTEM_ERROR);
    }

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LONGIN);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = currentUser.getId();
        // TODO 校验用户是否合法
        User user = userService.getById(userId);
        User safetyUser = SafetyUserUtils.toSafeTyUser(user);
        return new BaseResponse<User>(ErrorCode.SUCCESS,safetyUser);
    }

    @PostMapping("/logout")
    public BaseResponse<Integer> loginOut(HttpServletRequest request){
        request.getSession().removeAttribute(USER_LONGIN);
        return new BaseResponse<Integer>(ErrorCode.SUCCESS);
    }

    /**
     * 获取所有用户信息
     * @param request
     * @return
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> getUsers(HttpServletRequest request){
        Object userObj = request.getSession().getAttribute(USER_LONGIN);
        User currentUser = (User) userObj;
        long userId = currentUser.getId();
        List<User> list = userService.list();
        if(list==null) throw new BusinessException(ErrorCode.NULL_ERROR);
        return new BaseResponse<List<User>>(ErrorCode.SUCCESS,list);
    }

    @PostMapping ("/searchByCondition")
    public BaseResponse<List<User>> selectByConditions(@RequestBody User conditionUser){
        List<User> users = userService.searchByCondition(conditionUser);
        if(users==null) return new BaseResponse<>(ErrorCode.NULL_ERROR);
        List<User> safeTyUsers=new ArrayList<>();
        for (User user : users) {
            User safeTyUser = SafetyUserUtils.toSafeTyUser(user);
            safeTyUsers.add(safeTyUser);
        }
        return new BaseResponse<List<User>>(ErrorCode.SUCCESS,safeTyUsers);
    }

    /**
     * 批量删除，通过id数组
     * @param ids 要删除的id数组
     * @return
     */
    @DeleteMapping("/deleteByIds")
    public BaseResponse<Boolean> deleteByIds(@RequestBody int[] ids){
        Boolean deleteTag =  userService.deleteByIds(ids);
        if(deleteTag==false) return new BaseResponse<>(ErrorCode.SYSTEM_ERROR,false);
        return new BaseResponse<>(ErrorCode.SUCCESS,true);
    }

    /**
     * 标签查找
     * @param tagNames
     * @return
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchByTags(@RequestParam List<String> tagNames){
        if(tagNames.size()<=0) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        List<User> users = userService.searchByTags(tagNames);
        return new BaseResponse<List<User>>(ErrorCode.SUCCESS,users);
    }

    /**
     * 修改用户信息
     * @param user 要修改的用户
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody User user,HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        if(user==null || loginUser ==null)throw new BusinessException(ErrorCode.PARAMS_ERROR);
        Integer updateLows = userService.updateUser(user, loginUser);
        if(updateLows<=0) return new BaseResponse<Boolean>(ErrorCode.SYSTEM_ERROR,false);
        return new BaseResponse<>(ErrorCode.SUCCESS,true);
    }

    @GetMapping("/recommend")
    public BaseResponse<UserListVo> recommendUsers(@RequestParam Integer currentPage,@RequestParam Integer pageSize,HttpServletRequest request){
        User user= userService.getLoginUser(request);
        if(user==null)throw new BusinessException(ErrorCode.NOT_LOGIN);
        if(pageSize==null)pageSize=8;
        long userId=user.getId();
        String redisKey= GlobalConstants.recommendLock +userId;
        ValueOperations<String,Object> opsForValue = redisTemplate.opsForValue();
        //如果有直接读redis
        UserListVo userListVo = new UserListVo();
        Page<User> page =(Page<User>) opsForValue.get(redisKey);
        if (page!=null && page.getCurrent()==currentPage && page.getTotal()==pageSize){
            userListVo.setPage(page);
            return new BaseResponse<>(ErrorCode.SUCCESS,userListVo);
        }
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.ne("id",userId);
        //TODO 通过一定规则筛选出推荐的用户并计算总条数返回给前端

        int totalNum=30;
        userListVo.setTotalNum(totalNum);
        int showPageSize= totalNum%pageSize==0?(totalNum/pageSize):(totalNum/pageSize+1);
        userListVo.setShowPageSize(showPageSize);
        //没有则写缓存
        page=userService.page(new Page<User>(currentPage,pageSize),queryWrapper);
        try {
            opsForValue.set(redisKey, page, 30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        userListVo.setPage(page);
        return new BaseResponse(ErrorCode.SUCCESS,userListVo);
    }

    /**
     * 推荐匹配
     * @param num 匹配用户的数量
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<UserVO>> matchUsers(Integer num,HttpServletRequest request){
        if(num<=0 || num>20)throw new BusinessException(ErrorCode.PARAMS_ERROR,"num数量不合");
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null)throw new BusinessException(ErrorCode.NOT_LOGIN);
        List<UserVO> userVOList=userService.matchUsers(num,loginUser);
        return new BaseResponse<>(ErrorCode.SUCCESS,userVOList);
    }
}
