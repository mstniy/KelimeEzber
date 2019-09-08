package com.mstniy.kelimeezber;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.Iterator;

public class StatsFragment extends Fragment {
    MyApplication app;
    TextView estimatedKnown;
    TextView period1View;
    TextView periodInfView;
    View rootView;
    TextView total;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_stats, container, false);
        app = (MyApplication) getContext().getApplicationContext();
        total = rootView.findViewById(R.id.stats_total);
        period1View = rootView.findViewById(R.id.stats_period_1);
        periodInfView = rootView.findViewById(R.id.stats_period_inf);
        estimatedKnown = rootView.findViewById(R.id.stats_estimated_known);
        UpdateStats();
        return rootView;
    }

    void UpdateStats() {
        int period1Count = 0;
        int periodInfCount = 0;
        Iterator it = app.wlist.iterator();
        while (it.hasNext()) {
            Pair p = (Pair) it.next();
            if (p.period == 1) {
                period1Count++;
            } else if (p.period == 0) {
                periodInfCount++;
            }
        }
        total.setText(String.valueOf(app.wlist.size()));
        period1View.setText(String.valueOf(period1Count));
        periodInfView.setText(String.valueOf(periodInfCount));
        if (app.randomPassFraction.b == 0) {
            estimatedKnown.setText("?");
        } else {
            estimatedKnown.setText(String.valueOf((app.wlist.size() * app.randomPassFraction.a) / app.randomPassFraction.b));
        }
    }
}
