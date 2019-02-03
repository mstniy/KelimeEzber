package com.mstniy.kelimeezber;

import android.content.Intent;
import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import static java.lang.Math.exp;
import static java.lang.Math.max;
import static java.lang.Math.min;

class Pair
{
    public String first;
    public String second;
    public Pair(String _first, String _second) {
        first = _first;
        second = _second;
    }
};

public class MainActivity extends AppCompatActivity {

    static final String TAG = MainActivity.class.getName();

    //ArrayList<Integer> uncoveredPairs = new ArrayList<Integer>();
    ArrayList<Pair> wlist;
    HashMap<String, HashSet<String>> wordTranslationsFwd;
    HashMap<String, HashSet<String>> wordTranslationsBwd;
    ArrayList<Double> hardness; // A hardness score for each pair in wlist
    TextView label;
    Button buttons[] = new Button[4];
    FloatingActionButton fab;
    int currentPairIndex;
    boolean currentFwd;
    final int MistakeQueueLength=4;
    int mistakeQueue[]=new int[MistakeQueueLength];
    int currentQueueIndex=MistakeQueueLength-1; // This doesn't really matter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        label = findViewById(R.id.label);
        buttons[0] = findViewById(R.id.button0);
        buttons[1] = findViewById(R.id.button1);
        buttons[2] = findViewById(R.id.button2);
        buttons[3] = findViewById(R.id.button3);
        fab = findViewById(R.id.fab0);
        for (int i=0;i<4;i++)
            buttons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ButtonClicked((Button)v);
                }
            });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FabClicked((FloatingActionButton)v);
            }
        });
        GetWords();
        NewRound();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GetWords(); // TODO: We don't have to reload the db each time the activity is resumed. It's enough that we reload it if the db was modified (by AddWordActivity).
        NewRound();
    }

    /*bool AddToWordList(ifstream& in)
    {
        bool faultyLine=false;
        for (int i=1; ;i++)
        {
            string line;
            getline(in, line);
            if (in.fail())
                break;
            auto pos = line.find("=");

            //regex word_regex("(.+?)\\s*=\\s*(.+)");
            regex word_regex("^([^#]+?)\\s*=\\s*([^#]+?)\\s*(#|$)");
            auto words_begin = std::sregex_iterator(line.begin(), line.end(), word_regex);
            auto words_end = std::sregex_iterator();
            if (words_begin == words_end)
            {
                cout << "Line " << i << " invalid (no match found)" << endl;
                faultyLine=true;
                continue;
            }
            std::smatch match = *words_begin;
            assert(match.size() == 4);
            wlist.push_back(make_pair(match.str(1), match.str(2)));
            uncoveredPairs.push_back(wlist.size()-1);
            hardness.push_back(0);
        }
        return faultyLine==false;
    }*/

    void AddPairToMaps(Pair p){
        if (wordTranslationsFwd.containsKey(p.first) == false)
            wordTranslationsFwd.put(p.first, new HashSet<String>());
        if (wordTranslationsBwd.containsKey(p.second) == false)
            wordTranslationsBwd.put(p.second, new HashSet<String>());
        wordTranslationsFwd.get(p.first).add(p.second);
        wordTranslationsBwd.get(p.second).add(p.first);
    }


    boolean GetWords()
    {
        wlist = new ArrayList<Pair>();
        wordTranslationsFwd = new HashMap<String, HashSet<String>>();
        wordTranslationsBwd = new HashMap<String, HashSet<String>>();
        hardness = new ArrayList<Double>();
        for (int i=0;i<MistakeQueueLength;i++)
            mistakeQueue[i] = -1;

        DatabaseHelper helper = new DatabaseHelper(this);
        Pair[] pairs = helper.getPairs();
        if (pairs.length == 0) { // We just created the db
            pairs = new Pair[5];
            pairs[0] = new Pair("sedan", "since");
            pairs[1] = new Pair("annars", "otherwise");
            pairs[2] = new Pair("även om", "even if");
            pairs[3] = new Pair("snygg", "nice");
            pairs[4] = new Pair("trevlig", "nice");
        }
        for (Pair p : pairs)
        {
            wlist.add(p);
            AddPairToMaps(p);
            //uncoveredPairs.add(uncoveredPairs.size());
            hardness.add(0.0);
        }
        return true;
    }

    int GetRandomPairIndex()
    {
        /*if (uncoveredPairs.size() > 0) // Choose an uncovered pair
        {
            final int uncoveredPairsIndex = new Random().nextInt(uncoveredPairs.size());
            final int index = uncoveredPairs.get(uncoveredPairsIndex);
            //swap(uncoveredPairs[uncoveredPairsIndex], uncoveredPairs[uncoveredPairs.size()-1]);
            final int tmp = uncoveredPairs.get(uncoveredPairsIndex);
            uncoveredPairs.set(uncoveredPairsIndex, uncoveredPairs.get(uncoveredPairs.size()-1));
            uncoveredPairs.set(uncoveredPairs.size()-1, tmp);
            uncoveredPairs.remove(uncoveredPairs.size()-1);
            return index;
        }*/
        double hardnessSum = 0;
        for (double h : hardness)
            hardnessSum += exp(h);
        if (hardnessSum == 0)
            return 0;
        double rnd = new Random().nextDouble() * hardnessSum;
        for (int i=0; i<hardness.size(); i++)
        {
            if (exp(hardness.get(i))>rnd)
                return i;
            rnd -= exp(hardness.get(i));
        }
        return hardness.size()-1; // Mathematically, this cannot happen. But we're dealing with floats, so who knows.
    }

    void ChangeColorOfButton(Button button, boolean highlight)
    {
        if (highlight)
            button.setBackgroundColor(Color.rgb(100, 255, 100));
        else
            button.setBackgroundResource(android.R.drawable.btn_default);
    }

    void NewRound()
    {
        //cout << (wlist.size()-uncoveredPairs.size()) << "/" << wlist.size() << " pairs covered." << endl;
        for (int i=0;i<4;i++)
            ChangeColorOfButton(buttons[i], false);
        currentQueueIndex=(currentQueueIndex+1)%MistakeQueueLength;
        if (mistakeQueue[currentQueueIndex] != -1)
            currentPairIndex=mistakeQueue[currentQueueIndex];
        else
            currentPairIndex = GetRandomPairIndex();
        mistakeQueue[currentQueueIndex]=-1;
	    final Pair p = wlist.get(currentPairIndex);
	    final int answer=new Random().nextInt(4);
        currentFwd = new Random().nextBoolean();
        label.setText(currentFwd?p.first:p.second);
        for (int i=0;i<4;i++)
        {
            if (i == answer)
                buttons[i].setText(currentFwd ? p.second : p.first);
		    else {
			    final Pair p2 = wlist.get(new Random().nextInt(wlist.size()));
                buttons[i].setText(currentFwd?p2.second:p2.first);
            }
        }
    }

    boolean isACorrectAnswer(String s) {
        if (currentFwd)
            return wordTranslationsFwd.get(label.getText().toString()).contains(s);
        else
            return wordTranslationsBwd.get(label.getText().toString()).contains(s);
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
		    final double oldScore = hardness.get(currentPairIndex);
            double newScore = oldScore;
            if (mistakeQueue[currentQueueIndex] == -1) // The user chose the correct answer at the first try
                newScore -= 0.33;
            else
                newScore += 1;
            newScore = min(newScore, 2.0);
            newScore = max(newScore, -1.33);
            hardness.set(currentPairIndex, newScore); // Update the score of the current word
            NewRound();
        }
        else
        {
            //cout << "Incorrect!" << endl;
            mistakeQueue[currentQueueIndex]=currentPairIndex;
            for (int i=0;i<4;i++)
                if (isACorrectAnswer(buttons[i].getText().toString()))
                    ChangeColorOfButton(buttons[i], true);
        }
    }

    public void FabClicked(FloatingActionButton fab) {
        startActivity(new Intent(this, AddWordActivity.class));
    }
}
