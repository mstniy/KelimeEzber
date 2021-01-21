package com.mstniy.kelimeezber;

public class PeriodHelper {
    static void recordRoundOutcome(MyApplication app, PairSelectResult pair, boolean isPass) {
        if (isPass) {
            pair.p.period *= 2;
            if (pair.p.period > MyApplication.MaxWordPeriod)
                pair.p.period = 0;
        } else {
            if (pair.p.period == 0 || pair.p.period > MyApplication.WordDropPeriod)
                pair.p.period = MyApplication.WordDropPeriod;
            else if (pair.p.period > 1)
                pair.p.period /= 2;
        }
        if (pair.method == SelectionMethod.SMART) {
            if (pair.p.period != 0)
                pair.p.next = app.roundId + pair.p.period;
            else
                pair.p.next = -1;
            app.roundId++;
            app.helper.setRoundID(app.roundId);
        }
        app.UpdatePair(pair.p);
        if (pair.method == SelectionMethod.RANDOM)
            app.helper.pushExerciseResult(isPass);
    }
}
