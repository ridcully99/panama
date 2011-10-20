/*
 *  Copyright 2004-2010 Robert Brandner (robert.brandner@gmail.com) 
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License. 
 *  You may obtain a copy of the License at 
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *  
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License. 
 */
package panama.android.trackx;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * @author ridcully
 *
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String DISTANCE_UNIT_KEY = "distanceUnit";
	public static final String GENDER_KEY = "gender";
	public static final String WEIGHT_KEY = "weight";
	
	private ListPreference mDistanceUnitPreference;
	private ListPreference mGenderPreference;
	private EditTextPreference mWeightPreference;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		// Get a reference to the preferences
		mDistanceUnitPreference = (ListPreference)getPreferenceScreen().findPreference(DISTANCE_UNIT_KEY);
		mGenderPreference = (ListPreference)getPreferenceScreen().findPreference(GENDER_KEY);
		mWeightPreference = (EditTextPreference)getPreferenceScreen().findPreference(WEIGHT_KEY);

	}

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Setup the initial values
        String distanceUnit = getListLabel(R.array.prefs_unit_entries, R.array.prefs_unit_entryValues, sharedPreferences.getString(DISTANCE_UNIT_KEY, "km"));
        mDistanceUnitPreference.setSummary(distanceUnit);
        String gender = getListLabel(R.array.prefs_gender_entries, R.array.prefs_gender_entryValues, sharedPreferences.getString(GENDER_KEY, "0"));
        mGenderPreference.setSummary(gender); 
        mWeightPreference.setSummary(sharedPreferences.getString(WEIGHT_KEY, ""+Util.DEFAULT_WEIGHT)+" kg");
        
        // Set up a listener whenever a key changes            
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes            
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);    
    }
    
    private String getListLabel(int labelArrayResourceId, int valueArrayResourceId, String value) {
        String[] labels = getResources().getStringArray(labelArrayResourceId);
        String[] values = getResources().getStringArray(valueArrayResourceId);
        for (int i=0; i<values.length; i++) {
        	if (values[i].equals(value)) {
        		return labels[i];
        	}
        }
        return "";
    }
    
	// ------------------------------------------------ SharedPreferences.OnSharedPreferenceChangeListener Implementation
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (DISTANCE_UNIT_KEY.equals(key)) {
	        String distanceUnit = getListLabel(R.array.prefs_unit_entries, R.array.prefs_unit_entryValues, sharedPreferences.getString(DISTANCE_UNIT_KEY, "km"));
	        mDistanceUnitPreference.setSummary(distanceUnit);
		} else if (GENDER_KEY.equals(key)) {
	        String gender = getListLabel(R.array.prefs_gender_entries, R.array.prefs_gender_entryValues, sharedPreferences.getString(GENDER_KEY, "0"));
	        mGenderPreference.setSummary(gender); 
		} else if (WEIGHT_KEY.equals(key)) {
	        mWeightPreference.setSummary(sharedPreferences.getString(WEIGHT_KEY, ""+Util.DEFAULT_WEIGHT)+" kg");
		}
	}	
}
