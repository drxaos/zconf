package com.github.drxaos.zc;

import java.awt.*;
import java.util.*;

public class RandomLevel implements Level {
    int treeCount;
    int gCount;

    Random r = new Random(System.currentTimeMillis());

    public RandomLevel(int treeCount, int gCount) {
        this.treeCount = treeCount;
        this.gCount = gCount;
    }

    public void generate(Game.State state, Collection<Integer> z) {
        Game game = state.getGame();

        Set<Point> points = new HashSet<>();

        Point o1, o2;
        points.add(o1 = new Point(
                (int) (r.nextDouble() * game.getW() / 5) + game.getW() / 5,
                (int) (r.nextDouble() * game.getH() / 5) + game.getH() / 5));
        points.add(o2 = new Point(
                (int) (r.nextDouble() * game.getW() / 5) + game.getW() / 5 * 3,
                (int) (r.nextDouble() * game.getH() / 5) + game.getH() / 5 * 3));


        Point[] trees = new Point[8];
        points.add(trees[0] = new Point(o1.x + 6, o1.y));
        points.add(trees[1] = new Point(o1.x - 4, o1.y));
        points.add(trees[2] = new Point(o1.x, o1.y + 3));
        points.add(trees[3] = new Point(o1.x, o1.y - 2));

        points.add(trees[4] = new Point(o2.x + 3, o2.y));
        points.add(trees[5] = new Point(o2.x - 7, o2.y));
        points.add(trees[6] = new Point(o2.x, o2.y + 3));
        points.add(trees[7] = new Point(o2.x, o2.y - 5));

        while (points.size() < treeCount + gCount + z.size() + 2) {
            points.add(new Point((int) (r.nextDouble() * (game.getW() - 4)) + 2, (int) (r.nextDouble() * (game.getH() - 4)) + 2));
        }

        state.set(o1.x, o1.y, Game.O);
        state.set(o2.x, o2.y, Game.O);
        points.remove(o1);
        points.remove(o2);

        ArrayList<Point> list = new ArrayList<>(points);
        Collections.shuffle(list);

        int t = 0, g = 0;
        for (int i = 0; i < trees.length; i++) {
            Point p = trees[i];
            list.remove(p);
            state.set(p.x, p.y, Game.TREE);
            t++;
        }

        for (Point p : list) {
            if (t < treeCount) {
                state.set(p.x, p.y, Game.TREE);
                t++;
            } else if (g < gCount) {
                state.set(p.x, p.y, Game.G);
                g++;
            } else {
                if (z.size() > 0) {
                    Iterator<Integer> i = z.iterator();
                    state.set(p.x, p.y, i.next());
                    i.remove();
                }
            }
        }
    }

    public void addG(Game.State state, int count) {
        Game game = state.getGame();

        Set<Point> points = new HashSet<>();
        while (points.size() < count) {
            Point point = new Point((int) (r.nextDouble() * (game.getW() - 4)) + 2, (int) (r.nextDouble() * (game.getH() - 4)) + 2);
            if (state.get(point.x, point.y) == Game.EMPTY) {
                points.add(point);
            }
        }

        for (Point p : points) {
            state.set(p.x, p.y, Game.G);
        }
    }

    public void addZ(Game.State state, int zid) {
        Game game = state.getGame();

        while (true) {
            Point point = new Point((int) (r.nextDouble() * (game.getW() - 4)) + 2, (int) (r.nextDouble() * (game.getH() - 4)) + 2);
            if (state.get(point.x, point.y) == Game.EMPTY) {
                state.set(point.x, point.y, zid);
                break;
            }
        }
    }
}
