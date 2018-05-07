package com.github.drxaos.zc;

public class InitManager implements Manager {
    Db db;
    Game game;
    Level level;

    public InitManager(Db db, Game game, Level level) {
        this.db = db;
        this.game = game;
        this.level = level;
    }

    @Override
    public void manage(Game.State state) {
        level.generate(state, db.getActiveSessions());
    }
}
