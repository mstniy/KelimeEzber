package com.mstniy.kelimeezber;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class MyViewHolder extends RecyclerView.ViewHolder {
    public TextView mTextView0, mTextView1, mTextViewPeriod;
    public Button removeButton;
    public MyViewHolder(View view, TextView _mTextView0, TextView _mTextView1, TextView _mTextViewPeriod, Button _removeButton) {
        super(view);
        mTextView0 = _mTextView0;
        mTextView1 = _mTextView1;
        mTextViewPeriod = _mTextViewPeriod;
        removeButton = _removeButton;
    }
}

class RecycleViewAdapter extends RecyclerView.Adapter<MyViewHolder> {

    private static final String TAG = RecycleViewAdapter.class.getName();

    MyApplication app;
    private Pattern filterPattern = null;
    private ArrayList<Pair> mDataset;
    private HashSet<Pair> mDatasetUnfiltered;

    // Provide a suitable constructor (depends on the kind of dataset)
    public RecycleViewAdapter(MyApplication _app, HashSet<Pair> myDataset, LifecycleOwner lifecycleOwner) {
        app = _app;
        filterPattern = Pattern.compile("");
        setDataset(myDataset);
        app.sortByPeriod.observe(lifecycleOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean sortByHardness) {
                sortMDataset();
                notifyDataSetChanged();
            }
        });
    }

    private void sortMDataset() {
        if (app.sortByPeriod.getValue() == false) {
            Collections.sort(mDataset, new Comparator<Pair>() {
                @Override
                public int compare(Pair l, Pair r) {
                    return SwedishLexicographicalComparator.compare(l.first, r.first);
                }
            });
        }
        else {
            Collections.sort(mDataset, new Comparator<Pair>() {
                @Override
                public int compare(Pair l, Pair r) {
                    if (l.period == 0 && r.period > 0)
                        return 1;
                    if (r.period == 0 && l.period > 0)
                        return -1;
                    if (l.period > r.period)
                        return 1;
                    else if (r.period > l.period)
                        return -1;
                    return 0;
                }
            });
        }
    }

    public void setDataset(HashSet<Pair> newDataset) {
        mDatasetUnfiltered = newDataset;
        filter();
    }

    void setFilterPattern(String pattern) {
        try {
            filterPattern = Pattern.compile(pattern);
        }
        catch (PatternSyntaxException e) {
            filterPattern = null;
        }
        filter();
    }

    public void filter() {
        mDataset = new ArrayList<>();
        if (filterPattern != null) {
            for (Pair p : mDatasetUnfiltered) {
                if (filterPattern.matcher(p.first).find() ||
                        filterPattern.matcher(p.second).find())
                    mDataset.add(p);
            }
        }
        sortMDataset();
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
                (TextView)v.findViewById(R.id.textView0),
                (TextView)v.findViewById(R.id.textView1),
                (TextView)v.findViewById(R.id.text_view_period),
                (Button)v.findViewById(R.id.removeButton));
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.mTextView0.setText(mDataset.get(position).first);
        holder.mTextView1.setText(mDataset.get(position).second);
        int period = mDataset.get(position).period;
        holder.mTextViewPeriod.setText((period>0)?String.valueOf(period):"\u221E"); // \u221E is the infinity symbol

        holder.removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.RemovePair(mDataset.get(position));
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

    final static String TAG = WordListFragment.class.getName();

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
        setHasOptionsMenu(true);
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

        mAdapter = new RecycleViewAdapter(app, app.wlist, this);
        mRecyclerView.setAdapter(mAdapter);

        app.wlistObservers.add(new Runnable() {
            @Override
            public void run() {
                mAdapter.setDataset(app.wlist);
            }
        });

        searchView = rootView.findViewById(R.id.action_search);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mAdapter.setFilterPattern(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.setFilterPattern(newText);
                return true;
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_wordlist, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_sort) {
            new SortByDialog().show(getFragmentManager(), "sortby");
            return true;
        }
        else
            return super.onOptionsItemSelected(item);
    }
}
