package com.mstniy.kelimeezber;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.Toast;

import java.io.File;

enum ExerciseType {
    MC,
    Writing,
    Listening
}

public class ExerciseFragment extends Fragment {
    static final String TAG = ExerciseFragment.class.getName();

    private final int PICKDATASET_REQUEST_CODE = 40914;

    MyApplication app;
    FrameLayout frame;
    MCFragment multipleChoiceFragment;
    Button muteButton;
    WritingFragment writingFragment;
    ListeningFragment listeningFragment;

    public void onAttachFragment(Fragment childFragment) {
        super.onAttachFragment(childFragment);
        if (childFragment instanceof WritingFragment)
            writingFragment = (WritingFragment) childFragment;
        else if (childFragment instanceof MCFragment)
            multipleChoiceFragment = (MCFragment) childFragment;
        else if (childFragment instanceof ListeningFragment)
            listeningFragment = (ListeningFragment)childFragment;
    }

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
        multipleChoiceFragment = new MCFragment();
        writingFragment = new WritingFragment();
        listeningFragment = new ListeningFragment();
        ChangeExercise(app.currentPair, app.exerciseType);
        return rootView;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_exercise, menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_selectiontype) {
            new SelectionTypeDialog().show(getFragmentManager(), "selectiontype");
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
        Fragment fragment = null;
        if (et == ExerciseType.Writing)
            fragment = writingFragment;
        else if (et == ExerciseType.MC)
            fragment = multipleChoiceFragment;
        else if (et == ExerciseType.Listening)
            fragment = listeningFragment;
        if (!fragment.isAdded()) {
            getChildFragmentManager().beginTransaction().replace(R.id.exercise_fragment_frame, fragment).commit();
            getChildFragmentManager().executePendingTransactions();
            return;
        }

        if (et == ExerciseType.MC)
            multipleChoiceFragment.newRound(p);
        else if (et == ExerciseType.Writing)
            writingFragment.newRound(p);
        else if (et == ExerciseType.Listening)
            listeningFragment.newRound(); // TODO: We persist the states of the other exercise types by keeping the state in MyApplication. But we aren't doing the same thing for the random audio sample chosen by ListeningFragment. We should refrain from polluting MyApplication with all the state we wish to persist and instead use Android's Bundle's.
    }

    private void muteButtonPressed() {
        app.isMuted = !app.isMuted;
        if (!app.isMuted) {
            muteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_silent_mode, 0, 0, 0);
            if ((app.exerciseType == ExerciseType.MC && app.currentFwd) || app.exerciseType == ExerciseType.Writing)
                app.speak(app.currentPair.first);
            else if (app.exerciseType == ExerciseType.Listening)
                listeningFragment.speak();
        }
        else
            muteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_silent_mode_off, 0, 0, 0);
    }
}
