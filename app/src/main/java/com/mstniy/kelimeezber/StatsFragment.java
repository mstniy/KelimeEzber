package com.mstniy.kelimeezber;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

class MyXAxisFormatter extends ValueFormatter {
    private final String TAG = getClass().getName();
    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        String res = new SimpleDateFormat("d MMM").format(new Date((long)value*1000));
        return res;
    }
}

class MyYAxisFormatter extends ValueFormatter {
    private final String TAG = getClass().getName();
    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        return String.valueOf((int)value);
    }
}

public class StatsFragment extends Fragment {
    MyApplication app;
    TextView estimatedKnown;
    TextView period1View;
    TextView periodHighView;
    View rootView;
    TextView total;
    LineChart lineChart;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_stats, container, false);
        app = (MyApplication) getContext().getApplicationContext();
        total = rootView.findViewById(R.id.stats_total);
        period1View = rootView.findViewById(R.id.stats_period_1);
        periodHighView = rootView.findViewById(R.id.stats_period_high);
        estimatedKnown = rootView.findViewById(R.id.stats_estimated_known);
        lineChart = rootView.findViewById(R.id.statsLineChart);
        UpdateStats();
        return rootView;
    }

    void UpdateStats() {
        int period1Count = 0;
        int periodHighCount = 0;
        Iterator it = app.wlist.iterator();
        while (it.hasNext()) {
            Pair p = (Pair) it.next();
            if (p.period == 1) {
                period1Count++;
            } else if (p.period == 0 || p.period >= 256) {
                periodHighCount++;
            }
        }
        total.setText(String.valueOf(app.wlist.size()));
        period1View.setText(String.valueOf(period1Count));
        periodHighView.setText(String.valueOf(periodHighCount));
        Integer knownEstimate = app.getKnownEstimate();
        if (knownEstimate == null) {
            estimatedKnown.setText("?");
        } else {
            estimatedKnown.setText(String.valueOf(knownEstimate));
        }
        if (app.estimates.size() < 2)
            lineChart.setVisibility(View.GONE);
        else {
            ArrayList<StampedEstimate> estimates = app.estimates;
            ArrayList<Entry> entries = new ArrayList<>();
            for (StampedEstimate e : estimates) // Transform StampedEstimate into Entry
                entries.add(new Entry(e.timestamp, e.estimate));
            LineDataSet set1 = new LineDataSet(entries, "Dataset 1");
            set1.setDrawValues(false);
            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);
            lineChart.setData(new LineData(dataSets));
            lineChart.setTouchEnabled(false);
            lineChart.getLegend().setEnabled(false);
            lineChart.getDescription().setEnabled(false);
            lineChart.getXAxis().setValueFormatter(new MyXAxisFormatter());
            lineChart.getAxisLeft().setValueFormatter(new MyYAxisFormatter());
            lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            lineChart.getAxisRight().setEnabled(false);
            lineChart.getAxisLeft().setGranularity(1);
            lineChart.getAxisLeft().setAxisMinimum(0);
        }
    }
}
