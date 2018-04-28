package com.github.drxaos.zc;

import java.awt.*;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public class DemoLevel {

    public static void main(String[] args) throws Exception {

        int oCount = 1 + 1;
        int z1Count = 20;
        int z2Count = 20;
        int tCount = 50;
        int gCount = 20;
        int width = 50;
        int height = 25;

        List<String> config = Files.readAllLines(new File("config").toPath(), Charset.defaultCharset());
        while (true) {
            String line = config.remove(0);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            if (line.equals("players")) {
                break;
            }
            String[] cfg = line.split(" ", 2);
            if (cfg[0].equals("w")) {
                width = Integer.parseInt(cfg[1]);
            }
            if (cfg[0].equals("h")) {
                height = Integer.parseInt(cfg[1]);
            }
            if (cfg[0].equals("t")) {
                tCount = Integer.parseInt(cfg[1]);
            }
            if (cfg[0].equals("g")) {
                gCount = Integer.parseInt(cfg[1]);
            }
            if (cfg[0].equals("z1")) {
                z1Count = Integer.parseInt(cfg[1]);
            }
            if (cfg[0].equals("z2")) {
                z2Count = Integer.parseInt(cfg[1]);
            }
        }

        Game game = new Game(width, height);

        Set<Point> points = new HashSet<>();
        Point o1, o2;
        Random r = new Random(System.currentTimeMillis());
        points.add(o1 = new Point((int) (r.nextDouble() * width / 6) + width / 6, (int) (r.nextDouble() * height / 6) + height / 6));
        points.add(o2 = new Point((int) (r.nextDouble() * width / 6) + width / 6 * 4, (int) (r.nextDouble() * height / 6) + height / 6 * 4));

        while (points.size() < gCount + tCount + z2Count + z2Count + oCount) {
            points.add(new Point((int) (r.nextDouble() * width), (int) (r.nextDouble() * height)));
        }

        game.set(o1.x, o1.y, Game.O1);
        game.set(o2.x, o2.y, Game.O2);
        points.remove(o1);
        points.remove(o2);

        ArrayList<Point> list = new ArrayList<>(points);
        Collections.shuffle(list);

        int t = 0, g = 0, z1 = 0, z2 = 0;
        for (Point p : list) {
            if (t < tCount) {
                game.set(p.x, p.y, Game.TREE);
                t++;
            } else if (g < gCount) {
                game.set(p.x, p.y, Game.G);
                g++;
            } else if (z1 < z1Count) {
                game.set(p.x, p.y, Game.Z + z1++);
            } else if (z2 < z2Count) {
                game.set(p.x, p.y, Game.Z + 100 + z2++);
            }
        }

        while (config.size() > 0) {
            String line = config.remove(0);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] cfg = line.split(" ", 3);
            int player = Integer.parseInt(cfg[0]);
            game.putKey(cfg[1], player);
            game.putName(player, cfg[2]);
        }

        new Server(game).port(9999).start();
    }

}
