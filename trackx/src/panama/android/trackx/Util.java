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

import java.text.SimpleDateFormat;

import android.location.Location;
import android.location.LocationManager;
import android.util.FloatMath;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

/**
 * @author ridcully
 *
 */
public class Util {

	public final static int SECOND_IN_MILLIS = 1000;
	public final static int MAX_STARTOK_FIX_ACCURACY = 100;
	public final static long MAX_STARTOK_FIX_AGE = 30 * SECOND_IN_MILLIS;
	
	// calories calulation
	public final static int GENDER_MALE = 0;
	public final static int GENDER_FEMALE = 1;
	public final static int GENDER_UNKNOWN = 2;
	private final static float RESTING_COMPONENT[] = new float[] {3.5f, 3.2f, 3.35f};	// used GENDER_... as index
	private final static float MIN_RUNNING_SPEED_IN_M_PER_MINUTE = 5.9545728f*1000f/60f;
	
	public final static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd.MMM.yyyy HH:mm");	// TODO externalize and make I18n
	
	/**
	 * Location -> GeoPoint
	 * @param location not null
	 * @return
	 */
	public static GeoPoint locationToGeoPoint(Location location) {
		GeoPoint point = new GeoPoint((int)(location.getLatitude()*1E6), (int)(location.getLongitude()*1E6));
		return point;
	}

	/**
	 * Meters to pixel-distance radius for specified latitude.
	 * From http://www.anddev.org/viewtopic.php?p=16075 (via http://stackoverflow.com/questions/2077054/how-to-compute-a-radius-around-a-point-in-an-android-mapview)
	 * WARNING: Division by Zero für latitude == +/-90 (am Nord- und Südpol)
	 * @param meters
	 * @param map
	 * @param latitude
	 * @return
	 */
	public static int metersToRadius(float meters, MapView map, double latitude) {
		return (int) (map.getProjection().metersToEquatorPixels(meters) * (1/FloatMath.cos((float)Math.toRadians(latitude))));
	}	
	
	public static String formatTime(long millis) {
		long secs = millis / 1000;	// sekundengenau reicht
		long mins = secs / 60;
		secs -= mins * 60;
		long hours = mins / 60;
		mins -= hours * 60;
		if (hours > 0) {
			return String.format("%dh%02d'%02d''", hours, mins, secs);
		} else {
			return String.format("%02d'%02d''", mins, secs);
		}
	}
	
	public static String formatSpeed(float metersPerSecond) {
		float kmh = (metersPerSecond * 60 * 60)/1000f;
		return String.format("%.1f", kmh);
	}

	public static String formatDistance(float meters) {
		return String.format("%.2f", meters/1000f);
	}	
	
	public static CharSequence formatDate(long timestampMillis) {
		return dateFormat.format(timestampMillis);
	}
	
	/**
	 * 
	 * @param gender GENDER_MALE, GENDER_FEMALE or GENDER_UNKNOWN
	 * @param weight in kg
	 * @param timeMillis
	 * @param meters
	 * @param grade Steigung in % ... 0.02 == 2%
	 * @return kcal
	 */
	public static int calculateCalories(int gender, float weight, long timeMillis, float meters, float grade) {
		if (timeMillis == 0 || meters == 0) {
			return 0;
		}
		if (gender < 0 || gender > 2) {
			gender = 2;
		}
		float speed = meters/(timeMillis/SECOND_IN_MILLIS/60); // in meter/minute
		float oxygen;
		if (speed < MIN_RUNNING_SPEED_IN_M_PER_MINUTE) {
			oxygen = (0.1f * speed) + (1.8f * speed * grade) + RESTING_COMPONENT[gender];
		} else {
			oxygen = (0.2f * speed) + (0.9f * speed * grade) + RESTING_COMPONENT[gender];
		}
		float kcalPerMinute = (oxygen * 4.9f) / 1000f;
		float kcal = kcalPerMinute * weight * ((float)timeMillis)/((float)(SECOND_IN_MILLIS*60));
		return (int)kcal;
	}
	
	/** check if location is OK for starting session */
	public static boolean isOKforStart(Location location) {
		boolean ok = location != null &&
				LocationManager.GPS_PROVIDER.equals(location.getProvider()) &&
				((location.hasAccuracy() && location.getAccuracy() <= MAX_STARTOK_FIX_ACCURACY) || (!location.hasAccuracy())) &&
				(System.currentTimeMillis() - location.getTime()) <= MAX_STARTOK_FIX_AGE;
		if (!ok) {
			Log.d(MainActivity.TAG, String.format("location not OK for start: provider=%s, accuracy=%f meters, age=%d secs", location.getProvider(), location.getAccuracy(), (int)(System.currentTimeMillis() - location.getTime())/1000));
		}
		return ok;
	}
	
	/** 
	 * check if candidate is a better location than reference, based on age and accuracy.
	 * 
	 * based on http://developer.android.com/guide/topics/location/obtaining-user-location.html
	 */
	public static boolean isBetterLocation(Location reference, Location candidate) {
		if (reference == null) {
			return true;
		}
		long deltaTime = candidate.getTime() - reference.getTime();
		if (deltaTime > 30*SECOND_IN_MILLIS) {	// location is significantly newer than current location --> accept new one.
			return true;
		}
		if (!candidate.hasAccuracy()) {
			return false;
		}
		boolean isNewer = deltaTime >= 3 * SECOND_IN_MILLIS;	// 3+ seconds newer
		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (candidate.getAccuracy() - reference.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(candidate.getProvider(), reference.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (!isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;	
	}
	
	/** Checks whether two providers are the same */
	private static boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
		return provider2 == null;
		}
		return provider1.equals(provider2);
	}
}
