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
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import static java.lang.Math.max;
import static java.lang.Math.min;

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
        MaybeDrawEstimatePlot();
    }

    void MaybeDrawEstimatePlot() {
        if (app.estimates.size() < 2)
            lineChart.setVisibility(View.GONE);
        else {
            ArrayList<StampedEstimate> estimates = app.estimates;
            ArrayList<Entry> entries = new ArrayList<>();
            long plotStartTime = System.currentTimeMillis()/1000-30*24*3600; // One month
            int boundIndex = Collections.binarySearch(estimates, new StampedEstimate(plotStartTime, 0));
            if (boundIndex < 0) {
                boundIndex = -boundIndex-1;
            }
            /*if (estimates.size()-boundIndex < 2) { // If we have less than two datapoints left, do not draw the plot
                lineChart.setVisibility(View.GONE);
                return ;
            }*/
            int recentEstimateMax=estimates.get(boundIndex).estimate;
            int recentEstimateMin=recentEstimateMax;
            int estimateMax=estimates.get(0).estimate;
            int estimateMin=estimateMax;
            long timeMax=estimates.get(0).timestamp;
            long timeMin=timeMax;
            for (int i=0; i<estimates.size(); i++) { // Transform StampedEstimate into Entry
                StampedEstimate e = estimates.get(i);
                entries.add(new Entry(e.timestamp, e.estimate));
                estimateMax = max(estimateMax, e.estimate);
                estimateMin = min(estimateMin, e.estimate);
                timeMax = max(timeMax, e.timestamp);
                timeMin = min(timeMin, e.timestamp);
            }
            for (int i=boundIndex; i<estimates.size(); i++) {
                StampedEstimate e = estimates.get(i);
                recentEstimateMax = max(recentEstimateMax, e.estimate);
                recentEstimateMin = min(recentEstimateMin, e.estimate);
            }
            LineDataSet set1 = new LineDataSet(entries, "Dataset 1");
            set1.setDrawValues(false);
            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);
            lineChart.setData(new LineData(dataSets));
            //lineChart.setTouchEnabled(false);
            lineChart.getLegend().setEnabled(false);
            lineChart.getDescription().setEnabled(false);
            lineChart.getXAxis().setValueFormatter(new MyXAxisFormatter());
            lineChart.getAxisLeft().setValueFormatter(new MyYAxisFormatter());
            lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            lineChart.getAxisRight().setEnabled(false);
            lineChart.getAxisLeft().setGranularity(1);
            float xScale = (timeMax-timeMin)/(timeMax-plotStartTime);
            float yScale = (estimateMax-estimateMin)/(recentEstimateMax-recentEstimateMin)/2;
            lineChart.zoomAndCenterAnimated(xScale, yScale, (plotStartTime+timeMax)/2, (recentEstimateMin+recentEstimateMax)/2, YAxis.AxisDependency.LEFT, 250);
        }
    }
}
