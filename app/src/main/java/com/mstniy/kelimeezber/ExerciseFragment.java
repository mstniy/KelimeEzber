package com.mstniy.kelimeezber;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

enum ExerciseType {
    MC,
    Writing,
    Listening
}

interface ExerciseFragmentInterface {
    void unmuted();
}

public class ExerciseFragment extends Fragment {
    static final String TAG = ExerciseFragment.class.getName();

    private final int PICKDATASET_REQUEST_CODE = 40914;

    MyApplication app;
    FrameLayout frame;
    Button muteButton;

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
        Fragment.SavedState savedSubFragmentState = null;
        if (savedInstanceState != null)
            savedSubFragmentState = savedInstanceState.getParcelable("subFragment");
        ChangeExercise(app.currentPair, app.exerciseType, savedSubFragmentState);
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
                || (et == ExerciseType.Listening && ! (currentFragment instanceof  ListeningFragment))
                || (et == ExerciseType.MC && ! (currentFragment instanceof  MCFragment))
                || (et == ExerciseType.Writing && ! (currentFragment instanceof  WritingFragment))) {
            Fragment newFragment = null;
            if (et == ExerciseType.Writing)
                newFragment = Fragment.instantiate(getContext(), WritingFragment.class.getName());
            else if (et == ExerciseType.MC)
                newFragment = Fragment.instantiate(getContext(), MCFragment.class.getName());
            else if (et == ExerciseType.Listening)
                newFragment = Fragment.instantiate(getContext(), ListeningFragment.class.getName());
            newFragment.setInitialSavedState(savedSubFragmentState);
            getChildFragmentManager().beginTransaction().replace(R.id.exercise_fragment_frame, newFragment).commit();
            getChildFragmentManager().executePendingTransactions();
            return;
        }

        if (et == ExerciseType.MC)
            ((MCFragment)currentFragment).newRound(p);
        else if (et == ExerciseType.Writing)
            ((WritingFragment)currentFragment).newRound(p);
        else if (et == ExerciseType.Listening)
            ((ListeningFragment)currentFragment).newRound(); // TODO: We persist the states of the other exercise types by keeping the state in MyApplication. But we aren't doing the same thing for the random audio sample chosen by ListeningFragment. We should refrain from polluting MyApplication with all the state we wish to persist and instead use Android's Bundle's.
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Fragment currentSubFragment = getChildFragmentManager().findFragmentById(R.id.exercise_fragment_frame);
        SavedState subFragmentState = getChildFragmentManager().saveFragmentInstanceState(currentSubFragment);
        outState.putParcelable("subFragment", subFragmentState);
    }
}
