package com.mstniy.kelimeezber;

import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
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
