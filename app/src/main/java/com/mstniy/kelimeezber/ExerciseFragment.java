package com.mstniy.kelimeezber;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

enum ExerciseType {
    MC,
    Writing
}

interface ExerciseFragmentInterface {
    void unmuted();
}

public class ExerciseFragment extends Fragment {
    static final String TAG = ExerciseFragment.class.getName();

    private final int PICKDATASET_REQUEST_CODE = 40914;
    final double MC_PROBABILITY = 0.5;

    MyApplication app;
    FrameLayout frame;
    Button muteButton;
    Pair currentPair = null;
    boolean isCurrentPairRandom;
    SelectionMethod selectionMethod = SelectionMethod.SMART;
    boolean childIgnoreState;

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
            muteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_silent_mode, 0, 0, 0);
        else
            muteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_silent_mode_off, 0, 0, 0);
        muteButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ExerciseFragment.this.muteButtonPressed();
            }
        });

        if (savedInstanceState != null) {
            isCurrentPairRandom = savedInstanceState.getBoolean("isCurrentPairRandom");

            selectionMethod = (SelectionMethod) savedInstanceState.getSerializable("selectionMethod");

            currentPair = (Pair) savedInstanceState.getSerializable("currentPair");

            if (app.wlist.contains(currentPair) == false) { // The user removed the current pair (from the word list) and switched back to the ExerciseFragment
                StartRound(false);
                childIgnoreState = true; //TODO: There's probably a way to instruct Android to drop the saved states of the views under this fragment, but this works, too.
            }
            else
                childIgnoreState = false;
        }
        else
            StartRound(true);

        return rootView;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_exercise, menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_selectionmethod) {
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

    void ChangeExercise(Pair p, ExerciseType et) {
        ChangeExercise(p, et, null);
    }

    void ChangeExercise(Pair p, ExerciseType et, SavedState savedSubFragmentState) {
        Fragment currentFragment = getChildFragmentManager().findFragmentById(R.id.exercise_fragment_frame);
        if (currentFragment == null // If the new exercise is of a different type to the one currently shown on the screen, or there is no exercise currently shown on the screen
                || currentFragment.isAdded() == false // findFragmentById doesn't return null after rotations, even though the old fragment has already detached.
                || (et == ExerciseType.MC && ! (currentFragment instanceof  MCFragment))
                || (et == ExerciseType.Writing && ! (currentFragment instanceof  WritingFragment))) {
            Fragment newFragment = null;
            if (et == ExerciseType.Writing)
                newFragment = Fragment.instantiate(getContext(), WritingFragment.class.getName());
            else if (et == ExerciseType.MC)
                newFragment = Fragment.instantiate(getContext(), MCFragment.class.getName());
            newFragment.setInitialSavedState(savedSubFragmentState);
            getChildFragmentManager().beginTransaction().replace(R.id.exercise_fragment_frame, newFragment).commit();
            getChildFragmentManager().executePendingTransactions();
            return;
        }

        if (et == ExerciseType.MC)
            ((MCFragment)currentFragment).newRound(p);
        else if (et == ExerciseType.Writing)
            ((WritingFragment)currentFragment).newRound(p);
    }

    private void muteButtonPressed() {
        app.isMuted = !app.isMuted;
        if (!app.isMuted) {
            muteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_silent_mode, 0, 0, 0);
            ((ExerciseFragmentInterface)getChildFragmentManager().findFragmentById(R.id.exercise_fragment_frame)).unmuted();
        }
        else
            muteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_silent_mode_off, 0, 0, 0);
    }

    void StartRound(boolean show) {
        double randomDouble = new Random().nextDouble();
        ExerciseType newExerciseType;
        if (randomDouble < MC_PROBABILITY)
            newExerciseType = ExerciseType.MC;
        else
            newExerciseType = ExerciseType.Writing;
        if (selectionMethod == SelectionMethod.SMART) {
            ArrayList<Pair> candidates = new ArrayList<>();
            int smallestNext = -1;
            for (Pair p : app.wlist) {
                if (p.next == -1)
                    continue;
                if (smallestNext == -1 || p.next < smallestNext)
                    smallestNext = p.next;
            }
            Log.d(TAG, "smallestNext: " + smallestNext);
            if (smallestNext != -1 && smallestNext <= app.roundId) {
                for (Pair p : app.wlist)
                    if (p.next == smallestNext)
                        candidates.add(p);
            }
            Log.d(TAG, "candidatesSize: " + candidates.size());
            if (candidates.size() == 0) {
                isCurrentPairRandom = true;
                currentPair = PairChooser.ChoosePairRandom(app);
            }
            else {
                isCurrentPairRandom = false;
                currentPair = candidates.get(new Random().nextInt(candidates.size()));
            }
        }
        else if (selectionMethod == SelectionMethod.NEW) {
            currentPair = PairChooser.ChoosePairNew(app);
            isCurrentPairRandom = false;
        }
        else if (selectionMethod == SelectionMethod.RANDOM) {
            currentPair = PairChooser.ChoosePairRandom(app);
            isCurrentPairRandom = true;
        }
        if (show)
            ChangeExercise(currentPair, newExerciseType);
    }

    void FinishRound(boolean isPass) {
        if (isPass) {
            currentPair.period *= 2;
            if (currentPair.period > MyApplication.MaxWordPeriod)
                currentPair.period = 0;
        }
        else {
            if (currentPair.period == 0  || currentPair.period > MyApplication.WordDropPeriod)
                currentPair.period = MyApplication.WordDropPeriod;
            else if (currentPair.period > 1)
                currentPair.period /= 2;
        }
        if (selectionMethod == SelectionMethod.SMART) {
            if (currentPair.period != 0)
                currentPair.next = app.roundId + currentPair.period;
            else
                currentPair.next = -1;
            app.roundId++;
            app.helper.setRoundID(app.roundId);
        }
        app.UpdatePair(currentPair);
        if (isCurrentPairRandom)
            app.helper.pushExerciseResult(isPass);
        StartRound(true);
    }

    boolean isACorrectAnswer(String s, boolean currentFwd) {
        if (currentFwd)
            return (app.wordTranslationsFwd.get(currentPair.first)).contains(s);
        else
            return (app.wordTranslationsBwd.get(currentPair.second)).contains(s);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("isCurrentPairRandom", isCurrentPairRandom);

        outState.putSerializable("selectionMethod", selectionMethod);

        outState.putSerializable("currentPair", currentPair);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        /*if (savedInstanceState == null) {
            StartRound(true);
            return ;
        }*/
    }
}
