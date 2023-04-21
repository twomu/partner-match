package com.gdut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdut.common.ErrorCode;
import com.gdut.exception.BusinessException;
import com.gdut.pojo.Team;
import com.gdut.pojo.User;
import com.gdut.pojo.UserTeam;
import com.gdut.pojo.dto.TeamQuery;
import com.gdut.pojo.enums.TeamStatusEnum;
import com.gdut.pojo.requestBody.TeamJoinRequest;
import com.gdut.pojo.requestBody.TeamUpdateRequest;
import com.gdut.pojo.vo.TeamUserVO;
import com.gdut.pojo.vo.UserVO;
import com.gdut.service.TeamService;
import com.gdut.mapper.TeamMapper;
import com.gdut.service.UserService;
import com.gdut.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author Li
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2023-04-15 20:51:14
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        //系统（接口）设计
        //1、创建队伍
        //用户可以 创建 一个队伍，设置队伍的人数、队伍名称（标题）、描述、超时时间 P0
        //队长、剩余的人数
        //请求参数是否为空？
        if(team==null)throw new BusinessException(ErrorCode.PARAMS_ERROR);
        //是否登录，未登录不允许创建
        if(loginUser==null)throw new BusinessException(ErrorCode.NOT_LOGIN);
        //校验信息
        //队伍人数 > 1 且 <= 20
        long userId=loginUser.getId();
        Integer maxNum=Optional.ofNullable(team.getMaxNum()).orElse(1);
        if(maxNum<1 || maxNum>20)throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍人数错误");
        //队伍标题 <= 20
        String teamName=team.getName();
        if(StringUtils.isBlank(teamName) || teamName.length()>20  ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍标题错误");
        }
        //描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
        //status 是否公开（int）不传默认为 0（公开）
        Integer status=Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        //   5. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置不正确");
            }
        }
        // 6. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间小于当前时间");
        }
        // 7. 校验用户最多创建 5 个队伍
        // todo 有 bug，可能同时创建 100 个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建 5 个队伍");
        }
        // 8. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        // 9. 插入用户  => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return teamId;

    }

    @Override
    public List<TeamUserVO> listTeam(TeamQuery teamQuery, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<Team> query=new QueryWrapper<>();
        Boolean isAdmin = userService.isAdmin(request);
        String teamName = teamQuery.getName();
        if(StringUtils.isNotBlank(teamName)){
            query.like("name",teamName);
        }
        Long id = teamQuery.getId();
        if(id!=null && id>0)query.eq("id",id);
        List<Long> idList = teamQuery.getIdList();
        if(idList!=null){
            query.in("id",idList);
        }
        Integer status = teamQuery.getStatus();
        if(status!=null){
            TeamStatusEnum teamStatus = TeamStatusEnum.getEnumByValue(status);
            if(!isAdmin && (teamStatus.equals(TeamStatusEnum.PRIVATE) || teamStatus.equals(TeamStatusEnum.SECRET))){
                throw new BusinessException(ErrorCode.NO_AUTH,"不是管理员");
            }
            query.eq("status",status);
        }
        Integer maxNum = teamQuery.getMaxNum();
        if(maxNum!=null && maxNum>0){
            query.eq("maxNum",maxNum);
        }
        Long userId = teamQuery.getUserId();
        // 根据创建人来查询
        if (userId != null && userId > 0) {
            query.eq("userId", userId);
        }
        String description = teamQuery.getDescription();
        if (StringUtils.isNotBlank(description)) {
            query.like("description", description);
        }
        String searchText = teamQuery.getSearchText();
        if (StringUtils.isNotBlank(searchText)) {
            query.and(qw -> qw.like("name", searchText).or().like("description", searchText));
        }
        query.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> teamList=this.list(query);
        teamList=Optional.ofNullable(teamList).orElse(new ArrayList<>());
        List<TeamUserVO> teamUserVOList=new ArrayList<>();
        for (Team team : teamList) {
            TeamUserVO teamUserVO=new TeamUserVO();
            BeanUtils.copyProperties(team,teamUserVO);
            userId=team.getUserId();
            User user=userService.getById(userId);
            if(user!=null){
                UserVO userVO=new UserVO();
                BeanUtils.copyProperties(user,userVO);
                teamUserVO.setCreateUser(userVO);
            }
            if(this.hasJoin(team.getId(),loginUser)){
                teamUserVO.setHasJoin(true);
            }
            Long teamId = teamUserVO.getId();
            teamUserVO.setHasJoinNum(this.hasJoinNum(teamId));
            teamUserVOList.add(teamUserVO);
        }

        return teamUserVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTeam(TeamUpdateRequest teamUpdate,User loginUser) {
        if(teamUpdate.getId()==null)throw new BusinessException(ErrorCode.PARAMS_ERROR,"无队伍id");
        Team team = this.getById(teamUpdate.getId());
        if(team==null)throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍不存在");
        if(!userService.isAdmin(loginUser) && team.getUserId()!=loginUser.getId()){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        TeamStatusEnum teamStatus = TeamStatusEnum.getEnumByValue(teamUpdate.getStatus());
        if(TeamStatusEnum.SECRET.equals(teamStatus) && StringUtils.isBlank(teamUpdate.getPassword())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"加密必须有密码");
        }
        Team teamTemp = new Team();
        BeanUtils.copyProperties(teamUpdate,teamTemp);
        return this.updateById(teamTemp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean joinTeam(TeamJoinRequest joinRequest, User loginUser) {
        RLock lock = redissonClient.getLock("parnet_match:team:joinTeam");
        Long teamId = joinRequest.getTeamId();
        Team team = this.getById(teamId);
        if (team == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "无该队伍");
        TeamStatusEnum teamStatus = TeamStatusEnum.getEnumByValue(team.getStatus());
        if (TeamStatusEnum.PRIVATE.equals(teamStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私密队伍");
        }
        if (TeamStatusEnum.SECRET.equals(teamStatus) && !team.getPassword().equals(joinRequest.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不匹配");
        }
        try {
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    QueryWrapper<UserTeam> query = new QueryWrapper();
                    query.eq("teamId", teamId);
                    int teamHasNum = userTeamService.count(query);
                    if (teamHasNum >= team.getMaxNum()) throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    Date expireTime = team.getExpireTime();
                    if (expireTime.before(new Date())) throw new BusinessException(ErrorCode.PARAMS_ERROR, "已过期的队伍");
                    if (team.getUserId() == loginUser.getId()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能加入自己的队伍");
                    }
                    query.eq("userId", loginUser.getId());
                    int hadJoin = userTeamService.count(query);
                    if (hadJoin > 0) throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能加入已加入的队伍");
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(loginUser.getId());
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }

    }

    @Override
    public Boolean quitTeam(Long teamId,User loginUser) {
        Long userId = loginUser.getId();
        Team team = this.getById(teamId);
        if(team==null)throw new BusinessException(ErrorCode.PARAMS_ERROR,"无该队伍");
        QueryWrapper<UserTeam> query=new QueryWrapper<>();
        query.eq("teamId",teamId);
        query.eq("userId",userId);
        int count = userTeamService.count(query);
        if(count==0){
            throw new BusinessException(ErrorCode.NO_AUTH,"用户不在该队伍中");
        }
        QueryWrapper<UserTeam> queryNum =new QueryWrapper<>();
        queryNum.eq("teamId",teamId);
        int teamHasUsers = userTeamService.count(queryNum);
        if(teamHasUsers==1){
            this.removeById(teamId);
        }else {
            if(team.getUserId().equals(loginUser.getId())){
                QueryWrapper<UserTeam> userTeamQueryWrapper=new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId",teamId);
                userTeamQueryWrapper.orderByAsc("id");
                List<UserTeam> list = userTeamService.list(userTeamQueryWrapper);
                if(CollectionUtils.isEmpty(list) || list.size()<=1){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"队伍集合错误");
                }
                UserTeam userTeam = list.get(1);
                Long newUserId = userTeam.getUserId();
                team.setUserId(newUserId);
                boolean updateTag = this.updateById(team);
                if(!updateTag){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新队长失败");
                }
            }
        }
        return userTeamService.remove(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTeam(Long teamId, User loginUser) {
        Long userId = loginUser.getId();
        Team team = this.getById(teamId);
        if(team==null)throw new BusinessException(ErrorCode.PARAMS_ERROR,"无该队伍");
        if(!team.getUserId().equals(userId)){
            throw new BusinessException(ErrorCode.NO_AUTH,"你不是该队伍队长");
        }
        QueryWrapper<UserTeam> userTeamQuery=new QueryWrapper<>();
        userTeamQuery.eq("teamId",teamId);
        boolean rmTeam = this.removeById(teamId);
        boolean rmUserTeam = userTeamService.remove(userTeamQuery);
        if(rmTeam && rmUserTeam){
            return true;
        }
        return false;
    }

    @Override
    public Boolean hasJoin(Long teamId, User loginUser) {
        if(teamId==null || loginUser==null)throw new BusinessException(ErrorCode.PARAMS_ERROR);
        Long userId = loginUser.getId();
        if(userId==null)throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户id为空");
        QueryWrapper<UserTeam> query=new QueryWrapper<>();
        query.eq("teamId",teamId);
        query.eq("userId",userId);
        int count = userTeamService.count(query);
        if(count==1)return true;
        return false;
    }

    @Override
    public Integer hasJoinNum(Long teamId) {
        if(teamId==null)throw new BusinessException(ErrorCode.PARAMS_ERROR,"无该队伍");
        QueryWrapper<UserTeam> query=new QueryWrapper<>();
        query.eq("teamId",teamId);
        return userTeamService.count(query);
    }


}




