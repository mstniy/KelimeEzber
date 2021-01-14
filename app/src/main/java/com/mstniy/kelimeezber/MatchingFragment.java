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

enum ButtonState {
    ENABLED,
    DISABLED,
    HIGHLIGHTED
}

class PairAndWord {
    Pair p;
    String word;

    PairAndWord(Pair p_, String word_) {
        p=p_;
        word=word_;
    }
}

public class MatchingFragment extends Fragment implements ExerciseFragmentInterface {
    final String TAG = getClass().getName();

    final static private int PAIR_COUNT = 4;

    MyApplication app;
    ExerciseFragment exerciseFragment;
    boolean created = false;
    FlexboxLayout wordTable;
    Pair[] buttonPairs = new Pair[2*PAIR_COUNT];
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
        int margin = (int)Math.ceil(factor*5);
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

    void maybeFinished() {
        for (int i=0; i<wordTable.getChildCount(); i++) {
            if (wordTable.getChildAt(i).isEnabled())
                return;
        }

        exerciseFragment.FinishRound();
    }

    void ButtonClicked(int buttonIndex) {
        if (highlightedButtonIndex == -1) {
            highlightedButtonIndex = buttonIndex;
            changeButtonState(buttonIndex, ButtonState.HIGHLIGHTED);
        }
        else if (highlightedButtonIndex == buttonIndex) {
            highlightedButtonIndex = -1;
            changeButtonState(buttonIndex, ButtonState.ENABLED);
        }
        else {
            if (buttonPairs[buttonIndex] == buttonPairs[highlightedButtonIndex]) {
                changeButtonState(buttonIndex, ButtonState.DISABLED);
                changeButtonState(highlightedButtonIndex, ButtonState.DISABLED);
                PeriodHelper.recordRoundOutcome(app, buttonPairs[buttonIndex], true, false, true);
                maybeFinished();
            }
            else {
                for (int i=0; i<2; i++) { // We record two bad outcomes because each pair will eventually get a positive outcome (for the round to end)
                    PeriodHelper.recordRoundOutcome(app, buttonPairs[buttonIndex], false, false, true);
                    PeriodHelper.recordRoundOutcome(app, buttonPairs[highlightedButtonIndex], false, false, true);
                }
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
            ArrayList<PairAndWord> words = new ArrayList<>();
            for (int i=0; i<PAIR_COUNT; i++) {
                Pair pair = PairChooser.ChoosePairRandom(app).p; // TODO: How to allow smart choosing?
                words.add(new PairAndWord(pair, pair.first));
                words.add(new PairAndWord(pair, pair.second));
            }

            Collections.shuffle(words);
            for (int i=0; i<words.size(); i++) {
                String word = words.get(i).word;
                Button b = CreateButton(word, i);
                buttonPairs[i] = words.get(i).p;
                wordTable.addView(b);
            }
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

        long[] pairIds = new long[buttonPairs.length];
        for (int i=0; i<pairIds.length; i++) {
            pairIds[i] = buttonPairs[i].id;
        }
        outState.putLongArray("buttonPairIds", pairIds);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null) {
            newRound();
            return ;
        }

        long[] pairIds = savedInstanceState.getLongArray("buttonPairIds");
        for (int i=0; i<pairIds.length; i++) {
            buttonPairs[i] = app.pairsById.get(pairIds[i]);
            if (buttonPairs[i] == null) { // The user removed one of the displayed pairs (from the word list) and switched back to the exercise tab
                newRound();
                return ;
            }
        }

        CharSequence[] labels = savedInstanceState.getCharSequenceArray("labels");
        boolean[] enabled = savedInstanceState.getBooleanArray("enabled");

        for (int i=0; i<labels.length; i++) {
            Button b = CreateButton(labels[i].toString(), i);
            b.setEnabled(enabled[i]);
            wordTable.addView(b);
        }
    }

    @Override
    public void unmuted() {
    }
}
