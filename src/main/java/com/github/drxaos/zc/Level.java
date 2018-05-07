package com.github.drxaos.zc;

import java.util.Collection;

public interface Level {
    void generate(Game.State state, Collection<Integer> z);

    void addG(Game.State state, int count);

    void addZ(Game.State state, int zid);
}
