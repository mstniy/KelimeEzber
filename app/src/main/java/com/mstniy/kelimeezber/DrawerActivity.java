package com.mstniy.kelimeezber;

import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class DrawerActivity extends AppCompatActivity {

    MyApplication app;

    ExerciseFragment exerciseFragment;
    WordListFragment listFragment;
    StatsFragment statsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabbed);

        app = (MyApplication) getApplicationContext();

        exerciseFragment = new ExerciseFragment();
        listFragment = new WordListFragment();
        statsFragment = new StatsFragment();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(true);

        NavigationView navView = (NavigationView)findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                Fragment fragment;
                FragmentManager fragmentManager = getSupportFragmentManager(); // For AppCompat use getSupportFragmentManager
                switch(menuItem.getItemId()) {
                    default:
                    case R.id.drawer_exercise:
                        fragment = exerciseFragment;
                        break;
                    case R.id.drawer_list:
                        fragment = listFragment;
                        break;
                    case R.id.drawer_stats:
                        fragment = statsFragment;
                        break;
                }
                fragmentManager.beginTransaction()
                        .replace(R.id.main_fragment_frame, fragment)
                        .commit();
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment_frame, new ExerciseFragment()).commit();
            navView.setCheckedItem(R.id.drawer_exercise);
        }
    }
}
