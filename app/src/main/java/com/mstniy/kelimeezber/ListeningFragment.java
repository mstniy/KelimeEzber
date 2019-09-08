package com.mstniy.kelimeezber;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;
import com.opencsv.CSVReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AudioAndWords {
    Uri audio;
    ArrayList<String> words;
    String sentence;
    AudioAndWords(Uri _audio, ArrayList<String> _words, String _sentence) {
        audio = _audio;
        words = _words;
        sentence = _sentence;
    }
}

public class ListeningFragment extends Fragment {
    final String TAG = getClass().getName();
    final int MINIMUM_SENTENCE_LENGTH_IN_WORDS = 4;

    MyApplication app;
    boolean created = false;
    Button hintButton;
    TextView hintView;
    FlexboxLayout wordTableInput, wordTableOptions;
    ArrayList<AudioAndWords> ats = new ArrayList<>();
    AudioAndWords p = null; // TODO: We need to persist this. Both for in-app transitions and Android app hibernations.

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
        wordTableInput = rootView.findViewById(R.id.word_box_input);
        wordTableOptions = rootView.findViewById(R.id.word_box_options);
        try {
            CSVReader reader = new CSVReader(new FileReader(app.audioDatasetPath + "/validated.tsv"), '\t');
            for (String[] line : reader) {
                String sentence = line[2];
                ArrayList<String> words = tokenizeWords(sentence);
                if (words.size() < MINIMUM_SENTENCE_LENGTH_IN_WORDS)
                    continue;
                Uri uri = Uri.parse(app.audioDatasetPath + "/clips/" + line[1]);
                ats.add(new AudioAndWords(uri, words, sentence));
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException("validated.tsv not found in the audio dataset path.");
        }
        created = true;
        newRound();
        return rootView;
    }

    void speak() {
        app.playAudio(p.audio);
    }

    ArrayList<String> tokenizeWords(String sentence) {
        sentence = sentence.replace(",", "");
        sentence = sentence.replace(".", "");
        sentence = sentence.replace("!", "");
        sentence = sentence.replace("?", "");
        sentence = sentence.replace(":", "");
        sentence = sentence.replace(";", "");
        final Pattern word_etractor = Pattern.compile("(^| )([^ ]+)");
        Matcher word_matcher = word_etractor.matcher(sentence);
        ArrayList<String> list = new ArrayList<>();
        while (word_matcher.find())
            list.add(word_matcher.group(2).toLowerCase());
        return list;
    }

    void newRound() {
        newRound(ats.get(new Random().nextInt(ats.size())));
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
        for (String word : shuffledWords) {
            TextView b = new TextView(getContext());
            b.setText(word);
            b.setBackgroundResource(android.R.drawable.btn_default);
            b.setTextSize(20.0f);
            b.setGravity(Gravity.CENTER);
            b.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    ButtonClicked((TextView) v);
                }
            });
            wordTableOptions.addView(b);
        }
        app.playAudio(p.audio);
    }

    void ButtonClicked(TextView button) {
        if (wordTableOptions.indexOfChild(button) != -1) {
            wordTableOptions.removeView(button);
            wordTableInput.addView(button);

            if (wordTableInput.getChildCount() == p.words.size()) {
                boolean correctAnswer = true;
                for (int i=0; i<p.words.size(); i++)
                    if (((TextView)wordTableInput.getChildAt(i)).getText().equals(p.words.get(i)) == false) {
                        correctAnswer = false;
                        break;
                    }
                if (correctAnswer)
                    app.FinishRound();
            }

        } else {
            wordTableInput.removeView(button);
            wordTableOptions.addView(button);
            // Note that a round can only end after the user clicks on one of the word options, thus adding it to the input box.
        }
    }

    void HintButtonClicked() {
        hintView.setText(p.sentence);
    }
}
