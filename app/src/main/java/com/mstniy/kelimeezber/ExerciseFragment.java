package com.mstniy.kelimeezber;

import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Random;

public class ExerciseFragment extends Fragment {

    static final String TAG = ExerciseFragment.class.getName();

    MyApplication app;
    View rootView;
    MCFragment multipleChoiceFragment;
    WritingFragment writingFragment;
    FrameLayout frame;
    boolean isMC;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        app = (MyApplication) getContext().getApplicationContext();
        rootView = inflater.inflate(R.layout.exercise_fragment_frame, container, false);
        frame = rootView.findViewById(R.id.exercise_fragment_frame);
        multipleChoiceFragment = new MCFragment();
        writingFragment = new WritingFragment();
        isMC = true;
        getChildFragmentManager().beginTransaction().replace(R.id.exercise_fragment_frame, multipleChoiceFragment).commit();
        getChildFragmentManager().executePendingTransactions();

        app.currentPair.observe(this, new Observer<Pair>() {
            @Override
            public void onChanged(@Nullable Pair pair) {
                cpiChanged(pair);
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_exercise, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_exercisetype) {
            new ExerciseTypeDialog().show(getFragmentManager(), "exercisetype");
            return true;
        }
        else
            return super.onOptionsItemSelected(item);
    }

    void setMC(boolean mc) {
        if (mc == isMC)
            return ;
        isMC = mc;
        getChildFragmentManager().beginTransaction().replace(R.id.exercise_fragment_frame, mc ? multipleChoiceFragment : writingFragment).commit();
        getChildFragmentManager().executePendingTransactions();
    }

    void cpiChanged(Pair p) {
        if (p == null)
            return ;
        setMC(app.currentFwd);
        if (app.currentFwd) {
            multipleChoiceFragment.newRound(p);
        }
        else {
            writingFragment.newRound(p);
        }
    }
}
