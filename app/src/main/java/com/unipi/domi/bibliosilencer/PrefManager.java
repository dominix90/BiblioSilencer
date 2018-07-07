package com.unipi.domi.bibliosilencer;


import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;

    // shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "bibliosilencer-pref";
    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";
    private static final String IS_AUDIO_ON = "IsAudioOn";
    private static final String AVERAGE_SOUNDS = "averageSounds";

    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeLaunch() {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

    public void setAudio(boolean audioOn) {
        editor.putBoolean(IS_AUDIO_ON, audioOn);
        editor.commit();
    }

    public boolean isAudioOn() {
        return pref.getBoolean(IS_AUDIO_ON, true);
    }

    public void saveAverageSoundList(String soundString) {
        editor.putString(AVERAGE_SOUNDS, soundString);
        editor.commit();
    }

    public String getAverageSoundList() {
        return pref.getString(AVERAGE_SOUNDS, "");
    }

}
