package com.mstniy.kelimeezber;

import android.content.Intent;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class MyViewHolder extends RecyclerView.ViewHolder {
    public TextView mTextView0, mTextView1;
    public Button removeButton;
    public Pair p;
    public MyViewHolder(View view, Pair _p, TextView _mTextView0, TextView _mTextView1, Button _removeButton) {
        super(view);
        p = _p;
        mTextView0 = _mTextView0;
        mTextView1 = _mTextView1;
        removeButton = _removeButton;
    }
}

class PairAndIndex {
    public Pair p;
    public int wlistIndex;

    public PairAndIndex(Pair _p, int _wlistIndex) {
        p = _p;
        wlistIndex = _wlistIndex;
    }
}

class RecycleViewAdapter extends RecyclerView.Adapter<MyViewHolder> {

    private static final String TAG = RecycleViewAdapter.class.getName();

    MyApplication app;
    private ArrayList<PairAndIndex> mDataset;
    private ArrayList<PairAndIndex> mDatasetUnfiltered;

    // Provide a suitable constructor (depends on the kind of dataset)
    public RecycleViewAdapter(MyApplication _app, ObservableArrayList<Pair> myDataset) {
        app = _app;
        setDataset(myDataset);
    }

    public void setDataset(ArrayList<Pair> newDataset) {
        mDataset = new ArrayList<>();
        for (int i=0; i<newDataset.size(); i++)
            mDataset.add(new PairAndIndex(newDataset.get(i), i));
        mDatasetUnfiltered = mDataset;
        Collections.sort(mDataset, new Comparator<PairAndIndex>() {
            @Override
            public int compare(PairAndIndex l, PairAndIndex r)
            {
                return SwedishLexicographicalComparator.compare(l.p.first, r.p.first)?-1:1;
            }
        });
    }

    public void filter(String text) {
        if(text.isEmpty()){
            mDataset = mDatasetUnfiltered;
        } else{
            mDataset = new ArrayList<>();
            text = text.toLowerCase();
            for(PairAndIndex p : mDatasetUnfiltered){
                if(p.p.first.toLowerCase().contains(text) || p.p.second.toLowerCase().contains(text)){
                    mDataset.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.word_list_item, parent, false);
        MyViewHolder vh = new MyViewHolder(
                v,
                null,
                (TextView)v.findViewById(R.id.textView0),
                (TextView)v.findViewById(R.id.textView1),
                (Button)v.findViewById(R.id.removeButton));
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.mTextView0.setText(mDataset.get(position).p.first);
        holder.mTextView1.setText(mDataset.get(position).p.second);
        holder.p = mDataset.get(position).p;

        holder.removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.RemovePair(mDataset.get(position).wlistIndex);
            }
        });
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
    RecycleViewAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;
    FloatingActionButton fab;
    SearchView searchView;

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

        mAdapter = new RecycleViewAdapter(app, app.wlist);
        mRecyclerView.setAdapter(mAdapter);

        app.wlist.addOnListChangedCallback(new ObservableList.OnListChangedCallback<ObservableList<Pair>>() {
            @Override
            public void onChanged(ObservableList<Pair> sender) {
                mAdapter.setDataset(app.wlist);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeChanged(ObservableList<Pair> sender, int positionStart, int itemCount) {
                mAdapter.setDataset(app.wlist);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeInserted(ObservableList<Pair> sender, int positionStart, int itemCount) {
                mAdapter.setDataset(app.wlist);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeMoved(ObservableList<Pair> sender, int fromPosition, int toPosition, int itemCount) {
                mAdapter.setDataset(app.wlist);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeRemoved(ObservableList<Pair> sender, int positionStart, int itemCount) {
                mAdapter.setDataset(app.wlist);
                mAdapter.notifyDataSetChanged();
            }
        });



        searchView = rootView.findViewById(R.id.action_search);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mAdapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.filter(newText);
                return true;
            }
        });

        return rootView;
    }
}
