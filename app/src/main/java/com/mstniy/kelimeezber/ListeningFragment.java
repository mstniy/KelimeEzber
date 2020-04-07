package com.mstniy.kelimeezber;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

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
    Button replayButton;
    Button buttons[] = new Button[4];
    boolean buttonsHighlighted[] = new boolean[4];
    AudioAndWords p = null;

    public void onAttach(Context context) {
        super.onAttach(context);
        app = (MyApplication) context.getApplicationContext();
        app.listeningFragment = this;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) throws RuntimeException{
        app = (MyApplication) getContext().getApplicationContext();
        View rootView = inflater.inflate(R.layout.fragment_listening, container, false);
        replayButton = rootView.findViewById(R.id.replay_button);
        replayButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ReplayButtonClicked();
            }
        });
        buttons[0] = rootView.findViewById(R.id.option1);
        buttons[1] = rootView.findViewById(R.id.option2);
        buttons[2] = rootView.findViewById(R.id.option3);
        buttons[3] = rootView.findViewById(R.id.option4);
        for (int i=0; i<4; i++)
            buttons[i].setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ButtonClicked((Button)v);
                }
            });
        created = true;
        return rootView;
    }

    void newRound() {
        newRound(app.ats.get(new Random().nextInt(app.ats.size())));
    }

    void newRound(AudioAndWords _p) {
        if (created == false)
            return ;
        for (int i = 0; i < 4; i++)
            ChangeColorOfButton(i, false);
        p = _p;
        if (p == null)
            return ;
        for (int i=0; i<4; i++)
            buttons[i].setText(app.ats.get(new Random().nextInt(app.ats.size())).sentence);
        int correctAnswer = new Random().nextInt(4);
        buttons[correctAnswer].setText(p.sentence);
        app.playAudio(p.audioPath);
    }

    private void ChangeColorOfButton(int buttonIndex, boolean highlight) {
        Button button = buttons[buttonIndex];
        if (highlight)
            button.setBackgroundColor(Color.rgb(100, 255, 100));
        else
            button.setBackgroundResource(android.R.drawable.btn_default);
        buttonsHighlighted[buttonIndex] = highlight;
    }

    void ButtonClicked(Button button) {
        if (button.getText().equals(p.sentence)) {
            newRound();
        }
        else {
            for (int i=0; i<4; i++) // Highlight the correct answer
                if (buttons[i].getText().equals(p.sentence))
                    ChangeColorOfButton(i, true);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("p", p);

        CharSequence[] buttonTexts = new CharSequence[4];
        for (int i=0;i<4;i++)
            buttonTexts[i] = buttons[i].getText();
        outState.putCharSequenceArray("buttonTexts", buttonTexts);

        outState.putBooleanArray("buttonsHighlighted", buttonsHighlighted);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState == null) {
            newRound();
            return ;
        }

        p = (AudioAndWords) savedInstanceState.getSerializable("p");

        CharSequence[] buttonTexts = savedInstanceState.getCharSequenceArray("buttonTexts");
        for (int i=0; i<4; i++)
            buttons[i].setText(buttonTexts[i]);

        boolean[] buttonsHighlighted = savedInstanceState.getBooleanArray("buttonsHighlighted");
        for (int i=0; i<4; i++)
            ChangeColorOfButton(i, buttonsHighlighted[i]);
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
