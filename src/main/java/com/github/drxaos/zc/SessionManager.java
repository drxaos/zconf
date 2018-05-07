package com.github.drxaos.zc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SessionManager implements Manager, SessionControl {
    Db db;
    Game game;
    Level level;
    long sessionMs;
    List<Integer> start = new ArrayList<>();

    public SessionManager(Db db, Game game, Level level, long sessionMs) {
        this.db = db;
        this.game = game;
        this.level = level;
        this.sessionMs = sessionMs;
    }

    @Override
    public void manage(Game.State state) {
        // Стартуем игроков
        for (Integer zid : start) {
            level.addZ(state, zid);
        }
        start.clear();

        // Завершаем сессии
        Collection<Integer> expiredSessions = db.expiredSessions();
        for (Integer session : expiredSessions) {
            db.evictSession(session);
            db.recordScore(session);
            state.removeZ(session);
        }

        // Пересчитываем рейтинг
        db.cacheRating();
    }

    @Override
    public long start(int zid) {
        db.startSession(zid, System.currentTimeMillis() + sessionMs);
        start.add(zid);
        return sessionMs;
    }

    @Override
    public long timeLeft(int zid) {
        return Math.max(0L, db.getSessionEnd(zid) - System.currentTimeMillis());
    }
}
