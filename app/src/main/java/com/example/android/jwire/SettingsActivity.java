package com.example.android.jwire;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }


    public static class JWirePreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_main);

            //Now we tell it to show the summary of the preference so that the user can see what
            //the current value of number of results
            Preference numberOfResults = findPreference(getString(R.string.settings_number_of_results_key));
            bindPreferenceSummaryToValue(numberOfResults);
            //Current value of search terms
            Preference searchTerms = findPreference(getString(R.string.settings_search_terms_key));
            bindPreferenceSummaryToValue(searchTerms);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            //Refresh MainActivity
            Intent refresh = new Intent(getActivity(), MainActivity.class);
            startActivity(refresh);
            getActivity().finish();
        }

        private void bindPreferenceSummaryToValue(Preference preference) {
            preference.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) this);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
            String preferenceString = preferences.getString(preference.getKey(), "");
            onPreferenceChange(preference, preferenceString);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            try {
                String stringValue = value.toString();
                preference.setSummary(stringValue);
            } catch (Exception e) {
                return false;
            }
            return true;
        }
    }
}
