package com.gdut.job;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdut.globalConstants.GlobalConstants;
import com.gdut.pojo.User;
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
public class UserRecommendPreCache {
    @Resource
    private UserService userService;

    private List<Long> mainUsersId= GlobalConstants.mainUsersId;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;



    @Scheduled(cron = "0 59 23 * * ? ")
    public void PreCache(){
        RLock lock = redissonClient.getLock(GlobalConstants.recommendLock);
        try {
            if(lock.tryLock(0,-1,TimeUnit.MILLISECONDS)){
                for (long userId : mainUsersId) {
                    String redisKey=GlobalConstants.recommendLock+userId;
                    ValueOperations<String,Object> opsForValue = redisTemplate.opsForValue();
                    QueryWrapper<User> queryWrapper=new QueryWrapper<>();
                    //写缓存
                    Page<User> page=userService.page(new Page<User>(1,20),queryWrapper);
                    try {
                        opsForValue.set(redisKey, page, 8, TimeUnit.HOURS);
                    } catch (Exception e) {
                        log.error("redis set key error", e);
                    }
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
