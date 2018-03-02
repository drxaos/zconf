package com.example;

import org.rapidoid.setup.On;

public class Server {

    public static void main(String[] args) throws InterruptedException {
        Game game = new Game();
        game.set(1, 1, Game.TREE);
        game.set(1, 3, Game.TREE);
        game.set(4, 4, Game.TREE);
        game.set(0, 2, Game.O1);
        game.set(3, 2, Game.O2);
        game.set(2, 2, Game.Q);
        game.set(2, 4, Game.Q);
        game.set(3, 4, Game.Q);
        game.set(0, 0, Game.Z);

        On.get("/look/{id}").plain((Integer id) -> {

            return game.look(id);
        });

        On.get("/up/{id}").plain((Integer id) -> {

            return game.move(id, 0, -1);
        });

        On.get("/down/{id}").plain((Integer id) -> {

            return game.move(id, 0, 1);
        });

        On.get("/left/{id}").plain((Integer id) -> {

            return game.move(id, -1, 0);
        });

        On.get("/right/{id}").plain((Integer id) -> {

            return game.move(id, 1, 0);
        });

        while (true) {
            game.snap();
            Thread.sleep(100);
        }
    }

}
