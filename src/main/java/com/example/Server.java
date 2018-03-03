package com.example;

import org.rapidoid.http.Req;
import org.rapidoid.setup.On;

public class Server {

    public static void main(String[] args) throws InterruptedException {

        On.get("/").html((Req x) -> x.response().redirect("/index.html"));

        Game game = new Game(25, 50);
        for (int y = 0; y < 25; y++) {
            for (int x = 0; x < 50; x++) {
                if (Math.random() < 0.05) {
                    game.set(x, y, Game.TREE);
                }
                if (Math.random() < 0.1) {
                    game.set(x, y, Game.G);
                }
            }
        }
        game.set(5, 5, Game.O1);
        game.set(45, 20, Game.O2);
        game.set(25, 10, Game.Z);
        game.set(25, 15, Game.Z + 100);

        On.get("/size/{id}").plain((Integer id) -> {
            // [W, H]
            return "[" + game.getW() + ", " + game.getH() + "]";
        });

        On.get("/score").plain((Integer id) -> {
            // [Score1, Score2]
            return "[" + game.getScore1() + ", " + game.getScore2() + "]";
        });

        On.get("/look/{id}").plain((Integer id) -> {
            // [map] OR -1
//            long start = System.nanoTime();
            String map = game.look(id);
//            System.out.println(System.nanoTime() - start);
            return map;
        });

        On.get("/up/{id}").plain((Integer id) -> {
            // MS OR -1
//            long start = System.nanoTime();
            String res = game.move(id, 0, -1);
//            System.out.println(System.nanoTime() - start);
            return res;
        });

        On.get("/down/{id}").plain((Integer id) -> {
            // MS OR -1
//            long start = System.nanoTime();
            String res = game.move(id, 0, 1);
//            System.out.println(System.nanoTime() - start);
            return res;
        });

        On.get("/left/{id}").plain((Integer id) -> {
            // MS OR -1
//            long start = System.nanoTime();
            String res = game.move(id, -1, 0);
//            System.out.println(System.nanoTime() - start);
            return res;
        });

        On.get("/right/{id}").plain((Integer id) -> {
            // MS OR -1
//            long start = System.nanoTime();
            String res = game.move(id, 1, 0);
//            System.out.println(System.nanoTime() - start);
            return res;
        });

        while (true) {
            game.snap();
            Thread.sleep(100);
        }
    }

}
