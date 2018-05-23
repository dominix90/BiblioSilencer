package com.unipi.domi.bibliosilencer;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class AudioSettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio);
        addPreferencesFromResource(R.xml.preferences);
    }
}
