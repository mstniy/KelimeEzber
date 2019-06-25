package com.mstniy.kelimeezber;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class StatsFragment extends Fragment {

    TextView total, period1View, periodInfView;
    MyApplication app;
    View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_stats, container, false);
        app = (MyApplication) getContext().getApplicationContext();
        total = rootView.findViewById(R.id.stats_total);
        period1View = rootView.findViewById(R.id.stats_period_1);
        periodInfView = rootView.findViewById(R.id.stats_period_inf);

        app.wlistObservers.add(new Runnable() {
            @Override
            public void run() {
                UpdateStats();
            }
        });

        UpdateStats();

        return rootView;
    }

    void UpdateStats() {

        int period1Count=0, periodInfCount=0;
        for (Pair p : app.wlist) {
            if (p.period == 1)
                period1Count++;
            else if (p.period == 0)
                periodInfCount++;
        }

        total.setText(String.valueOf(app.wlist.size()));
        period1View.setText(String.valueOf(period1Count));
        periodInfView.setText(String.valueOf(periodInfCount));
    }
}
