package com.github.drxaos.zc;

import java.util.ArrayList;

public class ScoreManager implements Manager {
    Db db;
    Game game;
    Level level;
    ArrayList<Long> add = new ArrayList<>();

    public ScoreManager(Db db, Game game, Level level) {
        this.db = db;
        this.game = game;
        this.level = level;
    }

    @Override
    public void manage(Game.State state) {

        // Сохраняем очки
        while (state.hasScores()) {
            Game.Score score = state.nextScore();
            db.incScore(score.zid);
            add.add(System.currentTimeMillis() + 1500);
        }

        // Обновляем капусту
        if (add.size() > 0 && add.get(0) < System.currentTimeMillis()) {
            add.remove(0);
            level.addG(state, 1);
        }
    }
}
