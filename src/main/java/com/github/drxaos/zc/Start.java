package com.github.drxaos.zc;

public class Start {
    public static void main(String[] args) {
        Db db = new Db();
        Game game = new Game(50, 25);
        RandomLevel level = new RandomLevel(50, 50);
        InitManager init = new InitManager(db, game, level);
        CycleManager manager = new CycleManager(db, game, level);
        new Server(game, db, init, manager).start();
    }
}
