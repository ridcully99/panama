package panama.android.trackx;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class MainActivity extends MapActivity implements LocationListener {

	public final static String MY_TAG = "trackx";
	
	// UI
	public final static int REQUEST_ENABLING_GPS = 1;
	public final static int REQUEST_LOAD_SESSION  = 2;
	public final static int DIALOG_ENABLE_GPS = 1;

	private MapView mMapView;
	private PathOverlay mPathOverlay;
	private MyLocationOverlay mMyLocationOverlay;
	private TextView mDistanceView;
	private TextView mTimerView;
	private TextView mPaceView;
	private Button mStartButton;
	private Button mPauseButton;
	private Button mResumeButton;
	private Button mStopButton;
	private Button mDiscardButton;
	private Button mSaveButton;
	private ProgressDialog mWaitingDialog;
	private PowerManager.WakeLock mWakeLock;

	// Logic
	public final static int SECOND_IN_MILLIS = 1000;
	public final static int MIN_DISTANCE = 5; 						   // in meters; 5 ist in MyTracks empfohlen das von Google-Leuten gemacht wird
	public final static int MIN_TIME = 1*SECOND_IN_MILLIS;
	public final static int IDLE = 0;
	public final static int PAUSED = 1;
	public final static int RUNNING = 2;
	
	private LocationManager mLocationMgr;
	private final List<Position> mPositions = new ArrayList<Position>();	// final, weil PathOverlay die verwendet. Statt Zuweisen bei Bedarf clear() + addAll()
	public Location mCurrentLocation;
	private TimerTask mTimerTask;
	private float mPathLength;
	private long mTime;
	private float mPace;
	private int mSessionState = IDLE;	// IDLE, PAUSED, RUNNING
	private SessionPersistence mPersistence;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mLocationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, MY_TAG);

		mDistanceView = (TextView)findViewById(R.id.distance);
		mTimerView = (TextView)findViewById(R.id.time);
		mPaceView = (TextView)findViewById(R.id.pace);
		mStartButton = (Button)findViewById(R.id.start);
		mPauseButton = (Button)findViewById(R.id.pause);
		mResumeButton = (Button)findViewById(R.id.resume);
		mStopButton = (Button)findViewById(R.id.stop);
		mDiscardButton = (Button)findViewById(R.id.discard);
		mSaveButton = (Button)findViewById(R.id.save);

		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.setBuiltInZoomControls(true);
		mMapView.getController().setZoom(20);

		mPathOverlay = new PathOverlay(mPositions, savedInstanceState);
		mMyLocationOverlay = new MyLocationOverlay(this, savedInstanceState);
		mMapView.getOverlays().add(mPathOverlay);
		mMapView.getOverlays().add(mMyLocationOverlay);

		mCurrentLocation = getPreliminaryCurrentPosition();
		if (mCurrentLocation != null) {
			mMapView.getController().animateTo(Util.locationToGeoPoint(mCurrentLocation));
		}
		
		View dataAreaLayout = findViewById(R.id.dataArea);	// a Layout is a View
		dataAreaLayout.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO groß/klein togglen (schön wäre mit Animation); 
				// TODO eventuell durch ändern des Style bei klein auch die Schrift kleiner machen 
				return false;
			}
		});
		
		mPersistence = new SessionPersistence(this);
	}

	/** 
	 * Save state to be restored in onCreate.
	 * I.e. save current Track state.
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// TODO save current track state
	}

	@Override
	protected void onResume() {
		super.onResume();
		//if (!mLocationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
		//	showDialog(DIALOG_ENABLE_GPS);
		//} else {
		//	if (mSessionState == IDLE && !Util.isUpToDate(mCurrentLocation)) {
		//		mCurrentLocation = null;
		//		mWaitingDialog = ProgressDialog.show(this, "", "Finding current location...", true);
				mLocationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, 0, this);
				mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, 0, this);
		//	}
		//}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (mSessionState == IDLE) {
			mLocationMgr.removeUpdates(this);
//		} else {
//			// auch wenn running, die minTime auf 60 sek setzen; minDistance bleibt aber
//			// damit kann ich hoffentlich etwas Strom sparen
//			mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60*SECOND_IN_MILLIS, MIN_DISTANCE, this);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case DIALOG_ENABLE_GPS:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Please enable GPS for tracking.")
					.setCancelable(true)
					.setPositiveButton("Settings",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									Intent myIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
									startActivityForResult(myIntent, REQUEST_ENABLING_GPS);
								}
							})
					.setNegativeButton("Exit", 
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									MainActivity.this.finish();
								}
							});
			return builder.create();
		}
		return null;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.miNew:
			reset();
			mMapView.invalidate();								// re-render path
			return true;
		case R.id.miLoad:
			Intent intent = new Intent(this, SessionListActivity.class);
			startActivityForResult(intent, REQUEST_LOAD_SESSION);
		}
		return super.onOptionsItemSelected(item);
	}

	public void onStartClicked(View view) {
		mStartButton.setVisibility(View.GONE);
		mPauseButton.setVisibility(View.VISIBLE);
		mStopButton.setVisibility(View.VISIBLE);
		reset();
		mPositions.add(new Position(mCurrentLocation));		// set first point of path
		mSessionState = RUNNING;
		mMapView.invalidate();								// re-render path
		mTimerTask = new TimerTask();
		mTimerTask.execute(0L);	// TODO bei start/stop/start später andere Startzeit???
		mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
		
		mWakeLock.acquire();								// standby verhindern
	}

	public void onPauseClicked(View view) {
		mPauseButton.setVisibility(View.GONE);
		mResumeButton.setVisibility(View.VISIBLE);
		
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}
	
	public void onResumeClicked(View view) {
		mPauseButton.setVisibility(View.VISIBLE);
		mResumeButton.setVisibility(View.GONE);

		mWakeLock.acquire();		
	}
	
	public void onStopClicked(View view) {
		mPauseButton.setVisibility(View.GONE);
		mResumeButton.setVisibility(View.GONE);
		mStopButton.setVisibility(View.GONE);
		mDiscardButton.setVisibility(View.VISIBLE);
		mSaveButton.setVisibility(View.VISIBLE);
		mSessionState = IDLE;
		mTimerTask.cancel(true);
		mTimerTask = null;
		mLocationMgr.removeUpdates(this);
		
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}

	public void onDiscardClicked(View view) {
		mDiscardButton.setVisibility(View.GONE);
		mSaveButton.setVisibility(View.GONE);
		mStartButton.setVisibility(View.VISIBLE);
		reset();
		mMapView.invalidate();
	}
	
	public void onSaveClicked(View view) {
		try {
			Session session = new Session(Util.uniqueFilename(), "notizen", System.currentTimeMillis(), mTime, mPathLength, mPositions);
			mPersistence.save(session);
		} catch (Exception e) {
			Log.e("trackx", "saving failed", e);
			Toast.makeText(this, "saving failed: "+e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void reset() {
		mPositions.clear();
		setPathLength(0);
		setTime(0);
		setPace(0);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case REQUEST_ENABLING_GPS:
			// see if user enabled GPS
			if (!mLocationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				showDialog(DIALOG_ENABLE_GPS);	// if not, once again ask to enable it.
			}
			
		case REQUEST_LOAD_SESSION:
			if (resultCode == RESULT_OK) {
				long id = data.getLongExtra("id", -1);
				reset();
				Session session = mPersistence.load(id);
				setPathLength(session.distance);
				setTime(session.time);
				mPositions.clear();
				mPositions.addAll(session.positions);	// mPositions 
				mMapView.invalidate();
			}
		}
	}
	
	private Location getPreliminaryCurrentPosition() {
		Location coarseLocation = mLocationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location fineLocation = mLocationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (coarseLocation == null) {
			return fineLocation;
		} else if (fineLocation == null) {
			return coarseLocation;
		} else {
			if (coarseLocation.getTime() > fineLocation.getTime()) {
				return coarseLocation;
			} else {
				return fineLocation;
			}
		}
	}

	/** call from UI thread */
	private void setPathLength(float meters) {
		mPathLength = meters;
		//if (meters < 1000) {
		//	mDistanceView.setText(String.format("%dm", (int)meters));
		//} else {
			mDistanceView.setText(String.format("%.3f", meters/1000f));
		//}
		mDistanceView.invalidate();		
	}
	
	private void setPace(float metersPerSecond) {
		mPaceView.setText(Util.formatSpeed(metersPerSecond));
		mPaceView.invalidate();
	}
	
	private void setTime(long millis) {
		mTime = millis;
		mTimerView.setText(Util.formatTime(millis));
		mTimerView.invalidate();
	}
	
	// -------------------------------------------------------------------------------------- MapActivity Implementation

	@Override
	protected boolean isLocationDisplayed() {
		return true;
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	// -------------------------------------------------------------------------------- LocationListener Implementation
	
	@Override
	public void onLocationChanged(Location location) {

		if (!isAcceptableLocation(location)) {
			return;
		}
		
		if (mSessionState == RUNNING) {					// while running check distance too
			float dist = location.distanceTo(mCurrentLocation);
			if (dist < MIN_DISTANCE) {
				return;	// no adding to path _AND_ no update of current pos. (otherwise current pos would move away from path)
			}
			Position p = new Position(location);
			p.distance = dist;
			mPositions.add(p);
			setPathLength(mPathLength+dist);
			setPace(location.getSpeed());
		}
		mCurrentLocation = location;
		mMapView.getController().animateTo(Util.locationToGeoPoint(location));
		mMapView.invalidate();
	}

	private boolean isAcceptableLocation(Location location) {

		// based on http://developer.android.com/guide/topics/location/obtaining-user-location.html
		long deltaTime = location.getTime() - mCurrentLocation.getTime();
		if (deltaTime > 30*SECOND_IN_MILLIS) {	// location is significantly newer than current location --> accept new one.
			return true;
		}
		if (!location.hasAccuracy()) {
			return false;
		}
	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - mCurrentLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(), mCurrentLocation.getProvider());

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
			while (mSessionState != IDLE) {
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
			setTime(values[0]);
		}

		public long getTimeMillis() {
			return mMillis;
		}
	}	
}