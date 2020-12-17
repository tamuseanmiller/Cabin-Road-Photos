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


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        //Get the content resolver
        cResolver = getActivity().getContentResolver();

        //Get the current window
        window = getActivity().getWindow();

        try {
            // To handle the auto
            Settings.System.putInt(cResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            //Get the current system brightness
            getPreferenceManager().findPreference("brightness").setDefaultValue(
                    Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS)
            );
        } catch (Settings.SettingNotFoundException e) {
            //Throw an error case it couldn't be retrieved
            Log.e("Error", "Cannot access system brightness");
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mDimming = (SwitchPreferenceCompat) getPreferenceManager().findPreference("preventDim");
        mBrightness = (SeekBarPreference) getPreferenceManager().findPreference("brightness");
        mAutoplay = (SwitchPreferenceCompat) getPreferenceManager().findPreference("autoplay");
        mAutoplaySpeed = (SeekBarPreference) getPreferenceManager().findPreference("autoplaySpeed");

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
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}