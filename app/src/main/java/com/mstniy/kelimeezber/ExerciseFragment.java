package com.mstniy.kelimeezber;

import android.arch.lifecycle.Observer;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
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

import java.util.HashSet;
import java.util.Random;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class ExerciseFragment extends Fragment {

    static final String TAG = ExerciseFragment.class.getName();

    final double MC_HARDNESS_TRESHOLD = -1.0;
    final double WRITING_PROBABILITY = 0.33; // For easy pairs. Hards pairs always get the writing exercise.
    MyApplication app;
    boolean currentFwd;
    TextView labelMC, labelW, wHintView;
    EditText userInputW;
    Button mcvButtons[] = new Button[4];
    View rootView;
    View multipleChoiceView;
    View writingView;
    Button wHintButton, wBackspace;
    FrameLayout frame;
    FlexboxLayout wLetterTable;
    boolean isMC;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.exercise_root, container, false);
        frame = rootView.findViewById(R.id.frame);
        multipleChoiceView = inflater.inflate(R.layout.exercise_multiple_choice, frame, false);
        frame.addView(multipleChoiceView); // Settings attachToRoot=true in the call to *inflate* above is NOT the same thing.
        isMC = true;
        writingView = inflater.inflate(R.layout.exercise_writing, frame, false);

        app = (MyApplication) getContext().getApplicationContext();

        labelMC = multipleChoiceView.findViewById(R.id.label);
        mcvButtons[0] = multipleChoiceView.findViewById(R.id.button0);
        mcvButtons[1] = multipleChoiceView.findViewById(R.id.button1);
        mcvButtons[2] = multipleChoiceView.findViewById(R.id.button2);
        mcvButtons[3] = multipleChoiceView.findViewById(R.id.button3);
        for (int i=0;i<4;i++)
            mcvButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MCVButtonClicked((Button)v);
                }
            });

        labelW = writingView.findViewById(R.id.label);
        wHintView = writingView.findViewById(R.id.hint_view);
        wHintButton = writingView.findViewById(R.id.hint_button);

        wHintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WHintButtonClicked();
            }
        });

        wBackspace = writingView.findViewById(R.id.backspace_button);
        wBackspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WBackspaceClicked();
            }
        });

        userInputW = writingView.findViewById(R.id.user_input);

        userInputW.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                WEditTextChanged();
            }
        });

        wLetterTable = writingView.findViewById(R.id.letter_table);

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
        frame.removeAllViews();
        frame.addView(mc ? multipleChoiceView : writingView);
        isMC = mc;
    }

    void newRoundMC(Pair p) {
        //Clear the view first (we have the old exercise on it)
        for (int i=0;i<4;i++)
            ChangeColorOfButton(mcvButtons[i], false);
        labelMC.setText("");
        for (int i=0; i<4; i++)
            mcvButtons[i].setText("");

        currentFwd = new Random().nextBoolean();
        final int answer=new Random().nextInt(4);
        labelMC.setText(currentFwd?p.first:p.second);
        for (int i=0;i<4;i++)
        {
            if (i == answer)
                mcvButtons[i].setText(currentFwd ? p.second : p.first);
            else {
                final Pair p2 = PairChooser.ChoosePairRandom(app);
                mcvButtons[i].setText(currentFwd?p2.second:p2.first);
            }
        }
    }

    void newRoundW(Pair p) {
        // Clear the view first (we have the old exercise on it)
        labelW.setText("");
        wHintView.setText("");
        userInputW.setText("");
        wLetterTable.removeAllViews();

        currentFwd = new Random().nextBoolean();
        labelW.setText(currentFwd?p.first:p.second);
        HashSet<Character> choices = new HashSet<>();
        //TODO: Maybe have a dedicated button for space? We also need to check if the word is suitable for writing challenge (it may be too long)
        //TODO: And also, if the words has a lot of translations, trying to add all of their letters on the screen will be a mess.
        //TODO: And maybe add some "trap" letters for extra difficulty?
        for (String word : currentFwd?app.wordTranslationsFwd.get(p.first):app.wordTranslationsBwd.get(p.second))
            for (int i=0; i<word.length(); i++)
                choices.add(word.charAt(i));
        //TODO: Add the buttons in a random order here. Or maybe call the addView method with a (random) index?
        for (Character ch : choices) {
            TextView b = new TextView(getContext());
            b.setText(String.valueOf(ch));
            b.setBackgroundResource(android.R.drawable.btn_default);
            b.setTextSize(20);
            b.setGravity(Gravity.CENTER);
            //b.setMinWidth(75);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    WButtonClicked((TextView)v);
                }
            });
            final float factor = getContext().getResources().getDisplayMetrics().density;
            Log.d(TAG, String.valueOf(factor));
            ViewGroup.LayoutParams lp = new FrameLayout.LayoutParams((int)(59*factor), (int)(59*factor));
            wLetterTable.addView(b, lp);
        }
    }

    void cpiChanged(Pair p) {
        if (p == null)
            return ;
        setMC(!(p.hardness >= MC_HARDNESS_TRESHOLD || new Random().nextDouble() <= WRITING_PROBABILITY));
        if (isMC)
            newRoundMC(p);
        else
            newRoundW(p);
    }

    void ChangeColorOfButton(Button button, boolean highlight)
    {
        if (highlight)
            button.setBackgroundColor(Color.rgb(100, 255, 100));
        else
            button.setBackgroundResource(android.R.drawable.btn_default);
    }

    boolean isACorrectAnswer(String s) {
        if (currentFwd)
            return app.wordTranslationsFwd.get(app.currentPair.getValue().first.toString()).contains(s);
        else
            return app.wordTranslationsBwd.get(app.currentPair.getValue().second.toString()).contains(s);
    }

    void WSetInputTextAndSelection(String text, int selStart, int selEnd) {
        final int roundIdBefore = app.roundId;
        userInputW.setText(text);
        //WEditTextChanged(); // Android calls this automatically
        if (app.roundId != roundIdBefore) // Changing the content of the user input may change the round (if the new value is a correct answer)
            return ;
        userInputW.setSelection(selStart, selEnd);
    }

    void WButtonClicked(TextView button) {
        //userInputW.append(button.getText());
        final int selStart = userInputW.getSelectionStart();
        final int selEnd = userInputW.getSelectionEnd();
        String olds = userInputW.getText().toString();
        String news = olds.substring(0, selStart) + button.getText() + olds.substring(selEnd);

        if (selStart != selEnd)
            WSetInputTextAndSelection(news, selStart, selStart+1);
        else {
            WSetInputTextAndSelection(news, selStart + 1, selStart+1);
        }
    }

    void FinishRound(boolean pass) {
        final Pair currentPair = app.currentPair.getValue();
        final double oldScore = currentPair.hardness;
        double newScore = oldScore;
        if (pass)
            newScore -= isMC ? 0.33 : 0.5;
        else
            newScore += isMC ? 0.5 : 0.33;
        newScore = min(newScore, 2.0);
        newScore = max(newScore, -1.33);

        currentPair.hardness = newScore; // Update the score of the current word
        app.HardnessChanged(currentPair);
        app.NewRound();
    }

    void MCVButtonClicked(Button button)
    {
        final Pair currentPair = app.currentPair.getValue();
        if (currentPair == null)
            return ;
	    if (isACorrectAnswer(button.getText().toString()))
            FinishRound(app.mistakeQueue[app.currentQueueIndex] == null);
        else
        {
            //cout << "Incorrect!" << endl;
            app.mistakeQueue[app.currentQueueIndex]=currentPair;
            for (int i=0;i<4;i++)
                if (isACorrectAnswer(mcvButtons[i].getText().toString()))
                    ChangeColorOfButton(mcvButtons[i], true);
        }
    }

    void WEditTextChanged() {
        if (isACorrectAnswer(userInputW.getText().toString()))
            FinishRound(app.mistakeQueue[app.currentQueueIndex] == null );
    }

    void WHintButtonClicked() {
        Pair currentPair = app.currentPair.getValue();
        app.mistakeQueue[app.currentQueueIndex]=currentPair;
        wHintView.setText(currentFwd ? currentPair.second : currentPair.first);
    }

    void WBackspaceClicked() {
        final int selStart = userInputW.getSelectionStart();
        final int selEnd = userInputW.getSelectionEnd();
        if (selStart == selEnd && selStart == 0)
            return ;
        String olds = userInputW.getText().toString();
        if (selStart == selEnd) {
            String news = olds.substring(0, selStart - 1) + olds.substring(selStart);
            WSetInputTextAndSelection(news, selStart-1, selStart-1);
        }
        else {
            String news = olds.substring(0, selStart) + olds.substring(selEnd);
            WSetInputTextAndSelection(news, selStart, selStart);
        }
    }
}
