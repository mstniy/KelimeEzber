package com.mstniy.kelimeezber;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

enum SelectionMethod {
    SMART, NEW, RANDOM, CONFUSION
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

    static ArrayList<PairSelectResult> ChoosePairSmart(MyApplication app, int numPairs) {
        final double RANDOM_PROB = 0.05;
        final double NEW_PROB = 0.1;
        final double CONFUSION_SELECT_PROB = 0.25;

        Comparator<Pair> pairComparator = new Comparator<Pair>() {
            @Override
            public int compare(Pair o1, Pair o2) {
                if (o1.next == -1 && o2.next == -1)
                    return 0;
                if (o1.next == -1) // Magic value for "not scheduled"
                    return 1;
                if (o2.next == -1)
                    return -1;
                return o1.next - o2.next;
            }
        };

        ArrayList<Pair> sortedByNext = new ArrayList<>(app.wlist);
        Collections.sort(sortedByNext, pairComparator);

        int index = Collections.binarySearch(sortedByNext, new Pair(0, null, null, 0, app.roundId+numPairs), pairComparator);
        if (index < 0) {
            index = -index-1;
        }
        else
            index++;

        ArrayList<PairSelectResult> pairs = new ArrayList<>();
        List<Pair> candidates = sortedByNext.subList(0, index); // A list of pairs scheduled at (or before) the range of rounds we're interested in
        Collections.shuffle(candidates);
        for (int pairs_index=0, candidates_index = 0; pairs_index < numPairs; pairs_index++) {
            if (candidates_index == candidates.size()) { // If we have run out of scheduled pairs, fill in the rest using random pairs
                pairs.add(new PairSelectResult(ChoosePairRandom(app, 1).get(0).p, SelectionMethod.SMART));
                // We replace the selection method with SMART here, so that if the user loses the round, the pair get scheduled.
                // We won't end up scheduling too many pairs, because if we have entered this branch our schedule is probably not too tight.
            }
            else {
                double rnd = new Random().nextDouble();
                if (rnd < RANDOM_PROB)
                    pairs.add(ChoosePairRandom(app, 1).get(0));
                else if (rnd < RANDOM_PROB + NEW_PROB)
                    pairs.add(ChoosePairNew(app, 1).get(0));
                else { // Otherwise, select one of the scheduled pairs
                    pairs.add(new PairSelectResult(candidates.get(candidates_index), SelectionMethod.SMART));
                    candidates_index++;
                }
            }

            if (pairs_index != numPairs-1) {
                ArrayList<Pair> confusions = app.getConfusionsForPair(pairs.get(pairs_index).p);
                if (confusions.size() != 0 && new Random().nextFloat() < CONFUSION_SELECT_PROB) {
                    pairs.add(new PairSelectResult(confusions.get(new Random().nextInt(confusions.size())), SelectionMethod.CONFUSION));
                    pairs_index++;
                }
            }
        }

        return pairs;
    }
    static ArrayList<PairSelectResult> ChoosePairRandom(MyApplication app, int numPairs) {
        ArrayList<PairSelectResult> pairs = new ArrayList<>();
        Pair[] wlist = app.wlist.toArray(new Pair[app.wlist.size()]);
        for (int i=0; i<numPairs; i++) {
            Pair p = wlist[new Random().nextInt(app.wlist.size())];
            pairs.add(new PairSelectResult(p, SelectionMethod.RANDOM));
        }
        return pairs;
    }

    static ArrayList<PairSelectResult> ChoosePairNew(MyApplication app, int numPairs) {
        ArrayList<Pair> sortedByPeriod = new ArrayList<>(app.wlist);

        Collections.sort(sortedByPeriod, new Comparator<Pair>() {
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

        List<Pair> first25 = sortedByPeriod.subList(0, Math.min(25, sortedByPeriod.size()));
        Collections.shuffle(first25);

        ArrayList<PairSelectResult> pairs = new ArrayList<>();
        for (int i=0; i<numPairs; i++) {
            pairs.add(new PairSelectResult(first25.get(i % sortedByPeriod.size()), SelectionMethod.NEW));
        }

        return pairs;
    }

    // Note that *return.method* might not equal *method*
    static ArrayList<PairSelectResult> ChoosePair(MyApplication app, SelectionMethod method, int numPairs) {
        if (method == SelectionMethod.NEW)
            return ChoosePairNew(app, numPairs);
        else if (method == SelectionMethod.RANDOM)
            return ChoosePairRandom(app, numPairs);
        else if (method == SelectionMethod.SMART)
            return ChoosePairSmart(app, numPairs);
        return null;
    }
}
