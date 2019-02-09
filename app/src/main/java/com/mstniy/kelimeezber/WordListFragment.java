package com.mstniy.kelimeezber;

import android.content.Intent;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private static final String TAG = MyAdapter.class.getName();

    ObservableArrayList<Pair> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView0, mTextView1;
        public Button removeButton;
        public MyViewHolder(View view, TextView _mTextView0, TextView _mTextView1, Button _removeButton) {
            super(view);
            mTextView0 = _mTextView0;
            mTextView1 = _mTextView1;
            removeButton = _removeButton;
            //TODO: Here, we also need to set the onClickListener of the removeButton.
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAdapter(ObservableArrayList<Pair> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.word_list_item, parent, false);
        MyViewHolder vh = new MyViewHolder(
                v,
                (TextView)v.findViewById(R.id.textView0),
                (TextView)v.findViewById(R.id.textView1),
                (Button)v.findViewById(R.id.removeButton));
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.mTextView0.setText(mDataset.get(position).first);
        holder.mTextView1.setText(mDataset.get(position).second);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}

public class WordListFragment extends Fragment {

    MyApplication app;
    RecyclerView mRecyclerView;
    MyAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;
    FloatingActionButton fab;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_word_list, container, false);

        app = ((MyApplication)getActivity().getApplicationContext());

        fab = (FloatingActionButton) rootView.findViewById(R.id.floatingActionButton);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), AddWordActivity.class);
                startActivity(intent);
            }
        });

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.word_list);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new MyAdapter(app.wlist);
        mRecyclerView.setAdapter(mAdapter);

        app.wlist.addOnListChangedCallback(new ObservableList.OnListChangedCallback<ObservableList<Pair>>() {
            @Override
            public void onChanged(ObservableList<Pair> sender) {
                mAdapter.mDataset = app.wlist;
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeChanged(ObservableList<Pair> sender, int positionStart, int itemCount) {
                mAdapter.mDataset = app.wlist;
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeInserted(ObservableList<Pair> sender, int positionStart, int itemCount) {
                mAdapter.mDataset = app.wlist;
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeMoved(ObservableList<Pair> sender, int fromPosition, int toPosition, int itemCount) {
                mAdapter.mDataset = app.wlist;
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeRemoved(ObservableList<Pair> sender, int positionStart, int itemCount) {
                mAdapter.mDataset = app.wlist;
                mAdapter.notifyDataSetChanged();
            }
        });

        return rootView;
    }
}
