package com.mstniy.kelimeezber;

import android.support.design.widget.TextInputEditText;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
        for (Pair p : ((MyApplication)getApplication()).wlist)
            if (p.first.equals(first) && p.second.equals(second)) {
                Toast.makeText(this, "This pair already exists!",
                        Toast.LENGTH_SHORT).show();
                return ;
            }
        ((MyApplication)getApplicationContext()).AddPair(new Pair(0, first, second, 0, 1)); // AddPair will set proper id
        finish();
    }
}
