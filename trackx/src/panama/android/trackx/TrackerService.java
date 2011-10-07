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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

/**
 * Track (and record) positions.
 * 
 * @author ridcully
 *
 */
public class TrackerService extends Service {

	public final static int MIN_DISTANCE = 5; 						   // in meters; 5 ist in MyTracks empfohlen das von Google-Leuten gemacht wird
	public final static int MIN_TIME = 1*Util.SECOND_IN_MILLIS;
	
	public final static int IDLE = 0;
	public final static int TRACKING = 1;
	public final static int RECORDING = 2;

	public final static int RECORDING_NOTIFICATION = 1;
	
	public Location currentLocation;
	private LocationManager mLocationMgr;
	private Timer mTimer;
	private IBinder mLocalBinder = new LocalBinder();
	private Set<Listener> mListeners = new HashSet<Listener>();
	
	public List<Position> positions = new ArrayList<Position>();
	public float pathLength;
	public long elapsedTimeMillis;
	public float currentPace;
	public boolean isTracking = false;
	public boolean isRecording = false;
	private long startTimeMillis;
	
	// create Notification for using while recording
	private Notification notification;
	
	@Override
	public void onCreate() {
		mLocationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// create Notification for using while recording
		notification = new Notification(R.drawable.icon, "text1", System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.contentIntent = pendingIntent;
	}
	
	/** do IPC communication with Activities here */
	@Override
	public IBinder onBind(Intent intent) {
		return mLocalBinder;
	}

	public void addListener(Listener listener) {
		Toast.makeText(this, "SRV:addListener", Toast.LENGTH_SHORT).show();
		mListeners.add(listener);
	}

	public void startRecording() {
		Toast.makeText(this, "SRV:startRecording", Toast.LENGTH_SHORT).show();
		reset();
		positions.add(new Position(currentLocation));
		mTimer = new Timer();
		mTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis;
				Toast.makeText(TrackerService.this, "SRV:sendTimerNotifications", Toast.LENGTH_SHORT).show();
			}
		}, 0, Util.SECOND_IN_MILLIS);
		notification.setLatestEventInfo(this, "notification-title", "notification-message", notification.contentIntent);
		startForeground(RECORDING_NOTIFICATION, notification);
		isRecording = true;
	}
	
	public void stopRecording() {
		Toast.makeText(this, "SRV:stopRecording", Toast.LENGTH_SHORT).show();
		mTimer.cancel();
		mTimer = null;
		mLocationMgr.removeUpdates(mLocationListener);
		stopForeground(true);
		isRecording = false;
	}
	
	public void startTracking() {
		currentLocation = getPreliminaryPosition();
		mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, 0, mLocationListener);
		isTracking = true;
	}
	
	public void stopTracking() {
		mLocationMgr.removeUpdates(mLocationListener);
		currentLocation = null;	// by setting to null, the marker will be no longer displayed on the map with the next refresh
		isTracking = false;
	}
	
	public void reset() {
		positions.clear();
		pathLength = 0;
		startTimeMillis = System.currentTimeMillis();
		elapsedTimeMillis = 0;
		currentPace = 0;
		for (Listener l : mListeners) {
			l.onLocationChanged(currentLocation);
			l.onTimerChanged(elapsedTimeMillis);
		}
	}
	
	/** set data from session (called after load) */
	public void setSession(Session session) {
		pathLength = session.distance;
		elapsedTimeMillis = session.time;
		positions = session.positions;
		// notify listeners
		for (Listener l : mListeners) {
			l.onLocationChanged(currentLocation);
			l.onTimerChanged(elapsedTimeMillis);
		}
	}	
	
	private Location getPreliminaryPosition() {
		Location coarseLocation = mLocationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location fineLocation = mLocationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location fallBackLocation = new Location(LocationManager.NETWORK_PROVIDER);
		fallBackLocation.setLatitude(0);
		fallBackLocation.setLongitude(0);
		fallBackLocation.setAccuracy(999);
		fallBackLocation.setTime(0);
		if (coarseLocation == null) {
			return fineLocation != null ? fineLocation : fallBackLocation;
		} else if (fineLocation == null) {
			return coarseLocation != null ? coarseLocation : fallBackLocation;
		} else {
			if (coarseLocation.getTime() > fineLocation.getTime()) {
				return coarseLocation;
			} else {
				return fineLocation;
			}
		}
	}	
	
	// -------------------------------------------------------------------------------------- LocalBinder
	
	public class LocalBinder extends Binder {	
		
		TrackerService getService() {
			return TrackerService.this;
		}
	}
	
	// ----------------------------------------------------------------------------------------- Listener
	
	/**
	 * Das Interface sollen alle die was wissen wollen implementieren und sich registrieren.
	 * Sie werden dann jeweils benachrichtigt (durch Aufruf von onLocationChanged()) und können
	 * sich weitere Infos nach Bedarf holen.
	 */
	public interface Listener {	
		
		public void onLocationChanged(Location location);
		
		public void onTimerChanged(long elapsedTimeMillis);
	}	
	
	// -------------------------------------------------------------------------------- LocationListener
	
	private LocationListener mLocationListener = new LocationListener() { /* nice trick with anonymous class */
		
		@Override
		public void onLocationChanged(Location location) {

			// FOR EMULATOR TESTING ONLY!!!
			Toast.makeText(TrackerService.this, "SRV:EMULATORTESTING MODE IS ACTIVE!!!", Toast.LENGTH_SHORT).show();
			if (location.hasAccuracy() == false) {
				location.setAccuracy(3);
				location.setTime(System.currentTimeMillis());
			}
			
			Toast.makeText(TrackerService.this, "SRV:newLocation", Toast.LENGTH_SHORT).show();
			
			if (!Util.isBetterLocation(currentLocation, location)) {
				Toast.makeText(TrackerService.this, "SRV:notBetter", Toast.LENGTH_SHORT).show();
				return;
			}
			
			if (isRecording) {
				float dist = location.distanceTo(currentLocation);
				if (dist < MIN_DISTANCE || dist < currentLocation.getAccuracy()+location.getAccuracy()) {
					Toast.makeText(TrackerService.this, "SRV:distTooSmall", Toast.LENGTH_SHORT).show();
					return;	// no adding to path _AND_ no update of current pos. (otherwise current pos would move away from path)
				}

				Position p = new Position(location);
				p.distance = dist;
				positions.add(p);
				pathLength += dist;
				currentPace = location.getSpeed();
			}
			currentLocation = location;
			// notify listeners
			Toast.makeText(TrackerService.this, "SRV:sendLocationNotification", Toast.LENGTH_SHORT).show();
			for (Listener l : mListeners) {
				l.onLocationChanged(location);
			}
		}
		
		@Override
		public void onProviderDisabled(String provider) {
		}
	
		@Override
		public void onProviderEnabled(String provider) {
		}
	
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};
}
