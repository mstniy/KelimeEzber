package com.mstniy.kelimeezber;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog.Builder;

import java.util.ArrayList;

public class SelectLanguageDialog extends DialogFragment {
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new Builder(getActivity());
        final MyApplication app = (MyApplication) getContext().getApplicationContext();
        final ArrayList<LanguageDB> dbs = app.dbs;
        CharSequence[] items = new CharSequence[dbs.size()+1];
        items[0] = "Create New";
        for (int i=0; i<dbs.size(); i++)
            items[i+1] = dbs.get(i).from + " -> " + dbs.get(i).to;
        builder.setTitle(R.string.selectlang_title).setItems(items, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    startActivity(new Intent(getContext(), AddLanguageActivity.class));
                } else {
                    if (app.currentDB != dbs.get(which-1))
                        app.changeDB(dbs.get(which-1));
                }
            }
        });
        return builder.create();
    }
}
