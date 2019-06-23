package com.mstniy.kelimeezber;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

public class SortByDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.sortby_title)
                .setItems(R.array.sortByArray, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MyApplication app = (MyApplication) getContext().getApplicationContext();
                        app.sortByPeriod.setValue(which==1);
                    }
                });
        return builder.create();
    }
}
