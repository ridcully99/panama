package panama.android.trackx;

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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class MainActivity extends MapActivity implements LocationListener {

	private final static int MILLIS = 1000;
	private final static int MIN_DISTANCE = 5; 						   // in meters; 5 ist in MyTracks empfohlen das von Google-Leuten gemacht wird
	private final static int MIN_TIME = 1*MILLIS;
	private final static int COLOR_IDLE = 0xffffffff;					// white
	private final static int COLOR_RUNNING = 0xff00ff00;				// green
	private final static int CURRENT_SPEED_INTERVAL_MILLIS = 1*MILLIS;	//  
	
	private final static int REQUEST_ENABLING_GPS = 1;
	private final static int DIALOG_ENABLE_GPS = 1;
	
	// UI
	private MapView mMapView;
	private PathOverlay mPathOverlay;
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

	// Logic
	private LocationManager mLocationMgr;
	private Location mCurrentLocation;
	private Location mPrevLocation;
	private TimerTask mTimerTask;
	private PaceTask mPaceTask;
	private boolean mIsRunning;	// flag if tracking started by user (then we do not pause tracking when app is paused)
	private float mDistance;
	private long mTime;
	private float mPace;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mLocationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		mDistanceView = (TextView)findViewById(R.id.distance);
		mTimerView = (TextView)findViewById(R.id.time);
		mPaceView = (TextView)findViewById(R.id.tacho);
		mStartButton = (Button)findViewById(R.id.start);
		mPauseButton = (Button)findViewById(R.id.pause);
		mResumeButton = (Button)findViewById(R.id.resume);
		mStopButton = (Button)findViewById(R.id.stop);
		mDiscardButton = (Button)findViewById(R.id.discard);
		mSaveButton = (Button)findViewById(R.id.save);

		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.setBuiltInZoomControls(true);
		mMapView.getController().setZoom(20);
		
		mPathOverlay = new PathOverlay(savedInstanceState);
		mMapView.getOverlays().add(mPathOverlay);

		Location l = getPreliminaryCurrentPosition();
		if (l != null) {
			mMapView.getController().animateTo(Util.locationToGeoPoint(l));
		}
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
		if (!mLocationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
    		showDialog(DIALOG_ENABLE_GPS);
    	} else {
    		if (!mIsRunning && !Util.isUpToDate(mCurrentLocation)) {
    			mCurrentLocation = null;
    			// TEST mWaitingDialog = ProgressDialog.show(this, "", "Finding current location...", true);
    			mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    		}
    	}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (!mIsRunning) {
			mLocationMgr.removeUpdates(this);
//		} else {
//			// auch wenn running, die minTime auf 60 sek setzen; minDistance bleibt aber
//			// damit kann ich hoffentlich etwas Strom sparen
//			mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60*MILLIS, MIN_DISTANCE, this);
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
	

	public void onStartClicked(View view) {
		mStartButton.setVisibility(View.GONE);
		mPauseButton.setVisibility(View.VISIBLE);
		mStopButton.setVisibility(View.VISIBLE);
		mIsRunning = true;
		mTimerTask = new TimerTask();
		mTimerTask.execute(0L);	// TODO bei start/stop/start später andere Startzeit
		mPaceTask = new PaceTask();
		mPaceTask.execute((Long)null);
		mDistanceView.setTextColor(COLOR_RUNNING);
		mTimerView.setTextColor(COLOR_RUNNING);
		mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
		
		// TODO unterscheiden zw. Neustart und Continue nach Pause (auch noch zu implementieren)
		//      Bei Neustart alles Resetten, bei Continue einfach weitermachen (ohne Reset)
		// folgende Zeilen dann nur bei Neustart:
		//mPathOverlay.reset(mLocationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER));
		//refreshDistanceView();
	}

	public void onPauseClicked(View view) {
		mPauseButton.setVisibility(View.GONE);
		mResumeButton.setVisibility(View.VISIBLE);
	}
	
	public void onResumeClicked(View view) {
		mPauseButton.setVisibility(View.VISIBLE);
		mResumeButton.setVisibility(View.GONE);
	}
	
	public void onStopClicked(View view) {
		mPauseButton.setVisibility(View.GONE);
		mResumeButton.setVisibility(View.GONE);
		mStopButton.setVisibility(View.GONE);
		mDiscardButton.setVisibility(View.VISIBLE);
		mSaveButton.setVisibility(View.VISIBLE);
		mIsRunning = false;
		mTimerTask.cancel(true);
		mTimerTask = null;
		mPaceTask.cancel(true);
		mPaceTask = null;
		mDistanceView.setTextColor(COLOR_IDLE);
		mTimerView.setTextColor(COLOR_IDLE);
		mLocationMgr.removeUpdates(this);
	}

	public void onDiscardClicked(View view) {
		mDiscardButton.setVisibility(View.GONE);
		mSaveButton.setVisibility(View.GONE);
		mStartButton.setVisibility(View.VISIBLE);
		mPathOverlay.reset(mLocationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER));
		setDistance(0);
		setTime(0);
		setPace(0);
	}
	
	public void onSaveClicked(View view) {
		mDiscardButton.setVisibility(View.GONE);
		mSaveButton.setVisibility(View.GONE);
		mStartButton.setVisibility(View.VISIBLE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLING_GPS) {
			// see if user enabled GPS
    		if (!mLocationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
    			showDialog(DIALOG_ENABLE_GPS);	// if not, once again ask to enable it.
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
	private void setDistance(float meters) {
		mDistance = meters;
		if (meters < 1000) {
			mDistanceView.setText(String.format("%dm", (int)meters));
		} else {
			mDistanceView.setText(String.format("%.3fkm", meters/1000f));
		}
		mDistanceView.invalidate();		
	}
	
	private void setPace(float metersPerSecond) {
		mPace = metersPerSecond;
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
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	// -------------------------------------------------------------------------------- LocationListener Implementation
	
	@Override
	public void onLocationChanged(Location location) {
		// if current location is null, we came here from startWaitingForUpToDateGPSLocation()
		// as we now have an up to date location, change the request so we do only get updates on moving MIN_DISTANCE.
		if (isAcceptableLocation(location)) {
			if (mCurrentLocation == null) {
				mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
				mStartButton.setEnabled(true);
				if (mWaitingDialog != null) {
					mWaitingDialog.dismiss();
				}
			}
			mPrevLocation = mCurrentLocation;
			mCurrentLocation = location;	// used for displaying where we are and where we're heading to.
			mPathOverlay.setCurrentLocation(location);
			mMapView.getController().animateTo(Util.locationToGeoPoint(location));

			if (mIsRunning) {
				mPathOverlay.appendLocation(location);
				setDistance(mPathOverlay.getPathLength());
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
			mCurrentLocation = null;	// this will trigger startWaitingForUpToDateGPSLocation() in onResume()
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
	}
	
	private boolean isAcceptableLocation(Location location) {
		if ((mCurrentLocation == null || mPrevLocation == null) && location.getAccuracy() <= 20) {
			return true;
		}
		float dist = mPrevLocation == null ? 0 : location.distanceTo(mPrevLocation);
		if (location.hasAccuracy() && location.getAccuracy() < dist) {	// genau genug?
			return true;
		}
		return false;
	}
	
	// -------------------------------------------------------------------------------- TimerTask

	/**
	 * Stoppuhr, läuft parallel und postet akt. Zeit schön formatiert.
	 * TODO Berechnet gleichzeitig auch die aktuelle Geschwindigkeit und postet sie ebenso schön formatiert
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
			while (mIsRunning) {
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

	// -------------------------------------------------------------------------------- PaceTask
	
	/**
	 * Läuft parallel und berechnet die aktuelle Geschwindigkeit und postet sie ebenso schön formatiert
	 * 
	 * @author ridcully
	 */
	class PaceTask extends AsyncTask<Long, Float, String> {

		@Override
		protected String doInBackground(Long... params) {
			float oldLength = mPathOverlay.getPathLength();
			while (mIsRunning) {
				try {
					Thread.sleep(CURRENT_SPEED_INTERVAL_MILLIS);
				} catch (InterruptedException e) {
				}
				float newLength = mPathOverlay.getPathLength();
				float metersPerSecond = newLength-oldLength/(CURRENT_SPEED_INTERVAL_MILLIS/1000f);
				publishProgress(metersPerSecond);
				oldLength = newLength;
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Float... values) {
			setPace(values[0]);
		}
	}	
}