package com.patrick.fisheyelabel;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContentResolverCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.patrick.fpilabel.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

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
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            SharedPreferences sp = getPreferenceManager().getSharedPreferences();

            // get recording method and set visibility of preferences
            // accordingly
            String mRecordingMethod = sp.getString("recording_method", "----");
            if(!mRecordingMethod.equals("----")) {
                setPreferenceVisibility(mRecordingMethod);
            } else {
                Log.e("Preference setup", "unknown recording method" + mRecordingMethod);
            }


            // Label file summary
            Preference labelList = (Preference) findPreference("label_file");
            labelList.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent().setType("text/plain").setAction(Intent.ACTION_OPEN_DOCUMENT);
                    i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(i, 7);
                    return true;
                }
            });

            String t[] = sp.getString("label_file", "").split("%2F");
            String fileName = "----";
            if(t.length > 1){
                fileName = t[t.length - 1];
            }
            labelList.setSummary(fileName);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            //Log.d("SettingsActivity", "Got result for request: " + requestCode + " with resultCode: " + resultCode);
            if(requestCode == 7 && resultCode == RESULT_OK) {

                Uri uri = data.getData();
                final int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
                getActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);

                SharedPreferences preferenceManager = getPreferenceManager().getSharedPreferences();
                SharedPreferences.Editor editor = preferenceManager.edit();
                Log.d("SettingsActivity", "onActivityResult: adding " + uri.toString() + " to preferences");
                editor.putString("label_file", uri.toString());
                editor.apply();
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            SharedPreferences sp = getPreferenceManager().getSharedPreferences();
            Log.i("onPreferenceChanged", "Preference with key: "+ s + " has changed");
            switch (s){
                case "label_file":
                    Preference p = findPreference(s);
                    // Need to use %2F as the split because the uri path encoding it as the directory marker
                    String t[] = sp.getString("label_file", "").split("%2F");
                    String fileName = "----";
                    if(t.length > 1){
                        fileName = t[t.length - 1];
                    }
                    p.setSummary(fileName);
                    sp.edit().putString("last_recording_value", "0").apply();
                    break;
                case "recording_method":
                    String method = sp.getString("recording_method", "----");
                    if(method != "----"){
                        sp.edit().putString("recording_method", method).apply();
                        setPreferenceVisibility(method);
                        sp.edit().putString("last_recording_value", "0").apply();
                    }
                    break;
                case "vines_per_bay":
                    sp.edit().putInt("vines_per_bay", sp.getInt("vines_per_bay", -1)).apply();
                    break;
                default:
            }
        }

        private void setPreferenceVisibility(String m){
            switch(m){
                case "FPI":
                    findPreference("vines_per_bay").setEnabled(true);
                    findPreference("label_file").setEnabled(false);
                    break;
                case "List":
                    findPreference("vines_per_bay").setEnabled(false);
                    findPreference("label_file").setEnabled(true);
                    break;
                case "Alpha-Numeric":
                    findPreference("vines_per_bay").setEnabled(false);
                    findPreference("label_file").setEnabled(false);
                    break;
                default:
                    Log.d("SettingsActivity", "What is this key: " + m);
                    break;
            }
        }
    }
}