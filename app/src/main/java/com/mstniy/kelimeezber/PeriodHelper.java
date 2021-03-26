package com.mstniy.kelimeezber;

enum RoundOutcome {
    PASS, FAIL, NEUTRAL
}

public class PeriodHelper {
    static void recordRoundOutcome(MyApplication app, PairSelectResult pair, RoundOutcome outcome) {
        if (outcome == RoundOutcome.PASS) {
            pair.p.period *= 2;
            if (pair.p.period > MyApplication.MaxWordPeriod)
                pair.p.period = 0;
        } else if (outcome == RoundOutcome.FAIL){
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
        if (pair.method == SelectionMethod.RANDOM && outcome != RoundOutcome.NEUTRAL)
            app.helper.pushExerciseResult(outcome == RoundOutcome.PASS);
    }
}
