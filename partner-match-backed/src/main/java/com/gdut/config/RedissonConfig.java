package com.gdut.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.ScanResult;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

    private String port;
    private String host;
    private String password;


    @Bean
    public RedissonClient redissonClient(){
        // 1. Create config object
        Config config = new Config();
        String url=String.format("redis://%s:%s",host,port);
        config.useSingleServer().setAddress(url).setDatabase(3).setPassword(password);
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }


}
