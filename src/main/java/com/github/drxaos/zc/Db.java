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
    public static final int ANS_WRONG_KEY = -4; // ключ не найден
    public static final int ANS_NOT_REGISTERED = -5; // не пройдена регистрация
    public static final int ANS_NO_SESSION = -6; // игровая сессия не начата

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

    public void shutdown() {
        r.shutdown();
    }

    public Collection<Integer> getActiveSessions() {
        return sessions.readAll();
    }

    public Integer getZid(String key) {
        Integer zid = auth.get(key);
        if (zid == null) {
            return ANS_WRONG_KEY;
        }
        if (names.get(zid) == null) {
            return ANS_NOT_REGISTERED;
        }
        return zid;
    }

    public void addKey(int zid, String key) {
        auth.put(key, zid);
    }

    public void resetPlayer(int zid) {
        names.remove(zid);
        infos.remove(zid);
        records.add(0, zid);
        scores.remove(zid);
        sessions.remove(zid);
    }

    public String register(String key, String name, String email, String phone, String comment) {
        Integer zid = auth.get(key);
        if (name == null || name.trim().length() <= 3 || !name.matches(".*[а-яА-ЯёЁ].*")) {
            return "wrong name";
        }
        if (zid == null || zid < Game.Z) {
            return "wrong zid";
        }
        if (names.get(zid) != null) {
            return "already registered";
        }
        names.put(zid, name);
        infos.put(zid, "" + name + " / " + email + " / " + phone + " / " + comment);
        return "ok";
    }

    public void setName(int zid, String name) {
        names.put(zid, name);
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
            if (score == 0L) {
                continue;
            }
            Integer zid = entry.getValue();
            String name = Optional.ofNullable(names.get(zid)).orElse("").replace("\"", "");
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

    public void clearKeys() {
        auth.clear();
    }
}
