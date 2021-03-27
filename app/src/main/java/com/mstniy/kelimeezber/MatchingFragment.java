package com.mstniy.kelimeezber;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout.LayoutParams;

import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

enum ButtonState {
    ENABLED,
    DISABLED,
    HIGHLIGHTED
}

class PSRAndWord {
    PairSelectResult p;
    String word;

    PSRAndWord(PairSelectResult p_, String word_) {
        p=p_;
        word=word_;
    }
}

public class MatchingFragment extends Fragment implements ExerciseFragmentInterface {
    final String TAG = getClass().getName();

    final static private int PAIR_COUNT = 6;

    MyApplication app;
    ExerciseFragment exerciseFragment;
    boolean created = false;
    FlexboxLayout wordTable;
    PairSelectResult[] buttonPairs = new PairSelectResult[2*PAIR_COUNT];
    Set<Long> wrongAnswer = new HashSet<>();
    int highlightedButtonIndex = -1;


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        app = (MyApplication) getContext().getApplicationContext();
        View rootView = inflater.inflate(R.layout.fragment_matching, container, false);
        wordTable = rootView.findViewById(R.id.word_table);
        exerciseFragment = (ExerciseFragment)getParentFragment();
        created = true;
        return rootView;
    }

    Button CreateButton(String s, final int buttonIndex) {
        Button b = new Button(getContext(), null, 0, R.style.BlueButton);
        b.setText(s);
        b.setTextSize(20.0f);
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ButtonClicked(buttonIndex);
            }
        });
        float factor = getContext().getResources().getDisplayMetrics().density;
        LayoutParams layout = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int)Math.ceil(factor*4);
        layout.setMargins(margin, margin*3, margin, margin*3);
        b.setLayoutParams(layout);
        return b;
    }

    private void changeButtonState(int buttonIndex, ButtonState state) {
        Button button = (Button)wordTable.getChildAt(buttonIndex);
        button.setEnabled(state != ButtonState.DISABLED);
        if (state == ButtonState.HIGHLIGHTED)
            button.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.ButtonBlueHighlight));
        else if (state == ButtonState.ENABLED)
            button.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.ButtonBlue));
        else if (state == ButtonState.DISABLED)
            button.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.ButtonBlueDisabled));
    }

    int getEnabledButtonCount() {
        int res=0;
        for (int i=0; i<wordTable.getChildCount(); i++) {
            if (wordTable.getChildAt(i).isEnabled())
                res++;
        }
        return res;
    }

    void maybeFinished() {
        for (int i=0; i<wordTable.getChildCount(); i++) {
            if (wordTable.getChildAt(i).isEnabled())
                return;
        }

        Set<Long> correctPairs = new HashSet<>();
        for (PairSelectResult p : buttonPairs)
            if (wrongAnswer.contains(p.p.id) == false) // If a pair was involved in a mismatch, we completely exclude it from decreaseConfusion
                correctPairs.add(p.p.id);

        for (Long pair1 : correctPairs)
            for (Long pair2 : correctPairs) // A complexity of O(NUM_PAIRS**2) if fine, considering we have only 4 pairs
                if (pair2 > pair1)
                        app.helper.decreaseConfusion(pair1, pair2); // Note that a confusion entry for won't be defined for many pairs of pairs, so this line won't do anything most of the time.

        exerciseFragment.FinishRound();
    }

    void ButtonClicked(int buttonIndex) {
        if (highlightedButtonIndex == buttonIndex) {
            highlightedButtonIndex = -1;
            changeButtonState(buttonIndex, ButtonState.ENABLED);
            return ;
        }

        if (highlightedButtonIndex == -1) {
            highlightedButtonIndex = buttonIndex;
            changeButtonState(buttonIndex, ButtonState.HIGHLIGHTED);
            if (buttonPairs[buttonIndex].p.first.equals(((Button)wordTable.getChildAt(buttonIndex)).getText()))
                app.speak(buttonPairs[buttonIndex].p.first);
        }
        else {
            if (buttonPairs[buttonIndex].p == buttonPairs[highlightedButtonIndex].p) {
                changeButtonState(buttonIndex, ButtonState.DISABLED);
                changeButtonState(highlightedButtonIndex, ButtonState.DISABLED);
                if (getEnabledButtonCount() > 1) {
                    if (wrongAnswer.contains(buttonPairs[buttonIndex].p.id) == false) // Avoid double-recording
                        PeriodHelper.recordRoundOutcome(app, buttonPairs[buttonIndex], RoundOutcome.PASS);
                    if (buttonPairs[buttonIndex].p.first.equals(((Button)wordTable.getChildAt(buttonIndex)).getText()))
                        app.speak(buttonPairs[buttonIndex].p.first);
                }
                else { // The outcome for the last two remaining pairs is at best NEUTRAL (or of course FAIL, if it got mismatched earlier on)
                    if (wrongAnswer.contains(buttonPairs[buttonIndex].p.id) == false) // Avoid double-recording
                        PeriodHelper.recordRoundOutcome(app, buttonPairs[buttonIndex], RoundOutcome.NEUTRAL);
                }
                maybeFinished();
            }
            else { // Mismatch
                if (wrongAnswer.contains(buttonPairs[buttonIndex].p.id) == false && wrongAnswer.contains(buttonPairs[highlightedButtonIndex].p.id) == false) {
                    PeriodHelper.recordRoundOutcome(app, buttonPairs[buttonIndex], RoundOutcome.FAIL);
                    PeriodHelper.recordRoundOutcome(app, buttonPairs[highlightedButtonIndex], RoundOutcome.FAIL);
                    app.helper.increaseConfusion(buttonPairs[buttonIndex].p.id, buttonPairs[highlightedButtonIndex].p.id);
                }
                wrongAnswer.add(buttonPairs[buttonIndex].p.id);
                wrongAnswer.add(buttonPairs[highlightedButtonIndex].p.id);
                changeButtonState(highlightedButtonIndex, ButtonState.ENABLED);
            }
            highlightedButtonIndex = -1;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SoftKeyboardHelper.hideSoftKeyboard(getActivity());
    }

    @Override
    public void newRound() {
        if (created) {
            wordTable.removeAllViews();
            ArrayList<PairSelectResult> pairs = PairChooser.ChoosePair(app, exerciseFragment.selectionMethod, PAIR_COUNT);
            ArrayList<PSRAndWord> words = new ArrayList<>();
            for (int i=0; i<PAIR_COUNT; i++) {
                PairSelectResult pair = pairs.get(i);
                words.add(new PSRAndWord(pair, pair.p.first));
                words.add(new PSRAndWord(pair, pair.p.second));
            }

            Collections.shuffle(words); // Shuffle the list of words before sorting it because there is no guarantee that the order returned by ChoosePair is random, and Collections.sort is stable
            Collections.sort(words, new Comparator<PSRAndWord>() { // Sort words by length before displaying them to fit more words on the screen
                @Override
                public int compare(PSRAndWord o1, PSRAndWord o2) {
                    return o1.word.length() - o2.word.length();
                }
            });
            for (int i=0; i<words.size(); i++) {
                String word = words.get(i).word;
                Button b = CreateButton(word, i);
                buttonPairs[i] = words.get(i).p;
                wordTable.addView(b);
            }

            wrongAnswer.clear();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        CharSequence[] labels = new CharSequence[wordTable.getChildCount()];
        boolean[] enabled = new boolean[labels.length];

        for (int i=0; i<labels.length; i++) {
            Button b = (Button)wordTable.getChildAt(i);
            labels[i] = b.getText().toString();
            enabled[i] = b.isEnabled();
        }

        outState.putCharSequenceArray("labels", labels);
        outState.putBooleanArray("enabled", enabled);
        outState.putSerializable("buttonPairs", buttonPairs);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null) {
            newRound();
            return ;
        }

        buttonPairs = (PairSelectResult[]) savedInstanceState.getSerializable("buttonPairs");

        for (PairSelectResult p : buttonPairs)
            if (app.pairsById.containsKey(p.p.id) == false) { // The user removed the current pair (from the word list) and switched back to the exercise tab
                newRound();
                return ;
            }

        CharSequence[] labels = savedInstanceState.getCharSequenceArray("labels");
        boolean[] enabled = savedInstanceState.getBooleanArray("enabled");

        for (int i=0; i<labels.length; i++) {
            Button b = CreateButton(labels[i].toString(), i);
            wordTable.addView(b);
            changeButtonState(i, enabled[i]?ButtonState.ENABLED:ButtonState.DISABLED);
        }
    }

    @Override
    public void unmuted() {
    }
}
