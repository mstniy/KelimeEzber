package com.mstniy.kelimeezber;

import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import static java.lang.Math.exp;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class ExerciseFragment extends Fragment {

    static final String TAG = ExerciseFragment.class.getName();

    MyApplication app;
    boolean currentFwd;
    TextView label;
    Button buttons[] = new Button[4];

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_exercise, container, false);

        app = (MyApplication) getContext().getApplicationContext();

        label = rootView.findViewById(R.id.label);
        buttons[0] = rootView.findViewById(R.id.button0);
        buttons[1] = rootView.findViewById(R.id.button1);
        buttons[2] = rootView.findViewById(R.id.button2);
        buttons[3] = rootView.findViewById(R.id.button3);
        for (int i=0;i<4;i++)
            buttons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ButtonClicked((Button)v);
                }
            });
        app.currentPairIndex.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer index) {
                cpiChanged(index);
            }
        });

        return rootView;
    }

    void cpiChanged(Integer index) {
        currentFwd = new Random().nextBoolean();
        for (int i=0;i<4;i++)
            ChangeColorOfButton(buttons[i], false);
        final Pair p = app.wlist.get(index);
        final int answer=new Random().nextInt(4);
        currentFwd = new Random().nextBoolean();
        label.setText(currentFwd?p.first:p.second);
        for (int i=0;i<4;i++)
        {
            if (i == answer)
                buttons[i].setText(currentFwd ? p.second : p.first);
            else {
                final Pair p2 = app.wlist.get(new Random().nextInt(app.wlist.size()));
                buttons[i].setText(currentFwd?p2.second:p2.first);
            }
        }
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
            return app.wordTranslationsFwd.get(label.getText().toString()).contains(s);
        else
            return app.wordTranslationsBwd.get(label.getText().toString()).contains(s);
    }

    public void ButtonClicked(Button button)
    {
	    int buttonId;
	    if (button.getId() == R.id.button0) buttonId = 0;
        else if (button.getId() == R.id.button1) buttonId = 1;
        else if (button.getId() == R.id.button2) buttonId = 2;
        else if (button.getId() == R.id.button3) buttonId = 3;
        else
            return ;
        if (isACorrectAnswer(button.getText().toString()))
        {
            //cout << "Correct!" << endl;
		    final double oldScore = app.hardness.get(app.currentPairIndex.getValue());
            double newScore = oldScore;
            if (app.mistakeQueue[app.currentQueueIndex] == -1) // The user chose the correct answer at the first try
                newScore -= 0.33;
            else
                newScore += 1;
            newScore = min(newScore, 2.0);
            newScore = max(newScore, -1.33);
            app.hardness.set(app.currentPairIndex.getValue(), newScore); // Update the score of the current word
            app.NewRound();
        }
        else
        {
            //cout << "Incorrect!" << endl;
            app.mistakeQueue[app.currentQueueIndex]=app.currentPairIndex.getValue();
            for (int i=0;i<4;i++)
                if (isACorrectAnswer(buttons[i].getText().toString()))
                    ChangeColorOfButton(buttons[i], true);
        }
    }
}
