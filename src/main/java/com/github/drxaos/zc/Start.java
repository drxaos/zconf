package com.github.drxaos.zc;

public class Start {
    public static void main(String[] args) {
        int w = 50, h = 25;
        int trees = 20, g = 20;
        int sessionLength = 3 * 60 * 1000;

        Db db = new Db();
        Game game = new Game(w, h);
        RandomLevel level = new RandomLevel(trees, g);
        InitManager init = new InitManager(db, game, level);
        ScoreManager scoreManager = new ScoreManager(db, game, level);
        SessionManager sessionManager = new SessionManager(db, game, level, sessionLength);
        new Server(game, db, init, new Manager[]{scoreManager, sessionManager}, sessionManager).start();
    }
}
