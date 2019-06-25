package com.mstniy.kelimeezber;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

public class ExerciseTypeDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.exercisetype_title)
                .setItems(R.array.exerciseTypeArray, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MyApplication app = (MyApplication) getContext().getApplicationContext();
                        if (app.exerciseType != which) {
                            app.exerciseType = which;
                            app.StartRound();
                        }
                    }
                });
        return builder.create();
    }
}
