package com.gdut.once;

import com.gdut.pojo.User;
import com.gdut.service.UserService;
import com.gdut.service.impl.UserServiceImpl;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 批量插入用户，可以通过定时任务来操作
 */
public class BatchSaveUsers {

    @Resource
    private UserServiceImpl userService;
    private ExecutorService executorService = new ThreadPoolExecutor(40, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));
    /**
     * 并发批量插入用户
     */

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
}
