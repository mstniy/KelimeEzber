package com.mstniy.kelimeezber;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog.Builder;
import android.widget.Toast;

public class SelectionMethodDialog extends DialogFragment {
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new Builder(getActivity());
        builder.setTitle(R.string.selectionmethod_title).setItems(R.array.exerciseTypeArray, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                MyApplication app = (MyApplication) getContext().getApplicationContext();
                SelectionMethod sm = SelectionMethod.SMART;
                if (which == 0)
                    sm = SelectionMethod.SMART;
                else if (which == 1)
                    sm = SelectionMethod.NEW;
                else if (which == 2)
                    sm = SelectionMethod.RANDOM;
                if (app.exerciseFragment.selectionMethod != sm) {
                    app.exerciseFragment.selectionMethod = sm;
                    app.exerciseFragment.StartRound(true);
                }
            }
        });
        return builder.create();
    }
}
