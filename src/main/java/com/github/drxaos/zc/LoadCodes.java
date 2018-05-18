package com.github.drxaos.zc;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class LoadCodes {
    public static void main(String[] args) throws IOException {
        Db db = new Db();
        db.clearKeys();
        int zid = 101;
        Scanner s = new Scanner(new File("codes.txt"));
        while (s.hasNext()) {
            String code = s.next().trim();
            if (!code.isEmpty()) {
                db.addKey(zid, code);
                zid++;
            }
        }
        s.close();

        db.addKey(Game.OBSERVER, "observer");
        db.setName(Game.OBSERVER, "observer");
        db.shutdown();
    }
}
