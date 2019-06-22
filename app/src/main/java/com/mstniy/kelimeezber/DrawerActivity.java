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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabbed);

        app = (MyApplication) getApplicationContext();

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
                        fragment = new ExerciseFragment();
                        break;
                    case R.id.drawer_new:
                        fragment = new ExerciseFragment();
                        break;
                    case R.id.drawer_random:
                        fragment = new ExerciseFragment();
                        break;
                    case R.id.drawer_list:
                        fragment = new WordListFragment();
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_sort) {
            DialogFragment sortbyDialog = new SortByDialog();
            sortbyDialog.show(getSupportFragmentManager(), "sortby");
            return true;
        }
        else
            return super.onOptionsItemSelected(item);
    }
}
