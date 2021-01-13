package com.mstniy.kelimeezber;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

enum SelectionMethod {
    SMART, NEW, RANDOM
}

class PairSelectResult implements Serializable {
    Pair p;
    boolean wasRandom;

    PairSelectResult(Pair p_, boolean wasRandom_) {
        p = p_;
        wasRandom = wasRandom_;
    }
}

public class PairChooser {
    static private final String TAG = PairChooser.class.getName();

    static PairSelectResult ChoosePairSmart(MyApplication app) {
        ArrayList<Pair> candidates = new ArrayList<>();
        int smallestNext = -1;
        for (Pair p : app.wlist) {
            if (p.next == -1)
                continue;
            if (smallestNext == -1 || p.next < smallestNext)
                smallestNext = p.next;
        }
        if (smallestNext != -1 && smallestNext <= app.roundId) {
            for (Pair p : app.wlist)
                if (p.next == smallestNext)
                    candidates.add(p);
        }
        if (candidates.size() == 0) {
            return ChoosePairRandom(app);
        }
        else {
            Pair p = candidates.get(new Random().nextInt(candidates.size()));
            return new PairSelectResult(p, false);
        }
    }
    static PairSelectResult ChoosePairRandom(MyApplication app) {
        Pair p = (app.wlist.toArray(new Pair[app.wlist.size()]))[new Random().nextInt(app.wlist.size())];
        return new PairSelectResult(p, true);
    }

    static PairSelectResult ChoosePairNew(MyApplication app) {
        ArrayList<Pair> arr = new ArrayList<>();
        Iterator it = app.wlist.iterator();
        while (it.hasNext()) {
            Pair p = (Pair) it.next();
            if (p.period == 1) {
                arr.add(p);
            }
        }
        Pair p;
        if (arr.isEmpty()) {
            p = app.wlist.iterator().next();
        }
        else
            p = arr.get(new Random().nextInt(arr.size()));

        return new PairSelectResult(p, false);
    }

    static PairSelectResult ChoosePair(MyApplication app, SelectionMethod method) {
        if (method == SelectionMethod.NEW)
            return ChoosePairNew(app);
        else if (method == SelectionMethod.RANDOM)
            return ChoosePairRandom(app);
        else if (method == SelectionMethod.SMART)
            return ChoosePairSmart(app);
        return null;
    }
}
