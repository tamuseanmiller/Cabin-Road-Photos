package com.rebeccamcfadden.cabinroadphotos;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;
import androidx.preference.*;

import static android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS;

public class SettingsFragment extends PreferenceFragmentCompat {
    private SwitchPreferenceCompat mDimming;
    private SeekBarPreference mBrightness;
    private SwitchPreferenceCompat mAutoplay;
    private SeekBarPreference mAutoplaySpeed;
    //Content resolver used as a handle to the system's settings
    private ContentResolver cResolver;
    //Window object, that will store a reference to the current window
    private Window window;
    private SharedPreferencesManager preferencesManager;


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        preferencesManager = new SharedPreferencesManager(getContext());
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        //Get the content resolver
        cResolver = getActivity().getContentResolver();
        //Get the current window
        window = getActivity().getWindow();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mDimming = (SwitchPreferenceCompat) getPreferenceManager().findPreference("preventDim");
        mDimming.setDefaultValue(preferencesManager.retrieveBoolean("preventDim", false));
        mBrightness = (SeekBarPreference) getPreferenceManager().findPreference("brightness");
        try {
            mBrightness.setDefaultValue(Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS));
        } catch (Settings.SettingNotFoundException e) {
            Log.e("Error", "Cannot access system brightness");
            e.printStackTrace();
        }
        mAutoplay = (SwitchPreferenceCompat) getPreferenceManager().findPreference("autoplay");
        mAutoplay.setDefaultValue(preferencesManager.retrieveBoolean("autoplay", false));
        mAutoplaySpeed = (SeekBarPreference) getPreferenceManager().findPreference("autoplaySpeed");
        mAutoplaySpeed.setDefaultValue(preferencesManager.retrieveInt("autoplaySpeed", 20));

        mBrightness.setOnPreferenceChangeListener((preference, newValue) -> {
                Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, (Integer) newValue);
                //Get the current window attributes
                WindowManager.LayoutParams layoutpars = window.getAttributes();
                //Set the brightness of this window
                layoutpars.screenBrightness = ((Integer) newValue) / (float)255;
                //Apply attribute changes to this window
                window.setAttributes(layoutpars);
                return true;
        });
        mAutoplaySpeed.setOnPreferenceChangeListener((preference, newValue) -> {
            preferencesManager.storeInt("autoplaySpeed", (Integer) newValue);
            return true;
        });
        mAutoplay.setOnPreferenceChangeListener((preference, newValue) -> {
            preferencesManager.storeBoolean("autoplay", (boolean) newValue);
            return true;
        });
        mDimming.setOnPreferenceChangeListener((preference, newValue) -> {
            preferencesManager.storeBoolean("preventDim", (boolean) newValue);
            return true;
        });
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}