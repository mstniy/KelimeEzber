package com.mstniy.kelimeezber;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

enum ExerciseType {
    MC,
    Writing,
    Matching
}

interface ExerciseFragmentInterface {
    void unmuted();
    void newRound();
}

public class ExerciseFragment extends Fragment {
    static final String TAG = ExerciseFragment.class.getName();

    private final int PICKDATASET_REQUEST_CODE = 40914;
    final double MC_PROBABILITY = 0.75;
    final double MATCH_PROBABILITY = 0.05;

    MyApplication app;
    FrameLayout frame;
    ImageView muteButton;
    SelectionMethod selectionMethod = SelectionMethod.SMART;

    public void onAttach(Context context) {
        super.onAttach(context);
        app = (MyApplication) context.getApplicationContext();
        app.exerciseFragment = this;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.exercise_fragment_frame, container, false);
        frame = rootView.findViewById(R.id.exercise_fragment_frame);
        muteButton = rootView.findViewById(R.id.mute_button);
        if (!app.isMuted)
            muteButton.setImageResource(android.R.drawable.ic_lock_silent_mode);
        else
            muteButton.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
        muteButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ExerciseFragment.this.muteButtonPressed();
            }
        });

        if (savedInstanceState != null) {
            selectionMethod = (SelectionMethod) savedInstanceState.getSerializable("selectionMethod");
            // Android automatically restores the states of sub-fragments
        }
        else
            StartRound();

        return rootView;
    }

    Fragment instantiateSubFragment(ExerciseType et) {
        if (et == ExerciseType.Writing)
            return Fragment.instantiate(getContext(), WritingFragment.class.getName());
        else if (et == ExerciseType.MC)
            return Fragment.instantiate(getContext(), MCFragment.class.getName());
        else if (et == ExerciseType.Matching)
            return Fragment.instantiate(getContext(), MatchingFragment.class.getName());
        return null;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_exercise, menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_selectlang) {
            new SelectLanguageDialog().show(getFragmentManager(), "selectlang");
            return true;
        }
        else if (item.getItemId() == R.id.action_selectionmethod) {
            new SelectionMethodDialog().show(getFragmentManager(), "selectionmethod");
            return true;
        }
        else if (item.getItemId() == R.id.action_datasetpath) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(intent, PICKDATASET_REQUEST_CODE);
            return true;
        }
        else
            return super.onOptionsItemSelected(item);
    }

    private String getRealPath(String contentPath) {
        if (contentPath.startsWith("/tree/primary:") == false)
            return null;
        return "/sdcard/" + contentPath.substring(14);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICKDATASET_REQUEST_CODE) {
            if (data == null) // User cancelled the operation
                return ;
            String path = data.getData().getPath();
            path = getRealPath(path);
            if (path == null) {
                Toast.makeText(getContext(), "Unsupported content path.",  Toast.LENGTH_SHORT).show();
                return ;
            }

            if (new File(path+"/validated.tsv").exists() == false) {
                Toast.makeText(getContext(), "Not a valid Common Voice dataset.", Toast.LENGTH_SHORT).show();
                return;
            }
            app.audioDatasetPath = path;
            app.helper.setAudioDatasetPath(path);
            app.MaybeReadAudioDataset();
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    void ChangeExercise(ExerciseType et) {
        Fragment subFragment = getChildFragmentManager().findFragmentById(R.id.exercise_fragment_frame);
        if (subFragment == null // If the new exercise is of a different type to the one currently shown on the screen, or there is no exercise currently shown on the screen
                || subFragment.isAdded() == false // findFragmentById doesn't return null after rotations, even though the old fragment has already detached.
                || et != getTypeOfExerciseFragment(subFragment)) {
            subFragment = instantiateSubFragment(et);
            getChildFragmentManager().beginTransaction().replace(R.id.exercise_fragment_frame, subFragment).commit();
            getChildFragmentManager().executePendingTransactions();
        }
        else
            ((ExerciseFragmentInterface)subFragment).newRound();
    }

    ExerciseType getTypeOfExerciseFragment(Fragment f) {
        if (f instanceof MCFragment)
            return ExerciseType.MC;
        if (f instanceof WritingFragment)
            return ExerciseType.Writing;
        if (f instanceof MatchingFragment)
            return ExerciseType.Matching;
        return null;
    }

    private void muteButtonPressed() {
        app.isMuted = !app.isMuted;
        if (!app.isMuted) {
            muteButton.setImageResource(android.R.drawable.ic_lock_silent_mode);
            ((ExerciseFragmentInterface)getChildFragmentManager().findFragmentById(R.id.exercise_fragment_frame)).unmuted();
        }
        else
            muteButton.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
    }

    void MaybeRecordEstimate() {
        long lastEstimateTimestamp = 0;
        if (app.estimates.size() > 0)
            lastEstimateTimestamp = app.estimates.get(app.estimates.size()-1).timestamp;
        if (System.currentTimeMillis()/1000-lastEstimateTimestamp > 24*3600) { // One day
            Integer knownEstimate = app.getKnownEstimate();
            if (knownEstimate != null)
                app.pushEstimate(knownEstimate);
        }
    }

    ExerciseFragmentInterface getCurrentExercise() {
        return (ExerciseFragmentInterface) getChildFragmentManager().findFragmentById(R.id.exercise_fragment_frame);
    }

    void StartRound() {
        MaybeRecordEstimate();

        double randomDouble = new Random().nextDouble();
        ExerciseType newExerciseType;
        if (randomDouble < MC_PROBABILITY)
            newExerciseType = ExerciseType.MC;
        else if (randomDouble < MC_PROBABILITY + MATCH_PROBABILITY)
            newExerciseType = ExerciseType.Matching;
        else
            newExerciseType = ExerciseType.Writing;

        ChangeExercise(newExerciseType);
    }

    void FinishRound() {
        StartRound();
    }

    public void dbChanged() {
        StartRound();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("selectionMethod", selectionMethod);
    }
}
