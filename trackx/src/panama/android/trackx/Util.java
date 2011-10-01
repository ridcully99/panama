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

import android.location.Location;
import android.util.FloatMath;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

/**
 * @author ridcully
 *
 */
public class Util {

	public final static long UP_TO_DATE_MILLIS = 60 * 1000;	// 60 Sekunden gelten als Up To Date
	
	/**
	 * Check if location is up to date
	 * @param location may be null
	 * @return
	 */
	public static boolean isUpToDate(Location location) {
		return location != null && (System.currentTimeMillis() - location.getTime()) <= UP_TO_DATE_MILLIS;
	}

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
	 * WARNING: Division für latitude == +/-90 (am Nord- und Südpol)
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
			return String.format("%d:%02d:%02d", hours, mins, secs);
		} else {
			return String.format("%02d:%02d", mins, secs);
		}
	}
	
	public static String formatSpeed(float metersPerSecond) {
		float kmh = (metersPerSecond * 60 * 60)/1000f;
		return String.format("%.2fkm/h", kmh);
	}
}
