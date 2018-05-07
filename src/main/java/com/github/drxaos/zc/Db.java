package com.github.drxaos.zc;

import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    public Integer getZid(String key) {
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

    public void incScore(int zid) {
        scores.addScoreAsync(zid, 1);
    }

    public Collection<Integer> expiredSessions() {
        return sessions.valueRangeReversed(0, true, System.currentTimeMillis(), true);
    }

    public void evictSession(Integer session) {
        sessions.remove(session);
    }

    public void recordScore(Integer zid) {
        Integer currentScore = getCurrentScore(zid);
        Integer maxScore = getMaxScore(zid);
        if (currentScore > maxScore) {
            records.add(currentScore, zid);
        }
        scores.remove(zid);
    }

    public void cacheRating() {
        List<String> result = new ArrayList<>();
        Collection<ScoredEntry<Integer>> scoredEntries = records.entryRangeReversed(0, 100);
        for (ScoredEntry<Integer> entry : scoredEntries) {
            long score = entry.getScore().longValue();
            Integer zid = entry.getValue();
            String name = names.get(zid).replace("\"", "");
            result.add("{\"z\":" + zid + ",\"s\":" + score + ",\"n\":\"" + name + "\"}");
        }
        rating = result.toString();
    }

    public void startSession(int zid, long sessionEnd) {
        sessions.add(sessionEnd, zid);
        scores.add(0, zid);
    }

    public long getSessionEnd(int zid) {
        return Optional.ofNullable(sessions.getScore(zid)).orElse(0d).longValue();
    }
}
