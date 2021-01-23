package com.mstniy.kelimeezber;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;

enum SelectionMethod {
    SMART, NEW, RANDOM
}

class PairSelectResult implements Serializable {
    Pair p;
    SelectionMethod method;

    PairSelectResult(Pair p_, SelectionMethod method_) {
        p = p_;
        method = method_;
    }
}

public class PairChooser {
    static private final String TAG = PairChooser.class.getName();

    static PairSelectResult ChoosePairSmart(MyApplication app) {
        final double RANDOM_PROB = 0.05;
        final double NEW_PROB = 0.1;

        double rnd = new Random().nextDouble();
        if (rnd < RANDOM_PROB)
            return ChoosePairRandom(app);
        if (rnd < RANDOM_PROB + NEW_PROB)
            return ChoosePairNew(app);
        // Otherwise, select one of the scheduled pairs

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
            return new PairSelectResult(ChoosePairRandom(app).p, SelectionMethod.SMART); // We replace the selection method with SMART here, so that if the user loses the round, the pair get scheduled.
                                                                                        // We won't end up scheduling too many pairs, because if we have entered this branch our schedule is probably not too tight.
        }
        else {
            Pair p = candidates.get(new Random().nextInt(candidates.size()));
            return new PairSelectResult(p, SelectionMethod.SMART);
        }
    }
    static PairSelectResult ChoosePairRandom(MyApplication app) {
        Pair p = (app.wlist.toArray(new Pair[app.wlist.size()]))[new Random().nextInt(app.wlist.size())];
        return new PairSelectResult(p, SelectionMethod.RANDOM);
    }

    static PairSelectResult ChoosePairNew(MyApplication app) {
        ArrayList<Pair> arr = new ArrayList<>(app.wlist);

        Collections.sort(arr, new Comparator<Pair>() {
            int getComparisonPeriod(int period) {
                if (period == 0)
                    return Integer.MAX_VALUE;
                if (period <= MyApplication.WordDropPeriod)
                    return 1;
                else
                    return period;
            }
            @Override
            public int compare(Pair o1, Pair o2) {
                int p1 = getComparisonPeriod(o1.period);
                int p2 = getComparisonPeriod(o2.period);
                if (p1 != p2)
                    return p1-p2;

                if (o1.id != o2.id)
                    return (o1.id < o2.id)?1:-1;

                return 0;
            }
        });

        return new PairSelectResult(arr.get(new Random().nextInt(Math.min(25, arr.size()))), SelectionMethod.NEW);
    }

    // Note that *return.method* might not equal *method*
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
