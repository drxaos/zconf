package com.github.drxaos.zc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;

public class ResetDatabase {
    public static void main(String[] args) throws IOException {
        Random r = new Random(System.currentTimeMillis());
        Db db = new Db();
        FileOutputStream out = new FileOutputStream("codes.txt");
        HashSet<String> codes = new HashSet<>();
        while (codes.size() < 500) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                sb.append((char) ('A' + (int) (r.nextDouble() * 26)));
            }
            codes.add(sb.toString());
        }
        int zid = Game.Z;
        db.clearKeys();
        for (String code : codes) {
            db.resetPlayer(zid);
            db.addKey(zid, code);
            out.write(code.getBytes());
            out.write("\n".getBytes());
            zid++;
        }

        db.addKey(Game.OBSERVER, "password");
        db.setName(Game.OBSERVER, "observer");

        out.close();
        db.shutdown();
    }
}
