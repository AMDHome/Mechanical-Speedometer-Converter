package com.ecs193.speedometerconverter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.content.Intent;
import android.widget.EditText;
import com.ecs193.speedometerconverter.R;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.Fragment;

public class BottomBarActivity extends AppCompatActivity {

    private static final String TAG_FRAGMENT_CALLS = "tag_frag_calls";
    private static final String TAG_FRAGMENT_RECENTS = "tag_frag_recents";
    private static final String TAG_FRAGMENT_TRIPS = "tag_frag_trips";

    private BottomNavigationView bottomNavigationView;

    /**
     * Maintains a list of Fragments for {@link BottomNavigationView}
     */
    private List<BottomBarFragment> fragments = new ArrayList<>(3);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottom_bar);

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_nav);

        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.bottombar_bluetooth:
                                switchFragment(0, TAG_FRAGMENT_CALLS);

                                //Intent intent = new Intent(BottomBarActivity.this, BluetoothActivity.class);
                                //startActivity(intent);
                                //Intent intent = new Intent(BottomBarActivity.this, BluetoothActivity.class);
                                //EditText editText = (EditText) findViewById(R.id.editText);
                                //String message = editText.getText().toString();
                                //intent.putExtra(EXTRA_MESSAGE, message);
                                //startActivity(intent);
                                return true;
                            case R.id.bottombar_settings:
                                switchFragment(1, TAG_FRAGMENT_RECENTS);
                                return true;
                            case R.id.bottombar_data:
                                switchFragment(2, TAG_FRAGMENT_TRIPS);
                                return true;
                        }
                        return false;
                    }
                });

        buildFragmentsList();

        // Set the 0th Fragment to be displayed by default.
        switchFragment(0, TAG_FRAGMENT_CALLS);

    }

    private void switchFragment(int pos, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_fragmentholder, fragments.get(pos), tag)
                .commit();
    }


    private void buildFragmentsList() {
        BottomBarFragment callsFragment = buildFragment("Bluetooth");
        BottomBarFragment recentsFragment = buildFragment("Settings");
        BottomBarFragment tripsFragment = buildFragment("Data");

        fragments.add(callsFragment);
        fragments.add(recentsFragment);
        fragments.add(tripsFragment);
    }

    /**
     * Creates a {@link BottomBarFragment} with corresponding Item title.
     *
     * @param title
     * @return
     */
    private BottomBarFragment buildFragment(String title) {
        BottomBarFragment fragment = new BottomBarFragment();
        Bundle bundle = new Bundle();
        bundle.putString(BottomBarFragment.ARG_TITLE, title);
        fragment.setArguments(bundle);
        return fragment;
    }

    /** Called when the user taps the Send button */
    /*public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }*/
}
