package com.mstniy.kelimeezber;

import java.util.Random;

import static java.lang.Math.exp;

public class PairChooser {
    static int ChoosePair(MyApplication app)
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
        for (double h : app.hardness)
            hardnessSum += exp(h);
        if (hardnessSum == 0)
            return 0;
        double rnd = new Random().nextDouble() * hardnessSum;
        for (int i=0; i<app.hardness.size(); i++)
        {
            if (exp(app.hardness.get(i))>rnd)
                return i;
            rnd -= exp(app.hardness.get(i));
        }
        return app.hardness.size()-1; // Mathematically, this cannot happen. But we're dealing with floats, so who knows.
    }
}
