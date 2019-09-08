package com.mstniy.kelimeezber;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;
import com.google.android.flexbox.FlexboxLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

public class WritingFragment extends Fragment {
    final String TAG = getClass().getName();
    MyApplication app;
    Button backspace;
    boolean created = false;
    Button hintButton;
    TextView hintView;
    TextView label;
    FlexboxLayout letterTable;
    EditText userInput;

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
        created = true;
        newRound(app.currentPair);
        return rootView;
    }

    void newRound(Pair p) {
        if (created) {
            String str = "";
            label.setText(str);
            hintView.setText(str);
            userInput.setText(str);
            letterTable.removeAllViews();
            if (p != null) {
                label.setText(p.second);
                HashSet<Character> choices = new HashSet<>();
                String word = p.first;
                for (int i = 0; i < word.length(); i++) {
                    choices.add(Character.valueOf(word.charAt(i)));
                }
                ArrayList<Character> choicesArray = new ArrayList<>(choices);
                Collections.shuffle(choicesArray);
                Iterator it = choicesArray.iterator();
                while (it.hasNext()) {
                    Character ch = (Character) it.next();
                    TextView b = new TextView(getContext());
                    b.setText(String.valueOf(ch));
                    b.setBackgroundResource(android.R.drawable.btn_default);
                    b.setTextSize(20.0f);
                    b.setGravity(Gravity.CENTER);
                    b.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            ButtonClicked((TextView) v);
                        }
                    });
                    float factor = getContext().getResources().getDisplayMetrics().density;
                    letterTable.addView(b, new LayoutParams((int) (factor * 59.0f), (int) (59.0f * factor)));
                }
                MyApplication myApplication = app;
                myApplication.speak(myApplication.currentPair.first);
            }
        }
    }

    void SetInputTextAndSelection(String text, int selStart, int selEnd) {
        int roundIdBefore = app.roundId;
        userInput.setText(text);
        if (app.roundId == roundIdBefore) {
            userInput.setSelection(selStart, selEnd);
        }
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
        if (userInput.getText().toString().compareTo(app.currentPair.first) == 0) {
            userInput.setText("");
            app.FinishRound();
        }
    }

    void HintButtonClicked() {
        Pair currentPair = app.currentPair;
        app.isPass = false;
        hintView.setText(currentPair.first);
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
}
