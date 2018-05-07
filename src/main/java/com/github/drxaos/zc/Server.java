package com.github.drxaos.zc;

import org.rapidoid.http.Req;
import org.rapidoid.setup.Setup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server implements Runnable {

    private long uid = 1;
    volatile boolean stop;
    Game game;
    Db db;

    String address = "0.0.0.0";
    int port = 9999;

    public Server(Game game, Db db) {
        this.game = game;
        this.db = db;
    }

    public Server address(String address) {
        this.address = address;
        return this;
    }

    public Server port(int port) {
        this.port = port;
        return this;
    }

    public Server start() {
        new Thread(this).start();
        return this;
    }

    public Server stop() {
        this.stop = true;
        return this;
    }


    public void run() {
        Setup setup = Setup.create("srv" + uid++).address(address).port(port);
        setup.config().sub("c3p0").set("initialPoolSize", 100);
        setup.config().sub("c3p0").set("minPoolSize", 100);
        setup.config().sub("c3p0").set("maxPoolSize", 1000);
        setup.config().sub("net").set("bufSizeKB", 1);
        setup.config().sub("http").set("timeout", 100);
        setup.config().sub("http").set("timeoutResolution", 100);

        setup.get("/").html((Req x) -> x.response().redirect("/index.html"));

        setup.get("/size/{key}").plain((String key) -> {
            // [W, H]
            try {
                int id = auth(key);
                if (id < 0) {
                    return "unauthorized";
                }
                return "[" + game.getW() + ", " + game.getH() + "]";
            } catch (Exception e) {
                e.printStackTrace();
                return "" + Game.ANS_ERROR;
            }
        });

        setup.get("/score/{key}").plain((String key) -> {
            // [Current score, Max score]
            try {
                int zid = auth(key);
                if (zid < 0) {
                    return "unauthorized";
                }
                return "[" + db.getCurrentScore(zid) + ", " + db.getMaxScore(zid) + "]";
            } catch (Exception e) {
                e.printStackTrace();
                return "" + Game.ANS_ERROR;
            }
        });

        setup.get("/rating/{key}").plain((String key) -> {
            // [ZID 1, Name 1, Max score 1, ZID 2, Name 2, Max score 2, ...]
            try {
                int id = auth(key);
                if (id < 0) {
                    return "unauthorized";
                }
                return db.getRating();
            } catch (Exception e) {
                e.printStackTrace();
                return "" + Game.ANS_ERROR;
            }
        });

        Map<String, Long> ipwait = new ConcurrentHashMap<>();
        setup.get("/auth/{key}").plain((Req req) -> {
            // ID
            try {
                String key = req.param("key");
                long prev = ipwait.getOrDefault(req.clientIpAddress(), 0L);
                if (System.currentTimeMillis() - prev < 1000) {
                    return "" + Game.ANS_WAIT;
                }
                ipwait.put(req.clientIpAddress(), System.currentTimeMillis());
                int id = auth(key);
                if (id < 0) {
                    return "unauthorized";
                }
                return "" + id;
            } catch (Exception e) {
                e.printStackTrace();
                return "-1";
            }
        });

        setup.get("/look/{key}").plain((String key) -> {
            // [map] OR -3
//            long start = System.nanoTime();
            try {
                int id = auth(key);
                if (id < 0) {
                    return "unauthorized";
                }
                return game.look(id);
            } catch (Exception e) {
                e.printStackTrace();
                return "" + Game.ANS_ERROR;
            } finally {
//                System.out.println(System.nanoTime() - start);
            }
        });

        setup.get("/up/{key}").plain((String key) -> {
            // MS OR -1
            try {
                int id = auth(key);
                if (id < 0) {
                    return "unauthorized";
                }
//            long start = System.nanoTime();
                String res = game.move(id, 0, -1);
//            System.out.println(System.nanoTime() - start);
                return res;
            } catch (Exception e) {
                e.printStackTrace();
                return "" + Game.ANS_ERROR;
            }
        });

        setup.get("/down/{key}").plain((String key) -> {
            // MS OR -1
            try {
                int id = auth(key);
                if (id < 0) {
                    return "unauthorized";
                }
//            long start = System.nanoTime();
                String res = game.move(id, 0, 1);
//            System.out.println(System.nanoTime() - start);
                return res;
            } catch (Exception e) {
                e.printStackTrace();
                return "" + Game.ANS_ERROR;
            }
        });

        setup.get("/left/{key}").plain((String key) -> {
            // MS OR -1
            try {
                int id = auth(key);
                if (id < 0) {
                    return "unauthorized";
                }
//            long start = System.nanoTime();
                String res = game.move(id, -1, 0);
//            System.out.println(System.nanoTime() - start);
                return res;
            } catch (Exception e) {
                e.printStackTrace();
                return "" + Game.ANS_ERROR;
            }
        });

        setup.get("/right/{key}").plain((String key) -> {
            // MS OR -1
            try {
                int id = auth(key);
                if (id < 0) {
                    return "unauthorized";
                }
//            long start = System.nanoTime();
                String res = game.move(id, 1, 0);
//            System.out.println(System.nanoTime() - start);
                return res;
            } catch (Exception e) {
                e.printStackTrace();
                return "" + Game.ANS_ERROR;
            }
        });

        setup.activate();

        while (!stop) {
            game.manage(state -> {

            });
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        setup.shutdown();
    }

    protected int auth(String key) {
        return db.getZid(key);
    }

}
