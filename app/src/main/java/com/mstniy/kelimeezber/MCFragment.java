package com.mstniy.kelimeezber;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;

public class MCFragment extends Fragment implements ExerciseFragmentInterface {
    final String TAG = getClass().getName();
    final double FWD_PROBABILITY = 0.5;
    final double FOREIGN_TEXT_SHOWN_PROB = 0.5;

    ExerciseFragment exerciseFragment;
    MyApplication app;
    Button[] buttons = new Button[4];
    boolean[] buttonsHighlighted = new boolean[4];
    boolean created = false;
    boolean currentFwd;
    TextView label;
    boolean foreignTextShown; // Valid only if currentFwd == true and the app is not muted
    boolean isPass;
    PairSelectResult currentPair;
    Pair[] buttonPairs = new Pair[4];

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        app = (MyApplication) getContext().getApplicationContext();
        Log.d(TAG, "OnCreateView");
        View rootView = inflater.inflate(R.layout.fragment_mc, container, false);
        label = rootView.findViewById(R.id.label);
        buttons[0] = rootView.findViewById(R.id.button0);
        buttons[1] = rootView.findViewById(R.id.button1);
        buttons[2] = rootView.findViewById(R.id.button2);
        buttons[3] = rootView.findViewById(R.id.button3);
        for (int i = 0; i < 4; i++) {
            final int ii = i;
            buttons[i].setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    buttonClicked(ii);
                }
            });
        }
        exerciseFragment = (ExerciseFragment)getParentFragment();
        created = true;
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        SoftKeyboardHelper.hideSoftKeyboard(getActivity());
    }

    void maybeSetLabel() {
        if (currentFwd) {
            if (foreignTextShown)
                label.setText(currentPair.p.first);
        }
        else
            label.setText(currentPair.p.second);
    }

    void setLabel() {
        label.setText(currentFwd ? currentPair.p.first : currentPair.p.second);
    }

    @Override
    public void newRound() {
        if (created) {
            isPass = true;
            for (int i = 0; i < 4; i++)
                ChangeColorOfButton(i, false);
            label.setText("");
            currentPair = PairChooser.ChoosePair(app, exerciseFragment.selectionMethod);
            currentFwd = new Random().nextDouble() <= FWD_PROBABILITY;
            if (app.isMuted == false && app.ttsSupported)
                foreignTextShown = new Random().nextDouble() <= FOREIGN_TEXT_SHOWN_PROB;
            else
                foreignTextShown = true;
            int answer = new Random().nextInt(4);
            maybeSetLabel();
            for (int i3 = 0; i3 < 4; i3++) {
                if (i3 == answer) {
                    buttons[i3].setText(currentFwd ? currentPair.p.second : currentPair.p.first);
                    buttonPairs[i3] = currentPair.p;
                }
                else {
                    Pair p2 = PairChooser.ChoosePairRandom(app).p;
                    buttonPairs[i3] = p2;
                    buttons[i3].setText(currentFwd ? p2.second : p2.first);
                }
            }
            if (currentFwd)
                app.speak(currentPair.p.first);
        }
    }

    private void ChangeColorOfButton(int buttonIndex, boolean highlight) {
        Button button = buttons[buttonIndex];
        if (highlight)
            button.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.colorAccent));
        else
            button.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.ButtonBlue));
        buttonsHighlighted[buttonIndex] = highlight;
    }

    private void buttonClicked(int buttonIndex) {
        Button button = buttons[buttonIndex];
        if (app.isACorrectAnswer(currentPair.p, button.getText().toString(), currentFwd)) {
            if (isPass)
                PeriodHelper.recordRoundOutcome(app, currentPair, true);
            // TODO: We need to decrease confusion perhaps, see MatchingFragment
            exerciseFragment.FinishRound();
        } else {
            if (isPass) {
                PeriodHelper.recordRoundOutcome(app, new PairSelectResult(buttonPairs[buttonIndex], SelectionMethod.RANDOM), false);
                app.helper.increaseConfusion(currentPair.p.id, buttonPairs[buttonIndex].id);
                PeriodHelper.recordRoundOutcome(app, currentPair, false);
            }
            isPass = false;
            setLabel();
            for (int i = 0; i < 4; i++)
                if (app.isACorrectAnswer(currentPair.p, buttons[i].getText().toString(), currentFwd))
                    ChangeColorOfButton(i, true);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("currentFwd", currentFwd);
        outState.putBoolean("foreignTextShown", foreignTextShown);

        CharSequence[] buttonTexts = new CharSequence[4];
        for (int i=0;i<4;i++)
            buttonTexts[i] = buttons[i].getText();
        outState.putCharSequenceArray("buttonTexts", buttonTexts);

        outState.putBooleanArray("buttonsHighlighted", buttonsHighlighted);
        outState.putCharSequence("label", label.getText());
        outState.putBoolean("isPass", isPass);
        outState.putLong("currentPairId", currentPair.p.id);
        outState.putSerializable("currentPairSelectionMethod", currentPair.method);

        long[] buttonPairIds = new long[4];
        for (int i=0; i<4; i++)
            buttonPairIds[i] = buttonPairs[i].id;
        outState.putLongArray("buttonPairIds", buttonPairIds);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null) {
            newRound();
            return ;
        }

        Long currentPairId = savedInstanceState.getLong("currentPairId");
        currentPair = new PairSelectResult(app.pairsById.get(currentPairId), (SelectionMethod) savedInstanceState.getSerializable("currentPairSelectionMethod"));
        if (currentPair.p == null) { // The user removed the current pair (from the word list) and switched back to the exercise tab
            newRound();
            return ;
        }

        currentFwd = savedInstanceState.getBoolean("currentFwd");
        foreignTextShown = savedInstanceState.getBoolean("foreignTextShown");
        maybeSetLabel();

        CharSequence[] buttonTexts = savedInstanceState.getCharSequenceArray("buttonTexts");
        for (int i=0; i<4; i++)
            buttons[i].setText(buttonTexts[i]);

        boolean[] buttonsHighlighted = savedInstanceState.getBooleanArray("buttonsHighlighted");
        for (int i=0; i<4; i++)
            ChangeColorOfButton(i, buttonsHighlighted[i]);

        label.setText(savedInstanceState.getCharSequence("label"));
        isPass = savedInstanceState.getBoolean("isPass");

        long[] buttonPairIds = savedInstanceState.getLongArray("buttonPairIds");
        for (int i=0; i<4; i++)
            buttonPairs[i] = app.pairsById.get(buttonPairIds[i]);
    }

    @Override
    public void unmuted() {
        if (currentFwd)
            app.speak(currentPair.p.first);
    }
}
