package com.gdut.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdut.common.ErrorCode;
import com.gdut.common.util.SafetyUserUtils;
import com.gdut.exception.BusinessException;
import com.gdut.pojo.Team;
import com.gdut.pojo.User;
import com.gdut.pojo.UserTeam;
import com.gdut.pojo.dto.TeamQuery;
import com.gdut.pojo.requestBody.*;
import com.gdut.pojo.result.BaseResponse;
import com.gdut.pojo.vo.TeamUserVO;
import com.gdut.pojo.vo.UserVO;
import com.gdut.service.TeamService;
import com.gdut.service.UserService;
import com.gdut.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/team")
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;
    @Resource
    private TeamService teamService;
    @Resource
    private UserTeamService userTeamService;


    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest,HttpServletRequest request){
        if(teamAddRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍参数错误");
        }
        Team team=new Team();
        BeanUtils.copyProperties(teamAddRequest,team);
        User loginUser=userService.getLoginUser(request);
        Long teamId = teamService.addTeam(team, loginUser);
        if(teamId==null)return new BaseResponse<Long>(ErrorCode.SYSTEM_ERROR,teamId);
        return new BaseResponse<Long>(ErrorCode.SUCCESS,teamId);
    }

    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeam(TeamQuery teamQuery, HttpServletRequest request){
        if(teamQuery==null)throw new BusinessException(ErrorCode.PARAMS_ERROR,"查询参数为空");
        List<TeamUserVO> teamList=teamService.listTeam(teamQuery,request);
        return new BaseResponse<List<TeamUserVO>>(ErrorCode.SUCCESS,teamList);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdate, HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if(teamUpdate==null)throw new BusinessException(ErrorCode.PARAMS_ERROR,"无修改参数");
        Boolean updateTag= teamService.updateTeam(teamUpdate,loginUser);
        if(!updateTag)return new BaseResponse<>(ErrorCode.SYSTEM_ERROR, false);
        return new BaseResponse<Boolean>(ErrorCode.SUCCESS,true);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam( @RequestBody TeamJoinRequest joinRequest,HttpServletRequest request){
        if(joinRequest==null)throw new BusinessException(ErrorCode.PARAMS_ERROR,"加入队伍参数为空");
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null)throw new BusinessException(ErrorCode.NOT_LOGIN);
        Boolean joinTag=teamService.joinTeam(joinRequest,loginUser);
        if(!joinTag)return new BaseResponse<>(ErrorCode.SYSTEM_ERROR, false);
        return new BaseResponse<Boolean>(ErrorCode.SUCCESS,true);
    }
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest,HttpServletRequest request){
        Long teamId=teamQuitRequest.getTeamId();
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null)throw new BusinessException(ErrorCode.NOT_LOGIN);
        if(teamId==null) throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍id不能为空");
        Boolean quitTag=teamService.quitTeam(teamId,loginUser);
        if(!quitTag){
            return new BaseResponse<Boolean>(ErrorCode.SYSTEM_ERROR,false);
        }
        return new BaseResponse<>(ErrorCode.SUCCESS,true);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody TeamDeleteRequest teamDeleteRequest,HttpServletRequest request){
        if(teamDeleteRequest==null)throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        Long teamId = teamDeleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null)throw new BusinessException(ErrorCode.NOT_LOGIN);
        if(teamId==null) throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍id不能为空");
        Boolean quitTag=teamService.deleteTeam(teamId,loginUser);
        if(!quitTag){
            return new BaseResponse<Boolean>(ErrorCode.SYSTEM_ERROR,false);
        }
        return new BaseResponse<>(ErrorCode.SUCCESS,true);
    }

    /**
     * 获取队伍信息
     * @param id 队伍id
     * @return
     */
    @GetMapping("get")
    public BaseResponse<TeamUserVO> getTeam(@RequestParam Long id,HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null)throw new BusinessException(ErrorCode.NOT_LOGIN);
        if(id==null)throw new BusinessException(ErrorCode.PARAMS_ERROR,"无队伍id");
        Team team = teamService.getById(id);
        if(team==null)throw new BusinessException(ErrorCode.PARAMS_ERROR,"无该队伍");
        TeamUserVO teamUserVO=new TeamUserVO();
        if(teamService.hasJoin(id,loginUser)){
            BeanUtils.copyProperties(team,teamUserVO);
            Long userId = team.getUserId();
            User createUser = userService.getById(userId);
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(createUser,userVO);
            teamUserVO.setCreateUser(userVO);
        }
        return new BaseResponse<>(ErrorCode.SUCCESS,teamUserVO);
    }

    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> myCreateTeam( TeamQuery teamQuery,HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null)throw new BusinessException(ErrorCode.NOT_LOGIN);
        if(teamQuery==null)throw new BusinessException(ErrorCode.PARAMS_ERROR,"查询参数为空");
        List<TeamUserVO> teamUserVOList = teamService.listTeam(teamQuery, request);
        teamUserVOList=teamUserVOList.stream().filter(teamUserVO -> {
            Long createUserId = teamUserVO.getCreateUser().getId();
            if(createUserId==null)throw new BusinessException(ErrorCode.SYSTEM_ERROR,"无创建者id");
            return createUserId.equals(loginUser.getId());
        }).collect(Collectors.toList());
        return new BaseResponse<>(ErrorCode.SUCCESS,teamUserVOList);
    }

    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> myJoinTeam(TeamQuery teamQuery,HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null)throw new BusinessException(ErrorCode.NOT_LOGIN);
        List<TeamUserVO> teamUserVOList = teamService.listTeam(teamQuery, request);
        //过滤出我加入的队伍
        teamUserVOList=teamUserVOList.stream().filter(teamUserVO -> {
            Long teamId = teamUserVO.getId();
            if(teamId==null)throw new BusinessException(ErrorCode.SYSTEM_ERROR,"队伍id为空");
            if(teamService.hasJoin(teamId,loginUser)){
                Long createId = teamUserVO.getCreateUser().getId();
                return !createId.equals(loginUser.getId());
            }
            else return false;
        }).collect(Collectors.toList());
        return new BaseResponse<>(ErrorCode.SUCCESS,teamUserVOList);
    }
}
