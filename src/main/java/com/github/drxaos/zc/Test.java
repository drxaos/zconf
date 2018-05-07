package com.github.drxaos.zc;

import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class Test {
    public static void main(String[] args) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        RedissonClient redisson = Redisson.create(config);
        RMap<String, Integer> auth = redisson.getMap("auth");

        auth.put("AHGFSASD", 103);

        redisson.shutdown();
    }
}
