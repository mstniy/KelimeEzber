package com.mstniy.kelimeezber;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class MyViewHolder extends RecyclerView.ViewHolder {
    public View rootView;
    public TextView mTextView0;
    public TextView mTextView1;
    public TextView mTextViewPeriod;
    public Button removeButton;

    public MyViewHolder(View view, TextView _mTextView0, TextView _mTextView1, TextView _mTextViewPeriod, Button _removeButton) {
        super(view);
        rootView = view;
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

    public RecycleViewAdapter(MyApplication _app, HashSet<Pair> myDataset, LifecycleOwner lifecycleOwner) {
        app = _app;
        filterPattern = Pattern.compile("");
        setDataset(myDataset);
        app.sortByPeriod.observe(lifecycleOwner, new Observer<Boolean>() {
            public void onChanged(@Nullable Boolean sortByPeriod) {
                sortMDataset();
                notifyDataSetChanged();
            }
        });
    }

    private void sortMDataset() {
        if (!(app.sortByPeriod.getValue()).booleanValue()) {
            Collections.sort(mDataset, new Comparator<Pair>() {
                public int compare(Pair l, Pair r) {
                    return SwedishLexicographicalComparator.compare(l.first, r.first);
                }
            });
        } else {
            Collections.sort(mDataset, new Comparator<Pair>() {
                public int compare(Pair l, Pair r) {
                    if (l.period == 0 && r.period > 0) {
                        return 1;
                    }
                    if (r.period == 0 && l.period > 0) {
                        return -1;
                    }
                    if (l.period > r.period) {
                        return 1;
                    }
                    if (r.period > l.period) {
                        return -1;
                    }
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
        } catch (PatternSyntaxException e) {
            filterPattern = null;
        }
        filter();
    }

    public void filter() {
        mDataset = new ArrayList<>();
        if (filterPattern != null) {
            Iterator it = mDatasetUnfiltered.iterator();
            while (it.hasNext()) {
                Pair p = (Pair) it.next();
                if (filterPattern.matcher(p.first).find() || filterPattern.matcher(p.second).find()) {
                    mDataset.add(p);
                }
            }
        }
        sortMDataset();
        notifyDataSetChanged();
    }

    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.word_list_item, parent, false);
        MyViewHolder myViewHolder = new MyViewHolder(v, (TextView) v.findViewById(R.id.textView0), (TextView) v.findViewById(R.id.textView1), (TextView) v.findViewById(R.id.text_view_period), (Button) v.findViewById(R.id.removeButton));
        return myViewHolder;
    }

    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final Pair p = mDataset.get(position);
        holder.mTextView0.setText(p.first);
        holder.mTextView1.setText(p.second);
        int period = p.period;
        holder.mTextViewPeriod.setText(period > 0 ? String.valueOf(period) : "∞");
        holder.removeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                RecycleViewAdapter.this.app.RemovePair(p);
                RecycleViewAdapter recycleViewAdapter = RecycleViewAdapter.this;
                recycleViewAdapter.setDataset(recycleViewAdapter.app.wlist);
            }
        });
        holder.rootView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                app.forceSpeak(p.first);
            }
        });
    }

    public int getItemCount() {
        return mDataset.size();
    }
}

public class WordListFragment extends Fragment {
    static final String TAG = WordListFragment.class.getName();
    private final int ADDWORD_REQUEST_CODE = 1;
    MyApplication app;
    FloatingActionButton fab;
    RecycleViewAdapter mAdapter;
    LayoutManager mLayoutManager;
    RecyclerView mRecyclerView;
    SearchView searchView;

    public void onAttach(Context context) {
        super.onAttach(context);
        app = (MyApplication) context.getApplicationContext();
        app.wordListFragment = this;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        app.wordListFragment = null;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.fragment_word_list, container, false);
        app = (MyApplication) getActivity().getApplicationContext();
        fab = rootView.findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(WordListFragment.this.getContext(), AddWordActivity.class), ADDWORD_REQUEST_CODE);
            }
        });
        mRecyclerView = rootView.findViewById(R.id.word_list);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new RecycleViewAdapter(app, app.wlist, this);
        mRecyclerView.setAdapter(mAdapter);
        searchView = rootView.findViewById(R.id.action_search);
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            public boolean onQueryTextSubmit(String query) {
                mAdapter.setFilterPattern(query);
                return true;
            }

            public boolean onQueryTextChange(String newText) {
                mAdapter.setFilterPattern(newText);
                return true;
            }
        });
        return rootView;
    }

    void dbChanged() {
        mAdapter.setDataset(app.wlist);
        searchView.setQuery("", false);
    }

    @Override
    public void onResume() {
        super.onResume();
        SoftKeyboardHelper.showSoftKeyboard(getActivity());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADDWORD_REQUEST_CODE) {
            mAdapter.setDataset(app.wlist);
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_wordlist, menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_selectlang) {
            new SelectLanguageDialog().show(getFragmentManager(), "selectlang");
            return true;
        }
        else if (item.getItemId() == R.id.action_sort) {
            new SortByDialog().show(getFragmentManager(), "sortby");
            return true;
        }
        else
            return super.onOptionsItemSelected(item);
    }
}
