package com.mstniy.kelimeezber;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.flexbox.FlexboxLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

public class WritingFragment extends Fragment implements ExerciseFragmentInterface {
    final String TAG = getClass().getName();

    final private double LETTER_TABLE_AVAILABLE_PROB = 0.75;
    final private double FOREIGN_SPEECH_AVAILABLE_PROB = 0.35; // Valid only if the app is not muted.

    MyApplication app;
    ExerciseFragment exerciseFragment;
    ImageView backspace;
    boolean created = false;
    ImageView hintButton;
    TextView hintView;
    TextView label;
    FlexboxLayout letterTable;
    EditText userInput;
    boolean letterTableAvailable; // If false, no letter table is given to the user. This is more challenging.
    boolean foreignSpeechAvailable; // Valid only if the app is not muted.
    boolean suppressEditTextChangedCallback = false;
    boolean isPass;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        app = (MyApplication) getContext().getApplicationContext();
        View rootView = inflater.inflate(R.layout.fragment_writing, container, false);
        label = rootView.findViewById(R.id.label);
        hintView = rootView.findViewById(R.id.hint_view);
        hintButton = rootView.findViewById(R.id.hint_button);
        hintButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                HintButtonClicked();
            }
        });
        backspace = rootView.findViewById(R.id.backspace_button);
        backspace.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                BackspaceClicked();
            }
        });
        userInput = rootView.findViewById(R.id.user_input);
        userInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                EditTextChanged();
            }
        });
        letterTable = rootView.findViewById(R.id.letter_table);
        exerciseFragment = (ExerciseFragment)getParentFragment();
        created = true;
        return rootView;
    }

    Button CreateButton(String s) {
        Button b = new Button(getContext(), null, 0, R.style.BlueButton);
        b.setText(s);
        b.setTextSize(20.0f);
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ButtonClicked((TextView) v);
            }
        });
        float factor = getContext().getResources().getDisplayMetrics().density;
        int btnSize = (int)Math.ceil(factor*59);
        LayoutParams layout = new LayoutParams(btnSize, btnSize);
        int margin = (int)Math.ceil(factor*3);
        layout.setMargins(margin, margin, margin, margin);
        b.setLayoutParams(layout);
        return b;
    }

    void newRound(Pair p) {
        if (created) {
            isPass = true;
            label.setText("");
            hintView.setText("");
            userInput.setText("");
            letterTable.removeAllViews();
            if (p == null)
                return ;

            letterTableAvailable = new Random().nextDouble() <= LETTER_TABLE_AVAILABLE_PROB;
            if (app.isMuted == false && app.ttsSupported)
                foreignSpeechAvailable = new Random().nextDouble() <= FOREIGN_SPEECH_AVAILABLE_PROB;
            else
                foreignSpeechAvailable = false;

            if (letterTableAvailable) {
                HashSet<Character> choices = new HashSet<>();
                String word = p.first;
                for (int i = 0; i < word.length(); i++)
                    choices.add(Character.valueOf(word.charAt(i)));
                ArrayList<Character> choicesArray = new ArrayList<>(choices);
                Collections.shuffle(choicesArray);
                for (Character ch : choicesArray) {
                    Button b = CreateButton(String.valueOf(ch));
                    letterTable.addView(b);
                }
                backspace.setVisibility(View.VISIBLE);
            }
            else {
                userInput.requestFocus();
                SoftKeyboardHelper.showSoftKeyboard(getActivity());
                backspace.setVisibility(View.INVISIBLE);
            }
            if (foreignSpeechAvailable)
                app.speak(exerciseFragment.currentPair.first);
            label.setText(exerciseFragment.currentPair.second);
        }
    }

    void SetInputTextAndSelection(String text, int selStart, int selEnd) {
        suppressEditTextChangedCallback = true;
        userInput.setText(text);
        userInput.setSelection(selStart, selEnd);
        suppressEditTextChangedCallback = false;
        EditTextChanged();
    }

    void ButtonClicked(TextView button) {
        int selStart = userInput.getSelectionStart();
        int selEnd = userInput.getSelectionEnd();
        String olds = userInput.getText().toString();
        StringBuilder sb = new StringBuilder();
        sb.append(olds.substring(0, selStart));
        sb.append(button.getText());
        sb.append(olds.substring(selEnd));
        String news = sb.toString();
        if (selStart != selEnd) {
            SetInputTextAndSelection(news, selStart, selStart + 1);
        } else {
            SetInputTextAndSelection(news, selStart + 1, selStart + 1);
        }
    }

    void EditTextChanged() {
        if (suppressEditTextChangedCallback)
            return ;
        if (isAdded() == false)
            return ;
        if ((letterTableAvailable && userInput.getText().toString().compareTo(exerciseFragment.currentPair.first) == 0) ||
                (letterTableAvailable == false && exerciseFragment.isACorrectAnswer(userInput.getText().toString(), false))) {
            userInput.setText("");
            exerciseFragment.FinishRound(isPass);
        }
    }

    void HintButtonClicked() {
        Pair currentPair = exerciseFragment.currentPair;
        isPass = false;
        hintView.setText(currentPair.first);
        label.setText(currentPair.second);
        app.speak(currentPair.first);
    }

    void BackspaceClicked() {
        int selStart = userInput.getSelectionStart();
        int selEnd = userInput.getSelectionEnd();
        if (selStart != selEnd || selStart != 0) {
            String olds = userInput.getText().toString();
            if (selStart == selEnd) {
                StringBuilder sb = new StringBuilder();
                sb.append(olds.substring(0, selStart - 1));
                sb.append(olds.substring(selStart));
                SetInputTextAndSelection(sb.toString(), selStart - 1, selStart - 1);
            } else {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(olds.substring(0, selStart));
                sb2.append(olds.substring(selEnd));
                SetInputTextAndSelection(sb2.toString(), selStart, selStart);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putCharSequence("userInput", userInput.getText());

        outState.putBoolean("letterTableAvailable", letterTableAvailable);

        if (letterTableAvailable == false)
            SoftKeyboardHelper.showSoftKeyboard(getActivity());

        outState.putBoolean("foreignSpeechAvailable", foreignSpeechAvailable);

        CharSequence[] letters = new CharSequence[letterTable.getChildCount()];
        for (int i=0; i<letterTable.getChildCount(); i++)
            letters[i] = ((TextView)letterTable.getChildAt(i)).getText();
        outState.putCharSequenceArray("letters", letters);

        outState.putBoolean("isPass", isPass);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null || exerciseFragment.childIgnoreState) {
            newRound(exerciseFragment.currentPair);
            return ;
        }

        letterTableAvailable = savedInstanceState.getBoolean("letterTableAvailable");
        foreignSpeechAvailable = savedInstanceState.getBoolean("foreignSpeechAvailable");

        label.setText(exerciseFragment.currentPair.second);

        isPass = savedInstanceState.getBoolean("isPass");

        if (isPass == false) {
            hintView.setText(exerciseFragment.currentPair.first);
            label.setText(exerciseFragment.currentPair.second);
        }
        else
            hintView.setText("");

        CharSequence[] letters = savedInstanceState.getCharSequenceArray("letters");
        for (int i=0; i<letters.length; i++)
            letterTable.addView(CreateButton(letters[i].toString()));
    }

    @Override
    public void unmuted() {
        if (foreignSpeechAvailable)
            app.speak(exerciseFragment.currentPair.first);
    }
}
