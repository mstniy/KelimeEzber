package com.mstniy.kelimeezber;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;
import com.opencsv.CSVReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AudioAndWords implements Serializable {
    String audioPath;
    ArrayList<String> words;
    String sentence;
    AudioAndWords(String _audioPath, ArrayList<String> _words, String _sentence) {
        audioPath = _audioPath;
        words = _words;
        sentence = _sentence;
    }
}

public class ListeningFragment extends Fragment {
    final String TAG = getClass().getName();
    final static int MINIMUM_SENTENCE_LENGTH_IN_WORDS = 4;

    MyApplication app;
    boolean created = false;
    Button hintButton, replayButton;
    TextView hintView;
    FlexboxLayout wordTableInput, wordTableOptions;
    AudioAndWords p = null;

    public void onAttach(Context context) {
        super.onAttach(context);
        app = (MyApplication) context.getApplicationContext();
        app.listeningFragment = this;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) throws RuntimeException{
        app = (MyApplication) getContext().getApplicationContext();
        View rootView = inflater.inflate(R.layout.fragment_listening, container, false);
        hintView = rootView.findViewById(R.id.hint_view);
        hintButton = rootView.findViewById(R.id.hint_button);
        hintButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                HintButtonClicked();
            }
        });
        replayButton = rootView.findViewById(R.id.replay_button);
        replayButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ReplayButtonClicked();
            }
        });
        wordTableInput = rootView.findViewById(R.id.word_box_input);
        wordTableOptions = rootView.findViewById(R.id.word_box_options);
        created = true;
        SoftKeyboardHelper.hideSoftKeyboard(getActivity());
        return rootView;
    }

    void newRound() {
        newRound(app.ats.get(new Random().nextInt(app.ats.size())));
    }

    TextView CreateButton(String text) {
        TextView b = new TextView(getContext());
        b.setText(text);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b.setTextSize(20.0f);
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ButtonClicked((TextView) v);
            }
        });
        return b;
    }

    void newRound(AudioAndWords _p) {
        if (created == false)
            return ;
        p = _p;
        hintView.setText("");
        wordTableInput.removeAllViews();
        wordTableOptions.removeAllViews();
        if (p == null)
            return ;
        ArrayList<String> shuffledWords = new ArrayList<>();
        shuffledWords.addAll(p.words);
        Collections.shuffle(shuffledWords);
        for (String word : shuffledWords)
            wordTableOptions.addView(CreateButton(word));
        app.playAudio(p.audioPath);
    }

    void ButtonClicked(TextView button) {
        if (wordTableInput.indexOfChild(button) == -1) {
            button.setEnabled(false);
            wordTableInput.addView(CreateButton(button.getText().toString()));

            if (wordTableInput.getChildCount() == p.words.size()) {
                boolean correctAnswer = true;
                for (int i=0; i<p.words.size(); i++)
                    if (((TextView)wordTableInput.getChildAt(i)).getText().equals(p.words.get(i)) == false) {
                        correctAnswer = false;
                        break;
                    }
                if (correctAnswer)
                    newRound();
            }

        } else {
            wordTableInput.removeView(button);
            final String buttonText = button.getText().toString();
            for (int i=0; i<wordTableOptions.getChildCount(); i++) {
                TextView child = (TextView) wordTableOptions.getChildAt(i);
                if (child.getText().toString().equals(buttonText) && child.isEnabled() == false) {
                    child.setEnabled(true);
                    break;
                }
            }
            // Note that a round can only end after the user clicks on one of the word options, thus adding it to the input box.
        }
    }

    void HintButtonClicked() {
        hintView.setText(p.sentence);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("p", p);

        outState.putBoolean("hintVisible", hintView.getText().length() != 0);

        String[] inputWords = new String[wordTableInput.getChildCount()];
        for (int i=0; i<wordTableInput.getChildCount(); i++)
            inputWords[i] = ((TextView)wordTableInput.getChildAt(i)).getText().toString();
        outState.putCharSequenceArray("inputWords", inputWords);

        String[] optionWords = new String[wordTableOptions.getChildCount()];
        boolean[] optionsEnabled = new boolean[wordTableOptions.getChildCount()];
        for (int i=0; i<wordTableOptions.getChildCount(); i++) {
            optionWords[i] = ((TextView) wordTableOptions.getChildAt(i)).getText().toString();
            optionsEnabled[i] = wordTableOptions.getChildAt(i).isEnabled();
        }
        outState.putCharSequenceArray("optionWords", optionWords);

        outState.putBooleanArray("optionsEnabled", optionsEnabled);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState == null) {
            newRound();
            return ;
        }

        p = (AudioAndWords) savedInstanceState.getSerializable("p");

        if (savedInstanceState.getBoolean("hintVisible"))
            HintButtonClicked();
        else
            hintView.setText("");

        CharSequence[] inputWords = savedInstanceState.getCharSequenceArray("inputWords");
        for (CharSequence inputWord : inputWords)
            wordTableInput.addView(CreateButton(inputWord.toString()));

        CharSequence[] optionWords = savedInstanceState.getCharSequenceArray("optionWords");
        boolean[] optionsEnabled = savedInstanceState.getBooleanArray("optionsEnabled");
        for (int i=0; i<optionWords.length; i++) {
            TextView button = CreateButton(optionWords[i].toString());
            button.setEnabled(optionsEnabled[i]);
            wordTableOptions.addView(button);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        app.stopPlayingAudio();
    }

    public void ReplayButtonClicked() {
        app.playAudio(p.audioPath);
    }
}
