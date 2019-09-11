package com.mstniy.kelimeezber;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
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
    StatsFragment statsFragment;
    NavigationView navView;
    Fragment.SavedState exerciseFragmentSavedState;

    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof ExerciseFragment) {
            exerciseFragment = (ExerciseFragment) fragment;
        } else if (fragment instanceof WordListFragment) {
            listFragment = (WordListFragment) fragment;
        } else if (fragment instanceof StatsFragment) {
            statsFragment = (StatsFragment) fragment;
        }
    }

    public static boolean getExternalStoragePermission(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0);
            return false;
        }

        return true; // We return true even if the user has denied the permission so that a SecurityException will be thrown later on.
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);
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
                Fragment fragment = null;
                FragmentManager fragmentManager = getSupportFragmentManager();
                switch (menuItem.getItemId()) {
                    case R.id.drawer_list:
                        fragment = listFragment;
                        break;
                    case R.id.drawer_stats:
                        fragment = statsFragment;
                        break;
                    case R.id.drawer_exercise:
                        fragment = exerciseFragment;
                        break;
                }
                if (fragment.isAdded() == false) { // User chose a fragment which is not the one that is already on the screen
                    if (exerciseFragment.isAdded()) // Save the state of the exercise fragment, if it was the active one.
                        exerciseFragmentSavedState = fragmentManager.saveFragmentInstanceState(exerciseFragment); // TODO: Why not use put/setFragment here, like we do in ExerciseFragment?
                    if (menuItem.getItemId() == R.id.drawer_exercise) // If the user chose the exercise drawer, restore its state.
                        exerciseFragment.setInitialSavedState(exerciseFragmentSavedState); // TODO: DrawerActivity doesn't override onSaveInstanceState, but somehow still does restore the state of the subfragments after rotations. How does that work?  Does onNagivationItemSelected get called somehow cna restore the fragment state using the exerciseFragmentSavedState member field?
                    fragmentManager.beginTransaction().replace(R.id.main_fragment_frame, fragment).commit();
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment_frame, exerciseFragment).commit();
            navView.setCheckedItem(R.id.drawer_exercise);
        }
        getExternalStoragePermission(this);
    }
}
