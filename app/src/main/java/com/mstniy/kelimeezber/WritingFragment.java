package com.mstniy.kelimeezber;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;


public class WritingFragment extends Fragment {

    final String TAG = getClass().getName();

    MyApplication app;
    boolean currentFwd;
    TextView labelW, wHintView;
    EditText userInputW;
    Button wHintButton, wBackspace;
    FlexboxLayout wLetterTable;
    // If this is false, the user has failed the current exercise (for example, clicked a wrong answer for a multiple choice exercise or requested a hint for a writing exercise)
    // Set to true at the beginning of each round.
    boolean isPass;

    public WritingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        app = (MyApplication) getContext().getApplicationContext();
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_writing, container, false);

        labelW = rootView.findViewById(R.id.labelW);
        wHintView = rootView.findViewById(R.id.hint_view);
        wHintButton = rootView.findViewById(R.id.hint_button);

        wHintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WHintButtonClicked();
            }
        });

        wBackspace = rootView.findViewById(R.id.backspace_button);
        wBackspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WBackspaceClicked();
            }
        });

        userInputW = rootView.findViewById(R.id.user_input);

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

        wLetterTable = rootView.findViewById(R.id.letter_table);

        return rootView;
    }

    void newRoundW(Pair p, boolean _currentFwd) {
        isPass = true;
        currentFwd = _currentFwd;
        // Clear the view first (we have the old exercise on it)
        labelW.setText("");
        wHintView.setText("");
        userInputW.setText("");
        wLetterTable.removeAllViews();

        labelW.setText(currentFwd?p.first:p.second);
        //TODO: Maybe have a dedicated button for space? We also need to check if the word is suitable for writing challenge (it may be too long)
        //TODO: And also, if the words has a lot of translations, trying to add all of their letters on the screen will be a mess.
        //TODO: And maybe add some "trap" letters for extra difficulty?
        HashSet<Character> choices = new HashSet<>();
        if (true) // Limit the scope of *word*
        {
            String word = currentFwd?p.second:p.first;
            for (int i=0; i<word.length(); i++)
                choices.add(word.charAt(i));
        }
        ArrayList<Character> choicesArray = new ArrayList<>(choices);
        Collections.shuffle(choicesArray);
        for (Character ch : choicesArray) {
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
            ViewGroup.LayoutParams lp = new FrameLayout.LayoutParams((int)(59*factor), (int)(59*factor));
            wLetterTable.addView(b, lp);
        }
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

    void WEditTextChanged() {
        Pair p = app.currentPair.getValue();
        String answer = currentFwd?p.second:p.first;
        if (userInputW.getText().toString().compareTo(answer) == 0)
            app.FinishRound(false, isPass);
    }

    void WHintButtonClicked() {
        Pair currentPair = app.currentPair.getValue();
        isPass = false;
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
