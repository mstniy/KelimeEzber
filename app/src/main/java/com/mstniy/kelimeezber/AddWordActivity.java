package com.mstniy.kelimeezber;

import android.support.design.widget.TextInputEditText;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class AddWordActivity extends AppCompatActivity {

    Button addButton;
    TextInputEditText edit0, edit1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_word);
        addButton = findViewById(R.id.addButton);
        edit0 = findViewById(R.id.edit0);
        edit1 = findViewById(R.id.edit1);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddButtonClicked();
            }
        });
    }

    void AddButtonClicked() {
        String first = edit0.getText().toString();
        String second = edit1.getText().toString();
        if (first.length() == 0 || second.length() == 0)
            return ;
        ((MyApplication)getApplicationContext()).AddPair(new Pair(first, second));
        finish();
    }
}
