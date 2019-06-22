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
    static Pair ChoosePairSmart(MyApplication app)
    {
        /*if (uncoveredPairs.size() > 0) // Choose an uncovered pair
        {
            final int uncoveredPairsIndex = new Random().nextInt(uncoveredPairs.size());
            final int index = uncoveredPairs.get(uncoveredPairsIndex);
            //swap(uncoveredPairs[uncoveredPairsIndex], uncoveredPairs[uncoveredPairs.size()-1]);
            final int tmp = uncoveredPairs.get(uncoveredPairsIndex);
            uncoveredPairs.set(uncoveredPairsIndex, uncoveredPairs.get(uncoveredPairs.size()-1));
            uncoveredPairs.set(uncoveredPairs.size()-1, tmp);
            uncoveredPairs.remove(uncoveredPairs.size()-1);
            return index;
        }*/
        double hardnessSum = 0;
        for (Pair p : app.wlist)
            hardnessSum += exp(p.hardness);
        if (hardnessSum == 0)
            return null;
        double rnd = new Random().nextDouble() * hardnessSum;
        Pair lastPair = null; // Java doesn't have a HashSet.end(), so we do this
        for (Pair p : app.wlist)
        {
            if (exp(p.hardness)>rnd)
                return p;
            rnd -= exp(p.hardness);
            lastPair = p;
        }
        return lastPair; // Mathematically, this cannot happen. But we're dealing with floats, so who knows.
    }
}
