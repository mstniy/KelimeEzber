package com.mstniy.kelimeezber;

public class PeriodHelper {
    static void recordRoundOutcome(MyApplication app, Pair pair, boolean isPass, boolean wasSmart, boolean wasRandom) {
        if (isPass) {
            pair.period *= 2;
            if (pair.period > MyApplication.MaxWordPeriod)
                pair.period = 0;
        } else {
            if (pair.period == 0 || pair.period > MyApplication.WordDropPeriod)
                pair.period = MyApplication.WordDropPeriod;
            else if (pair.period > 1)
                pair.period /= 2;
        }
        if (wasSmart) {
            if (pair.period != 0)
                pair.next = app.roundId + pair.period;
            else
                pair.next = -1;
            app.roundId++;
            app.helper.setRoundID(app.roundId);
        }
        app.UpdatePair(pair);
        if (wasRandom)
            app.helper.pushExerciseResult(isPass);
    }
}
