package com.mstniy.kelimeezber;

import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class AddLanguageActivity extends AppCompatActivity {
    Button addButton;
    MyApplication app;
    TextInputEditText edit_from, edit_to, edit_to_iso639, edit_to_iso3166;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (MyApplication) getApplicationContext();
        setContentView(R.layout.activity_add_language);
        addButton = findViewById(R.id.addButton);
        edit_from = findViewById(R.id.edit_from_lang);
        edit_to = findViewById(R.id.edit_to_lang);
        edit_to_iso639 = findViewById(R.id.edit_to_lang_iso639);
        edit_to_iso3166 = findViewById(R.id.edit_to_lang_iso3166);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddButtonClicked();
            }
        });
    }

    private void AddButtonClicked() {
        String from = edit_from.getText().toString();
        String to = edit_to.getText().toString();
        String to_iso639 = edit_to_iso639.getText().toString();
        String to_iso3166 = edit_to_iso3166.getText().toString();
        if (from.length() == 0 || to.length() == 0 || to_iso639.length() == 0 || to_iso3166.length() == 0)
            return ;
        LanguageDB ldb = new LanguageDB();
        ldb.from = from;
        ldb.to = to;
        ldb.to_iso639 = to_iso639;
        ldb.to_iso3166 = to_iso3166;
        ldb.dbPath = getExternalFilesDir(null).getPath() + '/' + app.dbs.size() + "_" + from.toLowerCase() + "_" + to.toLowerCase() + ".db";
        app.dbs.add(ldb);
        app.changeDB(ldb);
        finish();
    }
}
