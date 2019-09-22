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
    NavigationView navView;
    Fragment.SavedState exerciseFragmentSavedState;
    Fragment.SavedState listeningFragmentSavedState;


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
        app.drawerActivity = this;
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
                Fragment newFragment = null;
                FragmentManager fragmentManager = getSupportFragmentManager();
                Fragment currentFragment = fragmentManager.findFragmentById(R.id.main_fragment_frame);
                switch (menuItem.getItemId()) {
                    case R.id.drawer_exercise:
                        if (currentFragment instanceof ExerciseFragment)
                            break;
                        newFragment = Fragment.instantiate(DrawerActivity.this, ExerciseFragment.class.getName());
                        break;
                    case R.id.drawer_listening:
                        if (currentFragment instanceof ListeningFragment)
                            break;
                        app.isMuted = false;
                        newFragment = Fragment.instantiate(DrawerActivity.this, ListeningFragment.class.getName());
                        break;
                    case R.id.drawer_list:
                        if (currentFragment instanceof WordListFragment)
                            break;
                        newFragment = Fragment.instantiate(DrawerActivity.this, WordListFragment.class.getName());
                        break;
                    case R.id.drawer_stats:
                        if (currentFragment instanceof StatsFragment)
                            break;
                        newFragment = Fragment.instantiate(DrawerActivity.this, StatsFragment.class.getName());
                        break;
                }
                if (newFragment != null) { // User chose a fragment which is not the one that is already on the screen
                    if (currentFragment instanceof ExerciseFragment)
                        exerciseFragmentSavedState = fragmentManager.saveFragmentInstanceState(currentFragment);
                    else if (currentFragment instanceof ListeningFragment)
                        listeningFragmentSavedState = fragmentManager.saveFragmentInstanceState(currentFragment);
                    if (newFragment instanceof  ExerciseFragment)
                        newFragment.setInitialSavedState(exerciseFragmentSavedState);
                    else if (newFragment instanceof  ListeningFragment)
                        newFragment.setInitialSavedState(listeningFragmentSavedState);
                    fragmentManager.beginTransaction().replace(R.id.main_fragment_frame, newFragment).commit();
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
        if (savedInstanceState == null) {
            ExerciseFragment exerciseFragment = (ExerciseFragment) Fragment.instantiate(this, ExerciseFragment.class.getName());
            getSupportFragmentManager().beginTransaction().add(R.id.main_fragment_frame, exerciseFragment).commit();
            getSupportFragmentManager().executePendingTransactions();
            navView.setCheckedItem(R.id.drawer_exercise);
        }
        getExternalStoragePermission(this);
    }

    void discardExerciseFragmentState() {
        exerciseFragmentSavedState = null;
    }
}
