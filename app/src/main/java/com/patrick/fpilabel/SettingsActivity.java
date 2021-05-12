package com.patrick.fpilabel;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContentResolverCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            SharedPreferences.Editor e = getPreferenceManager().getSharedPreferences().edit();
            e.putString("label_file", "----");
            e.commit();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference labelList = (Preference) findPreference("label_file");
            labelList.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent().setType("text/plain").setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(i, 7);
                    return true;
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            //Log.d("SettingsActivity", "Got result for request: " + requestCode + " with resultCode: " + resultCode);
            if(requestCode == 7 && resultCode == RESULT_OK) {

                Uri uri = data.getData();

                SharedPreferences preferenceManager = getPreferenceManager().getSharedPreferences();
                SharedPreferences.Editor editor = preferenceManager.edit();
                Log.d("SettingsActivity", "onActivityResult: adding " + uri.getPath() + " to preferences");
                editor.putString("label_file", uri.getPath());
                editor.commit();
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            SharedPreferences sp = getPreferenceManager().getSharedPreferences();
            Log.i("onPreferenceChanged", "Preference with key: "+ s + " has changed");
            switch (s){
                case "label_file":
                    Preference p = findPreference(s);
                    String t[] = sp.getString("label_file", "").split("/");
                    String fileName = "----";
                    if(t.length > 1){
                        fileName = t[t.length - 1];
                    }
                    p.setSummary(fileName);
                    break;
                case "recording_method":
                    String method = sp.getString("recording_method", "----");
                    if(method != "----"){
                        sp.edit().putString("recording_method", method).commit();
                        switch(method){
                            case "FPI":
                                findPreference("vines_per_bay").setEnabled(true);
                                findPreference("label_file").setEnabled(false);
                                break;
                            case "List":
                                findPreference("vines_per_bay").setEnabled(false);
                                findPreference("label_file").setEnabled(true);
                                break;
                            case "Numeric":
                                findPreference("vines_per_bay").setEnabled(false);
                                findPreference("label_file").setEnabled(false);
                                break;
                            default:
                                Log.d("SettingsActivity", "What is this key: " + method);
                                break;
                        }
                    }
                    break;
                case "vines_per_bay":
                    sp.edit().putInt("vines_per_bay", sp.getInt("vines_per_bay", -1)).commit();
                    break;
                default:
            }
        }
    }
}