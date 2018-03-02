package com.example;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class Game {

    public static final int H = 100;
    public static final int W = 100;

    public static final int EMPTY = 0;
    public static final int TREE = 1;
    public static final int TREE_BUMP = -1;
    public static final int Q = 2;
    public static final int O1 = 10;
    public static final int O2 = 20;
    public static final int Z = 100;

    AtomicIntegerArray map = new AtomicIntegerArray(W * H);
    AtomicIntegerArray lock = new AtomicIntegerArray(W * H);

    AtomicInteger score1 = new AtomicInteger(0);
    AtomicInteger score2 = new AtomicInteger(0);

    AtomicIntegerArray zx = new AtomicIntegerArray(200);
    AtomicIntegerArray zy = new AtomicIntegerArray(200);
    AtomicIntegerArray zscore = new AtomicIntegerArray(200);

    public int team(int id) { // 0/1
        return (id - Z) / 100;
    }

    public int xytoi(int x, int y) {
        return (x % W) + (y % H) * W;
    }

    public int move(int id, int dx, int dy) {

        int i, x, y, xy;
        while (true) {
            // catch
            i = id = Z;
            x = zx.get(i);
            y = zy.get(i);
            xy = xytoi(x, y);
            if (lock.compareAndSet(xy, 0, id)) {
                if (map.get(xy) == id) {
                    // cought
                    break;
                }
                // miss
                lock.set(i, 0);
            }
        }

        int chain = push(id, x + dx, y + dy, dx, dy, id, 0);
        lock.set(xy, 0);
        if (chain >= 0) {
            return 100 + 50 * chain;
        }
        return 500;
    }

    public int push(int id, int x, int y, int dx, int dy, int pushObj, int chain) {
        int xy = xytoi(x, y);
        if (lock.get(xy) == id) {
            // cycle
            map.set(xy, pushObj);
            lock.set(xy, 0);
            return chain;
        }
        while (!lock.compareAndSet(xy, 0, id)) ; // lock
        int obj = map.get(xy);
        switch (obj) {
            case EMPTY:
                map.set(xy, pushObj);
                lock.set(xy, 0);
                return chain;
            case TREE:
                lock.set(xy, 0);
                return TREE_BUMP;
            case O1:
                return push(id, x + dx, y + dy, dx, dy, pushObj, chain);
            case O2:
                return push(id, x + dx, y + dy, dx, dy, pushObj, chain);
            case Q:
                int next = push(id, x + dx, y + dy, dx, dy, Q, chain + 1);
                if (next >= 0) {
                    map.set(xy, pushObj);
                }
                lock.set(xy, 0);
                return next;
            default:
                if (obj >= Z) {
                    int nextz = push(id, x + dx, y + dy, dx, dy, Q, chain + 1);
                    if (nextz >= 0) {
                        map.set(xy, pushObj);
                    }
                    lock.set(xy, 0);
                    return nextz;
                }
                System.out.println("ERROR");
                return 0;
        }
    }
}
