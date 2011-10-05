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

/**
 * Track (and record) positions.
 * 
 * @author ridcully
 *
 */
public class TrackerService extends Service {

	public final static int SECOND_IN_MILLIS = 1000;
	public final static int MIN_DISTANCE = 5; 						   // in meters; 5 ist in MyTracks empfohlen das von Google-Leuten gemacht wird
	public final static int MIN_TIME = 1*SECOND_IN_MILLIS;
	
	public final static int IDLE = 0;
	public final static int PAUSED = 1;
	public final static int RUNNING = 2;
	
	public List<Position> positions = new ArrayList<Position>();
	public Location currentLocation;
	public float pathLength;
	public long timeMillis;
	public float currentPace;
	public int sessionState = IDLE;	// IDLE, PAUSED, RUNNING
	private LocationManager mLocationMgr;
	private TimerTask mTimerTask;
	private IBinder mLocalBinder = new LocalBinder();
	private Set<Listener> mListeners = new HashSet<Listener>();
	
	// -------------------------------------------------------------------------- Live cycle and connection stuff

	@Override
	public void onCreate() {
		super.onCreate();
		
		mLocationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		currentLocation = getPreliminaryCurrentPosition();

	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	/** do IPC communication with Activities here */
	@Override
	public IBinder onBind(Intent intent) {
		return mLocalBinder;
	}

	public void addListener(Listener listener) {
		mListeners.add(listener);
	}
	
	// --------------------------------------------------------------------------------------- functionality
	
	private Location getPreliminaryCurrentPosition() {
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

	public void reset() {
		positions.clear();
		pathLength = 0;
		timeMillis = 0;
		currentPace = 0;
		// notify listeners
		for (Listener l : mListeners) {
			l.onLocationChanged(currentLocation);
			l.onTimerChanged(timeMillis);
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
		
		public void onTimerChanged(long timeMillis);
	}	
	
	// -------------------------------------------------------------------------------- LocationListener
	
	private LocationListener mLocationListener = new LocationListener() { /* nice trick with anonymous class */
		
		@Override
		public void onLocationChanged(Location location) {
	
			if (!isAcceptableLocation(location)) {
				return;
			}
			
			if (sessionState == RUNNING) {					// while running check distance too
				float dist = location.distanceTo(currentLocation);
				if (dist < MIN_DISTANCE || dist < currentLocation.getAccuracy()+location.getAccuracy()) {
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
			for (Listener l : mListeners) {
				l.onLocationChanged(location);
			}
		}
	
		private boolean isAcceptableLocation(Location location) {
	
			// based on http://developer.android.com/guide/topics/location/obtaining-user-location.html
			long deltaTime = location.getTime() - currentLocation.getTime();
			if (deltaTime > 30*SECOND_IN_MILLIS) {	// location is significantly newer than current location --> accept new one.
				return true;
			}
			if (!location.hasAccuracy()) {
				return false;
			}
		    // Check whether the new location fix is more or less accurate
		    int accuracyDelta = (int) (location.getAccuracy() - currentLocation.getAccuracy());
		    boolean isLessAccurate = accuracyDelta > 0;
		    boolean isMoreAccurate = accuracyDelta < 0;
		    boolean isSignificantlyLessAccurate = accuracyDelta > 200;
	
		    // Check if the old and new location are from the same provider
		    boolean isFromSameProvider = isSameProvider(location.getProvider(), currentLocation.getProvider());
	
		    // Determine location quality using a combination of timeliness and accuracy
		    if (isMoreAccurate) {
		        return true;
		    } else if (!isLessAccurate) {
		        return true;
		    } else if (!isSignificantlyLessAccurate && isFromSameProvider) {
		        return true;
		    }
		    return false;		
		}
		
		/** Checks whether two providers are the same */
		private boolean isSameProvider(String provider1, String provider2) {
		    if (provider1 == null) {
		      return provider2 == null;
		    }
		    return provider1.equals(provider2);
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

		private long mMillis;
		private long mOffsetMillis;
		
		/**
		 * params[0] == initial value in millis (0 at first start, but can be > 0 after stop/start ...)
		 */
		@Override
		protected String doInBackground(Long... params) {
			mMillis = params[0] != null ? params[0] : 0L;
			mOffsetMillis = System.currentTimeMillis();
			while (sessionState != IDLE) {
				mMillis = System.currentTimeMillis() - mOffsetMillis;
				publishProgress(mMillis);
				try {
					Thread.sleep(500);	// nur 1/2 Sekunde, damit ich sicher keine Sekunde verpasse
				} catch (InterruptedException e) {
				}
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Long... values) {
			// notify listeners
			for (Listener l : mListeners) {
				l.onTimerChanged(values[0]);
			}
		}

		public long getTimeMillis() {
			return mMillis;
		}
	}

	/** set data from session (called after load) */
	public void setSession(Session session) {
		pathLength = session.distance;
		timeMillis = session.time;
		positions = session.positions;
		// notify listeners
		for (Listener l : mListeners) {
			l.onLocationChanged(currentLocation);
			l.onTimerChanged(timeMillis);
		}
	}

	public void stopRecording() {
		sessionState = IDLE;
		mTimerTask.cancel(true);
		mTimerTask = null;
		// TODO nicht ganz deaktivieren!
		//mLocationMgr.removeUpdates(mLocationListener);
	}

	public void startRecording() {
		reset();
		positions.add(new Position(currentLocation));		// set first point of path
		sessionState = RUNNING;
		mTimerTask = new TimerTask();
		mTimerTask.execute(0L);	// TODO bei start/stop/start später andere Startzeit???
		mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, 0, mLocationListener);
	}

	/** stop tracking (when IDLE und Activity geht auf Pause) */
	public void stopTracking() {
		 mLocationMgr.removeUpdates(mLocationListener);
	}

	public void startTracking() {
		currentLocation = getPreliminaryCurrentPosition();
		// notify listeners
		for (Listener l : mListeners) {
			l.onLocationChanged(currentLocation);
		}
		mLocationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, 0, mLocationListener);
		mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, 0, mLocationListener);
	}	
}
