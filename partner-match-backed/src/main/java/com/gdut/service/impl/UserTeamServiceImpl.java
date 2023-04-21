package com.gdut.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdut.pojo.UserTeam;
import com.gdut.service.UserTeamService;
import com.gdut.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author Li
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2023-04-15 20:51:33
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




