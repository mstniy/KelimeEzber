package com.mstniy.kelimeezber;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

public class DrawerActivity extends AppCompatActivity {
    private final String TAG = getClass().getName();
    MyApplication app;
    ExerciseFragment exerciseFragment;
    WordListFragment listFragment;
    NavigationView navView;
    StatsFragment statsFragment;

    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof ExerciseFragment) {
            exerciseFragment = (ExerciseFragment) fragment;
        } else if (fragment instanceof WordListFragment) {
            listFragment = (WordListFragment) fragment;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabbed);
        app = (MyApplication) getApplicationContext();
        exerciseFragment = new ExerciseFragment();
        listFragment = new WordListFragment();
        statsFragment = new StatsFragment();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(true);
        navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(new OnNavigationItemSelectedListener() {
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                Fragment fragment;
                FragmentManager fragmentManager = getSupportFragmentManager();
                switch (menuItem.getItemId()) {
                    case R.id.drawer_list:
                        fragment = listFragment;
                        break;
                    case R.id.drawer_stats:
                        fragment = statsFragment;
                        break;
                    default:
                        fragment = exerciseFragment;
                        break;
                }
                fragmentManager.beginTransaction().replace(R.id.main_fragment_frame, fragment).commit();
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment_frame, exerciseFragment).commit();
            navView.setCheckedItem(R.id.drawer_exercise);
        }
    }
}
