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

import android.content.ContentValues;
import android.location.Location;

import com.google.android.maps.GeoPoint;

/**
 * Wrapper für Location + GeoPoint
 * @author ridcully
 *
 */
public class Position {

	public Location location;
	public GeoPoint geoPoint;
	public float distance; // to previous position
	
	public Position(Location location) {
		this.location = location;
		this.geoPoint = Util.locationToGeoPoint(location);
	}
	
	public String toString() {
		return location.getLatitude()+";"+location.getLongitude()+";"+location.getAccuracy()+";"+distance;
	}

	/** write our data to be saved to provided cv */
	public void applyTo(ContentValues cv) {
		cv.put(SessionPersistence.TIME, location.getTime());
		cv.put(SessionPersistence.LATITUDE, location.getLatitude());
		cv.put(SessionPersistence.LONGITUDE, location.getLongitude());
		cv.put(SessionPersistence.ALTITUDE, location.getAltitude());
		cv.put(SessionPersistence.ACCURACY, location.getAccuracy());
		cv.put(SessionPersistence.SPEED, location.getSpeed());
		cv.put(SessionPersistence.BEARING, location.getBearing());
		cv.put(SessionPersistence.PROVIDER, location.getProvider());
		cv.put(SessionPersistence.DISTANCE, distance);
	}
}
