package com.josealonsomendozahotmail.controllerbot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class SetPreferenceActivity extends AppCompatActivity {

    final String TAG = "SetPreferenceActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        // Add new title to the activity
        setTitle("Settings");

        Log.d(TAG, "Changed title of the activity");

        Log.d(TAG, "Created SetPreferenceActivity");
    }


}
