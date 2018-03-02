package com.example;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

public class Game {

    public static final int H = 5;
    public static final int W = 5;

    public static final int EMPTY = 0;
    public static final int TREE = 1;
    public static final int TREE_BUMP = -1;
    public static final int Q = 2;
    public static final int O1 = 10;
    public static final int O2 = 20;
    public static final int Z = 100;
    public static final int OBSERVER = 300;

    public static final int WAIT_ERROR = -100;
    public static final int WAIT_STEP = 100;
    public static final int WAIT_PUSH = 50;
    public static final int WAIT_TREE = 500;
    public static final int WAIT_LOOK = 100;

    AtomicIntegerArray map = new AtomicIntegerArray(W * H);
    AtomicIntegerArray lock = new AtomicIntegerArray(W * H);

    AtomicInteger score1 = new AtomicInteger(0);
    AtomicInteger score2 = new AtomicInteger(0);

    AtomicIntegerArray zx = new AtomicIntegerArray(200);
    AtomicIntegerArray zy = new AtomicIntegerArray(200);
    AtomicIntegerArray zscore = new AtomicIntegerArray(200);
    AtomicLongArray zwait = new AtomicLongArray(201);
    AtomicInteger zactive = new AtomicInteger(0);

    AtomicBoolean observerLock = new AtomicBoolean(false);
    String snapshot = "";

    public int xytoi(int x, int y) {
        return (x % W) + (y % H) * W;
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

        map.set(xy, EMPTY);
        int chain = push(id, x + dx, y + dy, dx, dy, id, 0);

        int wait;
        if (chain >= 0) {
            map.set(xytoi(x + dx, y + dy), id);
            zx.set(i, x + dx);
            zy.set(i, y + dy);
            wait = WAIT_STEP + WAIT_PUSH * chain;
        } else {
            map.set(xy, id);
            wait = WAIT_TREE;
        }

        lock.set(xy, 0);

        zwait.set(i, System.currentTimeMillis() + wait);
        zactive.decrementAndGet();
        return "" + wait;
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
                if (pushObj == Q) {
                    lock.set(xy, 0);
                    if (team(id) == 1) {
                        zscore.incrementAndGet(idtoi(id));
                    }
                    score1.incrementAndGet();
                    return chain;
                }
                lock.set(xy, 0);
                return push(id, x + dx, y + dy, dx, dy, pushObj, chain);
            case O2:
                if (pushObj == Q) {
                    lock.set(xy, 0);
                    if (team(id) == 2) {
                        zscore.incrementAndGet(idtoi(id));
                    }
                    score2.incrementAndGet();
                    return chain;
                }
                lock.set(xy, 0);
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
                    int nextz = push(id, x + dx, y + dy, dx, dy, obj, chain + 1);
                    if (nextz >= 0) {
                        int zi = idtoi(obj);
                        zx.set(zi, x + dx);
                        zy.set(zi, y + dy);
                        map.set(xy, pushObj);
                    }
                    lock.set(xy, 0);
                    return nextz;
                }
                System.out.println("ERROR");
                lock.set(xy, 0);
                return 0;
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
