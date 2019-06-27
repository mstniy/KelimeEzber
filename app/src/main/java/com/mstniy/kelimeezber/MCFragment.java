package com.mstniy.kelimeezber;

import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Random;


public class MCFragment extends Fragment {

    final String TAG = getClass().getName();

    MyApplication app;
    TextView label;
    Button buttons[] = new Button[4];
    // If this is false, the user has failed the current exercise (for example, clicked a wrong answer for a multiple choice exercise or requested a hint for a writing exercise)
    // Set to true at the beginning of each round.
    boolean isPass;
    WeakReference<ExerciseFragment> parent;

    public MCFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        app = (MyApplication) getContext().getApplicationContext();
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_mc, container, false);

        label = rootView.findViewById(R.id.label);
        buttons[0] = rootView.findViewById(R.id.button0);
        buttons[1] = rootView.findViewById(R.id.button1);
        buttons[2] = rootView.findViewById(R.id.button2);
        buttons[3] = rootView.findViewById(R.id.button3);
        for (int i=0;i<4;i++) {
            buttons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    buttonClicked((Button) v);
                }
            });
        }

        return rootView;
    }

    void newRound(Pair p) {
        isPass = true;
        //Clear the view first (we have the old exercise on it)
        for (int i=0;i<4;i++)
            ChangeColorOfButton(buttons[i], false);
        label.setText("");
        for (int i=0; i<4; i++)
            buttons[i].setText("");

        final int answer=new Random().nextInt(4);
        label.setText(app.currentFwd?p.first:p.second);
        for (int i=0;i<4;i++)
        {
            if (i == answer)
                buttons[i].setText(app.currentFwd ? p.second : p.first);
            else {
                final Pair p2 = PairChooser.ChoosePairRandom(app);
                buttons[i].setText(app.currentFwd?p2.second:p2.first);
            }
        }

        app.speak(p.first);
    }

    void ChangeColorOfButton(Button button, boolean highlight) {
        if (highlight)
            button.setBackgroundColor(Color.rgb(100, 255, 100));
        else
            button.setBackgroundResource(android.R.drawable.btn_default);
    }

    boolean isACorrectAnswer(String s) {
        if (app.currentFwd)
            return app.wordTranslationsFwd.get(app.currentPair.getValue().first.toString()).contains(s);
        else
            return app.wordTranslationsBwd.get(app.currentPair.getValue().second.toString()).contains(s);
    }

    void buttonClicked(Button button)
    {
        final Pair currentPair = app.currentPair.getValue();
        if (currentPair == null)
            return ;
        if (isACorrectAnswer(button.getText().toString()))
            app.FinishRound(true, isPass);
        else
        {
            isPass = false;
            for (int i=0;i<4;i++)
                if (isACorrectAnswer(buttons[i].getText().toString()))
                    ChangeColorOfButton(buttons[i], true);
        }
    }
}
