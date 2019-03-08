package com.mstniy.kelimeezber;

import android.arch.lifecycle.Observer;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

public class ExerciseFragment extends Fragment {

    static final String TAG = ExerciseFragment.class.getName();

    final double FORWARD_PROBABILITY = 0.33;
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
        rootView = inflater.inflate(R.layout.exercise_root, container, false);
        frame = rootView.findViewById(R.id.frame);
        multipleChoiceFragment = new MCFragment();
        writingFragment = new WritingFragment();
        isMC = true;
        getChildFragmentManager().beginTransaction().replace(R.id.frame, multipleChoiceFragment).commit();
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
        getChildFragmentManager().beginTransaction().replace(R.id.frame, mc ? multipleChoiceFragment : writingFragment).commit();
        getChildFragmentManager().executePendingTransactions();
    }

    void cpiChanged(Pair p) {
        if (p == null)
            return ;
        boolean currentFwd = (new Random().nextDouble() <= FORWARD_PROBABILITY);
        if (currentFwd) {
            setMC(true);
            multipleChoiceFragment.newRoundMC(p, currentFwd);
        }
        else {
            setMC(false);
            writingFragment.newRoundW(p, currentFwd);
        }
    }
}
