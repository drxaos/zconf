package com.example;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

public class Game {

    public static final int EMPTY = 0;
    public static final int TREE = 1;
    public static final int TREE_BUMP = -1;
    public static final int G = 2;
    public static final int O1 = 10;
    public static final int O2 = 20;
    public static final int Z = 100;
    public static final int OBSERVER = 300;

    public static final int WAIT_ERROR = -1;
    public static final int WAIT_STEP = 100;
    public static final int WAIT_PUSH = 50;
    public static final int WAIT_TREE = 500;
    public static final int WAIT_LOOK = 100;


    final int H;
    final int W;

    AtomicIntegerArray map;
    AtomicIntegerArray lock;

    AtomicInteger score1 = new AtomicInteger(0);
    AtomicInteger score2 = new AtomicInteger(0);

    AtomicIntegerArray zx = new AtomicIntegerArray(200);
    AtomicIntegerArray zy = new AtomicIntegerArray(200);
    AtomicIntegerArray zscore = new AtomicIntegerArray(200);
    AtomicLongArray zwait = new AtomicLongArray(201);
    AtomicInteger zactive = new AtomicInteger(0);

    AtomicBoolean observerLock = new AtomicBoolean(false);
    String snapshot = "";

    public Game(int h, int w) {
        H = h;
        W = w;

        map = new AtomicIntegerArray(W * H);
        lock = new AtomicIntegerArray(W * H);
    }

    public int getW() {
        return W;
    }

    public int getH() {
        return H;
    }

    public int getScore1() {
        return score1.get();
    }

    public int getScore2() {
        return score2.get();
    }

    public int xytoi(int x, int y) {
        return ((x + W) % W) + ((y + H) % H) * W;
    }

    public int xtox(int x) {
        return x < 0 ? x + W : x > W ? x - W : x;
    }

    public int ytoy(int y) {
        return y < 0 ? y + H : y > H ? y - H : y;
    }

    public int idtoi(int id) {
        return id - Z;
    }

    public int team(int id) { // 1/2
        return id / 100;
    }

    public String move(int id, int dx, int dy) {

        int i, x, y, xy;
        i = idtoi(id);

        // check timeout
        long after = zwait.get(i);
        if (after > System.currentTimeMillis()) {
            return "" + WAIT_ERROR;
        }
        if (!zwait.compareAndSet(i, after, Long.MAX_VALUE)) {
            return "" + WAIT_ERROR;
        }

        while (true) { // wait observer
            zactive.incrementAndGet();
            if (observerLock.get()) {
                zactive.decrementAndGet();
                while (observerLock.get()) ;
            } else {
                break;
            }
        }

        while (true) {
            // catch
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

        int wait;
        if (chain >= 0) {
            wait = WAIT_STEP + WAIT_PUSH * chain;
            map.set(xy, EMPTY);
        } else {
            wait = WAIT_TREE;
        }

        lock.set(xy, 0);
        zwait.set(i, System.currentTimeMillis() + wait);
        zactive.decrementAndGet();
        return "" + wait;
    }

    void zupdate(int id, int x, int y) {
        if (id >= Z) {
            int zi = idtoi(id);
            zx.set(zi, xtox(x));
            zy.set(zi, ytoy(y));
        }
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
                zupdate(pushObj, x, y);
                lock.set(xy, 0); // unlock
                return chain;
            case TREE:
                lock.set(xy, 0); // unlock
                return TREE_BUMP;
            case O1:
                lock.set(xy, 0); // unlock
                if (pushObj == G) {
                    if (team(id) == 1) {
                        zscore.incrementAndGet(idtoi(id));
                    }
                    score1.incrementAndGet();
                    return chain;
                }

                // if Z
                return push(id, x + dx, y + dy, dx, dy, pushObj, chain);
            case O2:
                lock.set(xy, 0);
                if (pushObj == G) {
                    if (team(id) == 2) {
                        zscore.incrementAndGet(idtoi(id));
                    }
                    score2.incrementAndGet();
                    return chain;
                }

                // if Z
                return push(id, x + dx, y + dy, dx, dy, pushObj, chain);
            case G:
                int next = push(id, x + dx, y + dy, dx, dy, G, chain + 1);
                if (next >= 0) {
                    map.set(xy, pushObj);
                    zupdate(pushObj, x, y);
                }
                lock.set(xy, 0);
                return next;
            default:
                int nextz = push(id, x + dx, y + dy, dx, dy, obj, chain + 1);
                if (nextz >= 0) {
                    map.set(xy, pushObj);
                    zupdate(pushObj, x, y);
                }
                lock.set(xy, 0);
                return nextz;
        }
    }

    public String look(int id) {
        int i = idtoi(id);

        // check timeout
        long after = zwait.get(i);
        if (after > System.currentTimeMillis()) {
            return "" + WAIT_ERROR;
        }
        if (!zwait.compareAndSet(i, after, Long.MAX_VALUE)) {
            return "" + WAIT_ERROR;
        }

        zwait.set(i, System.currentTimeMillis() + WAIT_LOOK);

        return snapshot;
    }

    public void snap() {
        observerLock.set(true);
        while (zactive.get() > 0) ;
        snapshot = map.toString();
        observerLock.set(false);
    }

    public void set(int x, int y, int obj) {
        int xy = xytoi(x, y);
        map.set(xy, obj);
        if (obj >= Z) {
            int i = idtoi(obj);
            zx.set(i, x);
            zy.set(i, y);
        }
    }
}
