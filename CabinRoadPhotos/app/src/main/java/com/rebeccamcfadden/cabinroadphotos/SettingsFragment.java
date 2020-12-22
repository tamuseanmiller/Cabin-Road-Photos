package com.rebeccamcfadden.cabinroadphotos;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.*;

import java.net.URISyntaxException;

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
    private String oldTitle;
    private Toolbar actionbar;
    private boolean hasNavigation;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        preferencesManager = new SharedPreferencesManager(getContext());
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        //Get the content resolver
        cResolver = requireActivity().getContentResolver();
        //Get the current window
        window = requireActivity().getWindow();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = super.onCreateView(inflater, container, savedInstanceState);

        actionbar = requireActivity().findViewById(R.id.toolbar_main);
        if (actionbar != null) {
            Log.d("debug", "action bar was non null");
            if (actionbar.getNavigationIcon() == null) {
                hasNavigation = false;
            } else {
                hasNavigation = true;
            }
            Drawable drawable = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_arrow_back);
            drawable.setTint(ContextCompat.getColor(getActivity(), R.color.white));
            actionbar.setNavigationIcon(drawable);
            actionbar.setTitle("Application Settings");
            actionbar.setNavigationOnClickListener(v -> {
                actionbar.setNavigationIcon(null);
                getActivity().onBackPressed();
            });
            oldTitle = actionbar.getTitle().toString();
            actionbar.setTitle("Settings");
        }

        mDimming = getPreferenceManager().findPreference("preventDim");
        mDimming.setDefaultValue(preferencesManager.retrieveBoolean("preventDim", false));
        mBrightness = getPreferenceManager().findPreference("brightness");
        try {
            mBrightness.setValue(Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS));
        } catch (Settings.SettingNotFoundException e) {
            Log.e("Error", "Cannot access system brightness");
            e.printStackTrace();
        }
        mAutoplay = getPreferenceManager().findPreference("autoplay");
        mAutoplay.setDefaultValue(preferencesManager.retrieveBoolean("autoplay", false));
        mAutoplaySpeed = getPreferenceManager().findPreference("autoplaySpeed");
        mAutoplaySpeed.setDefaultValue(preferencesManager.retrieveInt("autoplaySpeed", 20));

//         Check to see if we have write settings
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && !Settings.System.canWrite(getActivity())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + requireActivity().getPackageName()));
            startActivity(intent);
        }

        mBrightness.setOnPreferenceChangeListener((preference, newValue) -> {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1 || Settings.System.canWrite(getActivity())) {
                Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, (Integer) newValue);
                // Get the current window attributes
                WindowManager.LayoutParams layoutpars = window.getAttributes();
                // Set the brightness of this window
                layoutpars.screenBrightness = ((Integer) newValue) / (float) 255;
                // Apply attribute changes to this window
                window.setAttributes(layoutpars);
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + requireActivity().getPackageName()));
                startActivity(intent);
            }
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
        return mView;
    }

    @Override
    public void onDetach() {
        actionbar.setTitle(oldTitle);
        if (!hasNavigation) {
            actionbar.setNavigationIcon(null);
        }
        super.onDetach();
    }
}