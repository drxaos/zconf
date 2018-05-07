package com.github.drxaos.zc;

public class CycleManager implements Manager {
    Db db;
    Game game;
    Level level;

    public CycleManager(Db db, Game game, Level level) {
        this.db = db;
        this.game = game;
        this.level = level;
    }

    @Override
    public void manage(Game.State state) {

        int g = 0;

        // Сохраняем очки
        while (state.hasScores()) {
            Game.Score score = state.nextScore();
            db.incScore(score.zid);
            g++;
        }

        // Обновляем капусту
        level.addG(state, g);

        // Завершаем сессии

        // Пересчитываем рейтинг

    }
}
