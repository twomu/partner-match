package com.gdut.service;

import com.gdut.pojo.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gdut.pojo.User;
import com.gdut.pojo.dto.TeamQuery;
import com.gdut.pojo.requestBody.TeamJoinRequest;
import com.gdut.pojo.requestBody.TeamUpdateRequest;
import com.gdut.pojo.vo.TeamUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Li
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2023-04-15 20:51:14
*/
public interface TeamService extends IService<Team> {
    long addTeam(Team team, User loginUser);

    List<TeamUserVO> listTeam(TeamQuery teamQuery, HttpServletRequest request);

    Boolean updateTeam(TeamUpdateRequest teamUpdate,User loginUser);

    Boolean joinTeam(TeamJoinRequest joinRequest, User loginUser);

    Boolean quitTeam(Long teamId,User loginUser);

    Boolean deleteTeam(Long teamId, User loginUser);

    Boolean hasJoin(Long teamId,User loginUser);

    Integer hasJoinNum(Long teamId);

}
