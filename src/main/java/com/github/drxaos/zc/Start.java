package com.github.drxaos.zc;

public class Start {
    public static void main(String[] args) {
        Db db = new Db();
        Game game = new Game(50, 25);
        new RandomLevel().generate(game, 50, 50, db.getActiveSessions());
        new Server(game, db).start();
    }
}
