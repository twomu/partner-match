package com.gdut.job;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdut.common.ErrorCode;
import com.gdut.common.util.AlgorithmUtils;
import com.gdut.exception.BusinessException;
import com.gdut.globalConstants.GlobalConstants;
import com.gdut.pojo.User;
import com.gdut.pojo.vo.UserVO;
import com.gdut.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class UserMatchPreCache {
    @Resource
    private UserService userService;

    private List<Long> mainUsersId= GlobalConstants.mainUsersId;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;



    @Scheduled(cron = "0 58 23 * * ? ")
    public void PreCache(){
        RLock lock = redissonClient.getLock(GlobalConstants.matchLock);
        try {
            if(lock.tryLock(0,-1,TimeUnit.MILLISECONDS)){
                for (long userId : mainUsersId) {
                    String redisKey=GlobalConstants.matchLock+userId;
                    ValueOperations<String,Object> opsForValue = redisTemplate.opsForValue();
                    if(userId<0)throw new BusinessException(ErrorCode.SYSTEM_ERROR,GlobalConstants.matchLock+ "error");
                    User user = userService.getById(userId);
                    List<UserVO> userVOList = userService.matchUsers(20, user);
                    opsForValue.set(redisKey,userVOList,8,TimeUnit.HOURS);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            //如果是自己锁才可以释放
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }

        }
    }
}
