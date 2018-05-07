package com.github.drxaos.zc;

import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.Collection;
import java.util.Optional;

public class Db {
    protected RedissonClient r;
    protected RMap<String, Integer> auth;
    protected RMap<Integer, String> names;
    protected RMap<Integer, String> infos;
    protected RScoredSortedSet<Integer> records;
    protected RScoredSortedSet<Integer> scores;
    protected RScoredSortedSet<Integer> sessions;
    private String rating = "[]";

    public Db() {
        this("127.0.0.1");
    }

    public Db(String rAddress) {
        this(rAddress, 6379);
    }

    public Db(String rAddress, int rPort) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + rAddress + ":" + rPort);
        r = Redisson.create(config);
        auth = r.getMap("auth");
        names = r.getMap("names");
        infos = r.getMap("infos");
        records = r.getScoredSortedSet("records");
        scores = r.getScoredSortedSet("scores");
        sessions = r.getScoredSortedSet("sessions");
    }

    public Collection<Integer> getActiveSessions() {
        return sessions.readAll();
    }

    public int getZid(String key) {
        return auth.get(key);
    }

    public Integer getCurrentScore(int zid) {
        return Optional.ofNullable(scores.getScore(zid)).orElse(0d).intValue();
    }

    public Integer getMaxScore(int zid) {
        return Optional.ofNullable(records.getScore(zid)).orElse(0d).intValue();
    }

    public String getRating() {
        return rating;
    }
}
