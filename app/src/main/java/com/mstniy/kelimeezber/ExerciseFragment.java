package com.mstniy.kelimeezber;

import android.content.Context;
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

enum ExerciseType {
    MC,
    Writing
}

public class ExerciseFragment extends Fragment {
    static final String TAG = ExerciseFragment.class.getName();
    MyApplication app;
    FrameLayout frame;
    MCFragment multipleChoiceFragment;
    Button muteButton;
    WritingFragment writingFragment;

    public void onAttachFragment(Fragment childFragment) {
        super.onAttachFragment(childFragment);
        if (childFragment instanceof WritingFragment) {
            writingFragment = (WritingFragment) childFragment;
        } else if (childFragment instanceof MCFragment) {
            multipleChoiceFragment = (MCFragment) childFragment;
        }
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
        if (!app.ttsMuted)
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
        ChangeExercise(app.currentPair, app.exerciseType);
        return rootView;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_exercise, menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != R.id.action_selectiontype) {
            return super.onOptionsItemSelected(item);
        }
        new SelectionTypeDialog().show(getFragmentManager(), "selectiontype");
        return true;
    }

    void ChangeExercise(Pair p, ExerciseType et) {
        Fragment fragment = null;
        if (et == ExerciseType.Writing) {
            fragment = writingFragment;
        } else if (et == ExerciseType.MC) {
            fragment = multipleChoiceFragment;
        }
        if (!fragment.isAdded()) {
            getChildFragmentManager().beginTransaction().replace(R.id.exercise_fragment_frame, fragment).commit();
            getChildFragmentManager().executePendingTransactions();
        } else if (et == ExerciseType.MC) {
            multipleChoiceFragment.newRound(p);
        } else if (et == ExerciseType.Writing) {
            writingFragment.newRound(p);
        }
    }

    private void muteButtonPressed() {
        MyApplication myApplication = app;
        myApplication.ttsMuted = !myApplication.ttsMuted;
        if (!app.ttsMuted) {
            muteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_silent_mode, 0, 0, 0);
            if ((app.exerciseType == ExerciseType.MC && app.currentFwd) || app.exerciseType == ExerciseType.Writing) {
                MyApplication myApplication2 = app;
                myApplication2.speak(myApplication2.currentPair.first);
                return;
            }
            return;
        }
        muteButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_lock_silent_mode_off, 0, 0, 0);
    }
}
