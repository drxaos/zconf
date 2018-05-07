package com.github.drxaos.zc;

import java.awt.*;
import java.util.*;

public class RandomLevel {

    public void generate(Game game, int treeCount, int gCount, Collection<Integer> z) {
        game.manage(state -> {
            Random r = new Random(System.currentTimeMillis());

            Set<Point> points = new HashSet<>();

            Point o1, o2;
            points.add(o1 = new Point(
                    (int) (r.nextDouble() * game.getW() / 5) + game.getW() / 5,
                    (int) (r.nextDouble() * game.getH() / 5) + game.getH() / 5));
            points.add(o2 = new Point(
                    (int) (r.nextDouble() * game.getW() / 5) + game.getW() / 5 * 3,
                    (int) (r.nextDouble() * game.getH() / 5) + game.getH() / 5 * 3));

            while (points.size() < treeCount + gCount + z.size() + 2) {
                points.add(new Point((int) (r.nextDouble() * 50), (int) (r.nextDouble() * 25)));
            }

            state.set(o1.x, o1.y, Game.O);
            state.set(o2.x, o2.y, Game.O);
            points.remove(o1);
            points.remove(o2);

            ArrayList<Point> list = new ArrayList<>(points);
            Collections.shuffle(list);

            int t = 0, g = 0;
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
        });
    }

}
