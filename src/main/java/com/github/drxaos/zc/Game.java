package com.github.drxaos.zc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

public class Game {

    public static final int EMPTY = 0; // пустая клетка
    public static final int TREE = 1; // дерево
    public static final int G = 2; // капуста
    public static final int O = 10; // нора
    public static final int OBSERVER = 100; // id наблюдателя
    public static final int Z = 101; // первый id зайца

    public static final int ANS_WAIT = -1; // ждите
    public static final int ANS_TREE_BUMP = -2; // удар об дерево
    public static final int ANS_ERROR = -3; // ошибка

    public static final int WAIT_STEP = 100; // базовый таймаут
    public static final int WAIT_PUSH = 50; // дополнительный таймаут на каждый объект
    public static final int WAIT_TREE = 500; // таймаут после попадания в дерево
    public static final int WAIT_LOOK = 100; // таймаут после просмотра карты

    protected Map<String, Integer> keys = new HashMap<>(); // коды авторизации
    protected Map<Integer, String> names = new HashMap<>(); // имена игроков

    protected final int H; // ширина карты
    protected final int W; // высота карты

    protected AtomicIntegerArray map; // карта
    protected AtomicIntegerArray cellLock; // блокировки клеток
    protected AtomicIntegerArray rowLock; // блокировки строк
    protected AtomicIntegerArray colLock; // блокировки столбцов

    protected AtomicIntegerArray zx = new AtomicIntegerArray(999); // x-координаты зайцев
    protected AtomicIntegerArray zy = new AtomicIntegerArray(999); // y-координаты зайцев
    protected AtomicLongArray zwait = new AtomicLongArray(999); // таймауты зайцев

    protected AtomicIntegerArray zscore = new AtomicIntegerArray(999); // счет текущий
    protected AtomicIntegerArray zrecord = new AtomicIntegerArray(999); // счет итоговый
    protected LinkedHashMap<Long, Integer> zsession = new LinkedHashMap<Long, Integer>(); // активные игровые сессии <время окончания, индекс зайца>

    protected AtomicInteger activeRequests = new AtomicInteger(0); // количество исполняющихся запросов
    protected AtomicBoolean observerLock = new AtomicBoolean(false); // глобальная блокировка

    protected String snapshot = ""; // записанное состояние карты
    protected String rating = ""; // записанный рейтинг

    public Game(int w, int h) {
        W = w;
        H = h;

        map = new AtomicIntegerArray(W * H);
        cellLock = new AtomicIntegerArray(W * H);
        rowLock = new AtomicIntegerArray(H);
        colLock = new AtomicIntegerArray(W);
    }

    public int getW() {
        return W;
    }

    public int getH() {
        return H;
    }

    /**
     * XY в индекс
     */
    protected int xytoi(int x, int y) {
        return ((x + W) % W) + ((y + H) % H) * W;
    }

    /**
     * нормализация X
     */
    protected int xtox(int x) {
        return x < 0 ? x + W : x >= W ? x - W : x;
    }

    /**
     * нормализация Y
     */
    protected int ytoy(int y) {
        return y < 0 ? y + H : y >= H ? y - H : y;
    }

    /**
     * ZID в индекс
     */
    protected int zidtoi(int id) {
        return id - Z;
    }


    public String move(int zid, int dx, int dy) {

        if (zid < Z) {
            return "" + ANS_ERROR;
        }

        int i, x, y, xy;
        i = zidtoi(zid);

        if (!checkTimeout(i)) {
            return "" + ANS_WAIT;
        }

        checkObserver();

        // блокируем зайца
        while (true) {
            x = zx.get(i);
            y = zy.get(i);
            xy = xytoi(x, y);

            while (!colLock.compareAndSet(x, 0, zid)) ;
            while (!rowLock.compareAndSet(y, 0, zid)) ;

            if (cellLock.compareAndSet(xy, 0, zid)) {
                if (map.get(xy) == zid) {
                    // все ок
                    break;
                }
                // заяц исчез o_O
                cellLock.set(i, 0);
                rowLock.set(y, 0);
                colLock.set(x, 0);
            }
        }

        // толкаем
        int chain = push(zid, xtox(x + dx), ytoy(y + dy), dx, dy, zid, 0);
        if (chain >= 0 && map.get(xy) == zid) {
            // если зайца не потерло зацикливанием, то потрем пустотой
            map.set(xy, EMPTY);
        }

        // считаем новый таймаут
        int wait;
        if (chain >= 0) {
            wait = WAIT_STEP + WAIT_PUSH * chain;
            map.set(xy, EMPTY);
        } else {
            wait = WAIT_TREE;
        }

        // освобождаем зайца
        cellLock.set(xy, 0);
        rowLock.set(y, 0);
        colLock.set(x, 0);

        // ставим таймаут
        zwait.set(i, System.currentTimeMillis() + wait);

        activeRequests.decrementAndGet();
        return "" + wait;
    }

