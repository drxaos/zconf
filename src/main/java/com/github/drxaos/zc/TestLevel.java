package com.github.drxaos.zc;

import java.awt.*;
import java.util.*;

public class TestLevel {

    public static void main(String[] args) throws Exception {

        Game game = new Game(50, 25);

        Set<Point> points = new HashSet<>();
        Point o1, o2;
        Random r = new Random(System.currentTimeMillis());
        points.add(o1 = new Point((int) (r.nextDouble() * 10) + 10, (int) (r.nextDouble() * 5) + 5));
        points.add(o2 = new Point((int) (r.nextDouble() * 10) + 30, (int) (r.nextDouble() * 5) + 15));

        while (points.size() < 50 + 50 + 20 + 20 + 1 + 1 + 1) {
            points.add(new Point((int) (r.nextDouble() * 50), (int) (r.nextDouble() * 25)));
        }

        game.set(o1.x, o1.y, Game.O1);
        game.set(o2.x, o2.y, Game.O2);
        points.remove(o1);
        points.remove(o2);

        ArrayList<Point> list = new ArrayList<>(points);
        Collections.shuffle(list);

        int t = 0, g = 0, z1 = 0, z2 = 0;
        for (Point p : list) {
            if (t < 50) {
                game.set(p.x, p.y, Game.TREE);
                t++;
            } else if (g < 50) {
                game.set(p.x, p.y, Game.G);
                g++;
            } else {
                String paswd = null;
                while (paswd == null || game.auth(paswd) > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 10; i++) {
                        sb.append((char) ('A' + (int) (r.nextDouble() * 26)));
                    }
                    paswd = sb.toString();
                }
                if (z1 < 20) {
                    game.putKey(paswd, Game.Z + z1);
                    if (z1 == 0) {
                        System.out.println("Z100: " + paswd);
                    }
                    game.set(p.x, p.y, Game.Z + z1++);
                } else if (z2 < 20) {
                    game.putKey(paswd, Game.Z * 2 + z2);
                    if (z2 == 0) {
                        System.out.println("Z200: " + paswd);
                    }
                    game.set(p.x, p.y, Game.Z + 100 + z2++);
                } else {
                    game.putKey(paswd, Game.OBSERVER);
                    System.out.println("OBSERVER: " + paswd);
                }
            }
        }

        new Server(game).port(9999).start();
    }

}
