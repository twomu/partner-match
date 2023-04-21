package com.gdut;

import com.alibaba.fastjson.JSON;
import com.gdut.pojo.User;
import com.gdut.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
class PartnerMatchApplicationTest {
    @Resource
    private UserServiceImpl userService;
    private ExecutorService executorService = new ThreadPoolExecutor(40, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

    @Autowired
    private RedissonClient redissonClient;

    @Test
    void contextLoads() {
        User user=new User();
        user.setUserAccount("899");
        user.setUserPassword("123");
        String userStr = JSON.toJSONString(user);
        System.out.println(userStr);
    }

    @Test
    void testAddUser(){
       User user=new User();
       user.setUsername("zch");
       user.setUserAccount("11111");
       user.setAvatarUrl("https://t10.baidu.com/it/u=3987115131,2088942022&fm=58");
       user.setGender(0);
       user.setUserPassword("123");
       user.setPhone("12233");
       user.setEmail("123334");
        boolean testTag = userService.save(user);
        System.out.println(user.getId());
        Assertions.assertEquals(true,testTag);
    }

    @Test
    void testCommon(){
        int[] ids = {1,2,3};
        String s = JSON.toJSONString(ids);
        System.out.println(s);
    }


    @Test
    void testSearchByTags(){
        List<String> tagNames   = new ArrayList<>();
        tagNames.add("java");
        tagNames.add("c++");
        List<User> users = userService.searchByTags(tagNames);
        System.out.println(users);
    }

    /**
     * 并发批量插入用户
     */
    @Test
    public void doConcurrencyInsertUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 分十组
        int batchSize = 50;
        int j = 0;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            List<User> userList = new ArrayList<>();
            while(true) {
                j++;
                User user = new User();
                user.setUsername("贾名");
                user.setUserAccount("ikun");
                user.setAvatarUrl("https://img1.baidu.com/it/u=38051914,745056107&fm=253&fmt=auto&app=120&f=JPEG?w=800&h=802");
                user.setGender(0);
                user.setUserPassword("b0dd3697a192885d7c055db46155b26a");
                user.setPhone("123");
                user.setEmail("123@qq.com");
                user.setTags("[]");
                user.setUserStatus(0);
                user.setUserRole(0);
                user.setPlanetCode("10001");
                userList.add(user);
                if (j % batchSize == 0) {
                    break;
                }
            }
            System.out.println(userList.size());
            // 异步执行
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("threadName: " +Thread.currentThread().getName());
                userService.saveBatch(userList, batchSize);
            }, executorService);
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        // 20 秒 10 万条
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }

    @Test
    public void testRedisson(){
        RList<String> rList = redissonClient.getList("zs");
        rList.add("zs");
        System.out.println(rList);
    }

}