    private void checkObserver() {
        while (true) { // пытаемся попасть между снэпшотами
            activeRequests.incrementAndGet();
            if (observerLock.get()) {
                activeRequests.decrementAndGet();
                while (observerLock.get()) ; // если не получилось - ждем окончания
            } else {
                break;
            }
        }
    }

    protected void zupdate(int zid, int x, int y) {
        if (zid >= Z) {
            int zi = zidtoi(zid);
            zx.set(zi, xtox(x));
            zy.set(zi, ytoy(y));
        }
    }

    protected int push(int zid, int toX, int toY, int dx, int dy, int pushObj, int chainLength) {
        int posTo = xytoi(toX, toY);

        // проверяем на зацикливание
        if (cellLock.get(posTo) == zid) {
            map.set(posTo, pushObj);
            cellLock.set(posTo, 0);
            return chainLength;
        }

        // блокируем целевую клетку
        while (!cellLock.compareAndSet(posTo, 0, zid)) ;

        int obj = map.get(posTo);
        switch (obj) {
            case EMPTY:
                map.set(posTo, pushObj);
                zupdate(pushObj, toX, toY);
                cellLock.set(posTo, 0);
                return chainLength;
            case TREE:
                cellLock.set(posTo, 0);
                return ANS_TREE_BUMP;
            case O:
                cellLock.set(posTo, 0);
                if (pushObj == G) {
                    // гол!
                    zscore.incrementAndGet(zidtoi(zid));
                    return chainLength;
                }
                // иначе перепрыгиваем
                return push(zid, xtox(toX + dx), ytoy(toY + dy), dx, dy, pushObj, chainLength);
            case G:
                // катим капусту
                int next = push(zid, xtox(toX + dx), ytoy(toY + dy), dx, dy, obj, chainLength + 1);
                if (next >= 0) {
                    map.set(posTo, pushObj);
                    zupdate(pushObj, toX, toY);
                }
                cellLock.set(posTo, 0);
                return next;
            default: // Z
                int nextz = push(zid, xtox(toX + dx), ytoy(toY + dy), dx, dy, obj, chainLength + 1);
                if (nextz >= 0) {
                    map.set(posTo, pushObj);
                    zupdate(pushObj, toX, toY);
                }
                cellLock.set(posTo, 0);
                return nextz;
        }
    }

    /**
     * Просмотр карты
     */
    public String look(int zid) {
        int i = zidtoi(zid);

        if (!checkTimeout(i)) {
            return "" + ANS_WAIT;
        }

        zwait.set(i, System.currentTimeMillis() + WAIT_LOOK);

        return snapshot;
    }

    private boolean checkTimeout(int i) {
        // проверяем таймаут
        long after = zwait.get(i);
        if (after > System.currentTimeMillis()) {
            return false;
        }
        if (!zwait.compareAndSet(i, after, Long.MAX_VALUE)) {
            // двойное присвоение
            return false;
        }
        return true;
    }

    public String getRating() {
        return rating;
    }

    /**
     * Снэпшоты всего
     */
    public void snap() {
        // блокируем всех
        observerLock.set(true);
        while (activeRequests.get() > 0) ;

        // снэпшотим
        snapshot = map.toString();
        rating = zscore.toString();

        // убираем блокировку
        observerLock.set(false);
    }

    /**
     * Установка объекта на карту
     */
    public void set(int x, int y, int obj) {
        int xy = xytoi(x, y);
        map.set(xy, obj);
        if (obj >= Z) {
            int i = zidtoi(obj);
            zx.set(i, x);
            zy.set(i, y);
        }
    }

    /**
     * Добавление ключа
     */
    public void putKey(String key, Integer id) {
        keys.put(key, id);
    }

    /**
     * Добавление имени
     */
    public void putName(Integer id, String name) {
        names.put(id, name);
    }

    /**
     * Авторизация
     */
    public int auth(String key) {
        return keys.getOrDefault(key, -1);
    }

}
