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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * Track (and record) positions.
 * 
 * @author ridcully
 *
 */
public class TrackerService extends Service {

	private final static String TAG = "trackx-service";
	
	public final static int MIN_DISTANCE = 5; 						   // in meters; 5 ist in MyTracks empfohlen das von Google-Leuten gemacht wird
	public final static int MIN_TIME = 1*Util.SECOND_IN_MILLIS;
	
	public final static int IDLE = 0;
	public final static int TRACKING = 1;
	public final static int RECORDING = 2;

	public final static int FOREGROUND_WHILE_RECORDING = 1;
	
	public Location currentLocation;
	public Location preliminaryLocation;
	private LocationManager mLocationMgr;
	private TimerTask mTimer;
	private IBinder mLocalBinder = new LocalBinder();
	private Set<Listener> mListeners = new HashSet<Listener>();
	
	public List<Position> positions = new ArrayList<Position>();
	public float pathLength;
	public long sessionStartedAtMillis;
	public long elapsedTimeMillis;
	public float currentPace;
	public float averagePace;
	public boolean isTracking = false;
	public boolean isRecording = false;
	public boolean trackingFoundLocation = false;	// set true at first location we get after startTracking()
	
	// create Notification for using while recording
	private Notification mNotification;
	
	@Override
	public void onCreate() {
		mLocationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// create Notification for using while recording
		mNotification = new Notification(R.drawable.icon, getText(R.string.notification_recording_started), System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, MainActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		mNotification.contentIntent = pendingIntent;
	}
	
	/** do IPC communication with Activities here */
	@Override
	public IBinder onBind(Intent intent) {
		return mLocalBinder;
	}

	public void addListener(Listener listener) {
		mListeners.add(listener);
	}

	public void startRecording() {
		isRecording = true;
		resetQuietly();
		sessionStartedAtMillis = System.currentTimeMillis();
		for (Listener l : mListeners) {
			l.onRefreshAll();
		}
		positions.add(new Position(currentLocation));
		mNotification.setLatestEventInfo(this, getText(R.string.notification_recording), getText(R.string.notification_text), mNotification.contentIntent);
		startForeground(FOREGROUND_WHILE_RECORDING, mNotification);
		mTimer = new TimerTask();
		mTimer.execute(0L);
	}
	
	public void stopRecording() {
		isRecording = false;
		mTimer.cancel(true);
		mTimer = null;
		stopForeground(true);
	}
	
	/** returns preliminary location at start */
	public Location startTracking() {
		trackingFoundLocation = false;
		currentLocation = getPreliminaryPosition();
		mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, 0, mLocationListener);
		isTracking = true;
		return currentLocation;
	}
	
	public void stopTracking() {
		Log.d(TAG, "stopTracking begin");
		mLocationMgr.removeUpdates(mLocationListener);
		currentLocation = null;	// by setting to null, the marker will be no longer displayed on the map with the next refresh
		isTracking = false;
		Log.d(TAG, "stopTracking done");
	}
	
	public void reset() {
		resetQuietly();
		for (Listener l : mListeners) {
			l.onRefreshAll();
		}
	}
	
	private void resetQuietly() {
		positions.clear();
		pathLength = 0;
		sessionStartedAtMillis = 0;
		elapsedTimeMillis = 0;
		currentPace = 0;
		averagePace = 0;
	}
	
	/** use data from session (called after load) */
	public void applySession(Session session) {
		pathLength = session.distance;
		sessionStartedAtMillis = session.timestamp;
		elapsedTimeMillis = session.time;
		averagePace = (elapsedTimeMillis/Util.SECOND_IN_MILLIS) > 0 ? pathLength/(elapsedTimeMillis/Util.SECOND_IN_MILLIS) : 0;
		positions = session.positions;
		currentLocation = null;
		// notify listeners
		for (Listener l : mListeners) {
			l.onRefreshAll();
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
		
		public void onPaceChanged(float currentPace, float averagePace);
		
		public void onRefreshAll();
	}	
	
	// -------------------------------------------------------------------------------- LocationListener
	
	private LocationListener mLocationListener = new LocationListener() { /* nice trick with anonymous class */
		
		@Override
		public void onLocationChanged(Location location) {

//			// FOR EMULATOR TESTING ONLY!!!
//			if (location.hasAccuracy() == false) {
//				Log.d(TAG, "setting accuracy to 15 for location having no accuracy!");
//				location.setAccuracy(15);
//				location.setTime(System.currentTimeMillis());
//			}
			
			Log.d(TAG, "onLocationChanged. tracking/recording="+TrackerService.this.isTracking+","+TrackerService.this.isRecording);
			
			if (!Util.isBetterLocation(currentLocation, location)) {
				return;
			}
			
			trackingFoundLocation = true;
			
			if (TrackerService.this.isRecording) {
				// always notify about pace (to get speed 0 too, even if we do not actually record the position due to too less distance...)
				currentPace = location.getSpeed();
				averagePace = (elapsedTimeMillis/Util.SECOND_IN_MILLIS) > 0 ? pathLength/(elapsedTimeMillis/Util.SECOND_IN_MILLIS) : 0;
				for (Listener l : mListeners) {
					l.onPaceChanged(currentPace, averagePace);
				}
				float dist = location.distanceTo(currentLocation);
				if (dist < MIN_DISTANCE || dist < /*currentLocation.getAccuracy()+*/location.getAccuracy()) {
					return;	// no adding to path _AND_ no update of current pos. (otherwise current pos would move away from path)
				}

				Position p = new Position(location);
				p.distance = dist;
				positions.add(p);
				pathLength += dist;
			}
			currentLocation = location;
			// notify listeners
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
	
	// -------------------------------------------------------------------------------- TimerTask

	/**
	 * Stoppuhr, läuft parallel und postet akt. Zeit schön formatiert.
	 * TODO Berechnet gleichzeitig auch die aktuelle Geschwindigkeit und postet sie ebenso sch�n formatiert
	 * 
	 * @author ridcully
	 */
	class TimerTask extends AsyncTask<Long, Long, String> {
		/**
		 * params[0] == initial value in millis (0 at first start, but can be > 0 after stop/start ...)
		 */
		@Override
		protected String doInBackground(Long... params) {
			Log.d(TAG, "TimerTask.doInBackground begin");
			long startMillis = System.currentTimeMillis();
			while (TrackerService.this.isRecording) {
				long millis = System.currentTimeMillis() - startMillis;
				publishProgress(millis);
				try {
					Thread.sleep(500);	// nur 1/2 Sekunde, damit ich sicher keine Sekunde verpasse
				} catch (InterruptedException e) {
				}
			}
			Log.d(TAG, "TimerTask.doInBackground done");
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Long... values) {
			// notify listeners
			for (Listener l : mListeners) {
				elapsedTimeMillis = values[0];
				l.onTimerChanged(elapsedTimeMillis);
			}
		}
	}	
}
