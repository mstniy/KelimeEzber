package com.mstniy.kelimeezber;

import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class AddWordFragment extends Fragment {

    Button addButton;
    TextInputEditText edit0, edit1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_add_word, container, false);
        addButton = rootView.findViewById(R.id.addButton);
        edit0 = rootView.findViewById(R.id.edit0);
        edit1 = rootView.findViewById(R.id.edit1);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddButtonClicked();
            }
        });

        return rootView;
    }

    void AddButtonClicked() {
        String first = edit0.getText().toString();
        String second = edit1.getText().toString();
        if (first.length() == 0 || second.length() == 0)
            return ;
        DatabaseHelper helper = new DatabaseHelper(getContext());
        helper.insertPair(new Pair(first, second));
        edit0.setText("");
        edit1.setText("");
    }
}
