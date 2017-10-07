package com.josealonsomendozahotmail.controllerbot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;


public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SettingsFragment";

    public EditTextPreference editTextSocket;
    public EditTextPreference editTextVideo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        editTextSocket = (EditTextPreference) findPreference("socketServer");
        editTextVideo = (EditTextPreference) findPreference("videoServer");

        Log.d(TAG, "SettingsFragment initialized");
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key)
    {
       String data = "";
       switch (key) {
           case ("socketServer"):
               data = editTextSocket.getText();
               break;
           case ("videoServer"):
               data = editTextVideo.getText();
       }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, data);
        editor.apply();

        Log.d(TAG, key + " " + data);


    }
}