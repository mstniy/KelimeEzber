package com.mstniy.kelimeezber;

import java.util.ArrayList;
import java.util.Random;

import static java.lang.Math.exp;

public class PairChooser {
    static Pair ChoosePairRandom(MyApplication app) {
        Pair[] arr = (Pair[])app.wlist.toArray(new Pair[app.wlist.size()]);
        return arr[new Random().nextInt(app.wlist.size())];
    }
    static Pair ChoosePairNew(MyApplication app) {
        ArrayList<Pair> arr = new ArrayList<>();
        for (Pair p : app.wlist) {
            if (p.period == 1)
                arr.add(p);
        }
        if (arr.isEmpty())
            return app.wlist.iterator().next();
        return arr.get(new Random().nextInt(arr.size()));
    }
}
