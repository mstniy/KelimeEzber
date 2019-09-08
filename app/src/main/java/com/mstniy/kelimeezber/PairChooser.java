package com.mstniy.kelimeezber;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class PairChooser {
    static Pair ChoosePairRandom(MyApplication app) {
        return (app.wlist.toArray(new Pair[app.wlist.size()]))[new Random().nextInt(app.wlist.size())];
    }

    static Pair ChoosePairNew(MyApplication app) {
        ArrayList<Pair> arr = new ArrayList<>();
        Iterator it = app.wlist.iterator();
        while (it.hasNext()) {
            Pair p = (Pair) it.next();
            if (p.period == 1) {
                arr.add(p);
            }
        }
        if (arr.isEmpty()) {
            return app.wlist.iterator().next();
        }
        return arr.get(new Random().nextInt(arr.size()));
    }
}
