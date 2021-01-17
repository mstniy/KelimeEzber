package com.mstniy.kelimeezber;

import android.app.Dialog;
import android.arch.lifecycle.MutableLiveData;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog.Builder;

public class SortByDialog extends DialogFragment {
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new Builder(getActivity());
        builder.setTitle((int) R.string.sortby_title).setItems(R.array.sortByArray, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                MutableLiveData<Integer> mutableLiveData = ((MyApplication) getContext().getApplicationContext()).sortBy;
                mutableLiveData.setValue(which);
            }
        });
        return builder.create();
    }
}
