package com.mstniy.kelimeezber;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

    MyApplication app;
    Button[] buttons = new Button[4];
    boolean[] buttonsHighlighted = new boolean[4];
    boolean created = false;
    boolean currentFwd;
    TextView label;
    boolean foreignTextShown; // Valid only if currentFwd == true and the app is not muted

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
            buttons[i].setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    buttonClicked((Button) v);
                }
            });
        }
        created = true;
        SoftKeyboardHelper.hideSoftKeyboard(getActivity());
        return rootView;
    }

    void maybeSetLabel() {
        if (currentFwd) {
            if (foreignTextShown)
                label.setText(app.currentPair.first);
        }
        else
            label.setText(app.currentPair.second);
    }

    void setLabel() {
        label.setText(currentFwd ? app.currentPair.first : app.currentPair.second);
    }

    void newRound(Pair p) {
        if (created) {
            for (int i = 0; i < 4; i++)
                ChangeColorOfButton(i, false);
            label.setText("");
            for (int i2 = 0; i2 < 4; i2++)
                buttons[i2].setText("");
            if (p == null)
                return ;
            currentFwd = new Random().nextDouble() <= FWD_PROBABILITY;
            if (app.isMuted == false)
                foreignTextShown = new Random().nextDouble() <= FOREIGN_TEXT_SHOWN_PROB;
            else
                foreignTextShown = true;
            int answer = new Random().nextInt(4);
            maybeSetLabel();
            for (int i3 = 0; i3 < 4; i3++) {
                if (i3 == answer)
                    buttons[i3].setText(currentFwd ? p.second : p.first);
                else {
                    Pair p2 = PairChooser.ChoosePairRandom(app);
                    buttons[i3].setText(currentFwd ? p2.second : p2.first);
                }
            }
            if (currentFwd)
                app.speak(p.first);
        }
    }

    private void ChangeColorOfButton(int buttonIndex, boolean highlight) {
        Button button = buttons[buttonIndex];
        if (highlight)
            button.setBackgroundColor(Color.rgb(100, 255, 100));
        else
            button.setBackgroundResource(android.R.drawable.btn_default);
        buttonsHighlighted[buttonIndex] = highlight;
    }

    private boolean isACorrectAnswer(String s) {
        if (currentFwd) {
            return (app.wordTranslationsFwd.get(app.currentPair.first)).contains(s);
        }
        return (app.wordTranslationsBwd.get(app.currentPair.second)).contains(s);
    }

    private void buttonClicked(Button button) {
        if (app.currentPair != null) {
            if (isACorrectAnswer(button.getText().toString())) {
                app.FinishRound();
            } else {
                app.isPass = false;
                setLabel();
                for (int i = 0; i < 4; i++)
                    if (isACorrectAnswer(buttons[i].getText().toString()))
                        ChangeColorOfButton(i, true);
            }
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
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null) {
            newRound(app.currentPair);
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
    }

    @Override
    public void unmuted() {
        if (currentFwd)
            app.speak(app.currentPair.first);
    }
}
