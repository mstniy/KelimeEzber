package com.mstniy.kelimeezber;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog.Builder;

public class SelectionTypeDialog extends DialogFragment {
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new Builder(getActivity());
        builder.setTitle(R.string.selectiontype_title).setItems(R.array.exerciseTypeArray, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                MyApplication app = (MyApplication) getContext().getApplicationContext();
                if (app.selectionType != which) {
                    app.selectionType = which;
                    app.StartRound();
                }
            }
        });
        return builder.create();
    }
}
