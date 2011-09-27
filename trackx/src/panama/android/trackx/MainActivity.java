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
	private final static int MIN_DISTANCE = 10; 						// in meters
	private final static int MIN_TIME = 3*MILLIS;
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
	private TextView mTachoView;
	private Button mStartButton;
	private Button mStopButton;
	private ProgressDialog mWaitingDialog;

	// Logic
	private LocationManager mLocationMgr;
	private Location mCurrentLocation;
	private Location mPrevLocation;
	private TimerTask mTimer;
	private TachoTask mTacho;
	private boolean mIsRunning;	// flag if tracking started by user (then we do not pause tracking when app is paused)

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mLocationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		mDistanceView = (TextView)findViewById(R.id.distance);
		mTimerView = (TextView)findViewById(R.id.time);
		mTachoView = (TextView)findViewById(R.id.tacho);
		mStartButton = (Button)findViewById(R.id.start);
		mStopButton = (Button)findViewById(R.id.stop);

		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.setBuiltInZoomControls(true);
		mMapView.getController().setZoom(20);
		
		mPathOverlay = new PathOverlay(savedInstanceState);
		mMapView.getOverlays().add(mPathOverlay);

		Location l = getPreliminaryCurrentPosition();
		if (l != null) {
			mMapView.getController().animateTo(Util.locationToGeoPoint(l));
		}
		
		refreshDistanceView();
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
    			mWaitingDialog = ProgressDialog.show(this, "", "Finding current location...", true);
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
	
	/**
	 * @param view
	 */
	public void onStartClicked(View view) {
		mStartButton.setEnabled(false);
		mStopButton.setEnabled(true);
		mIsRunning = true;
		mTimer = new TimerTask();
		mTimer.execute(0L);	// TODO bei start/stop/start später andere Startzeit
		mTacho = new TachoTask();
		mTacho.execute((Long)null);
		mDistanceView.setTextColor(COLOR_RUNNING);
		mTimerView.setTextColor(COLOR_RUNNING);
		mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
		
		// TODO unterscheiden zw. Neustart und Continue nach Pause (auch noch zu implementieren)
		//      Bei Neustart alles Resetten, bei Continue einfach weitermachen (ohne Reset)
		// folgende Zeilen dann nur bei Neustart:
		mPathOverlay.reset(mLocationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER));
		refreshDistanceView();
	}

	public void onStopClicked(View view) {
		findViewById(R.id.start).setEnabled(true);
		findViewById(R.id.stop).setEnabled(false);
		mIsRunning = false;
		mTimer.cancel(true);
		mTimer = null;
		mTacho.cancel(true);
		mTacho = null;
		mDistanceView.setTextColor(COLOR_IDLE);
		mTimerView.setTextColor(COLOR_IDLE);
		mLocationMgr.removeUpdates(this);
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
	
	/** use only in UI Thread (e.g. in on...() methods */
	private void refreshDistanceView() {
		int meters = (int)mPathOverlay.getPathLength();
		if (meters < 1000) {
		mDistanceView.setText(String.format("%dm", meters));
		} else {
			mDistanceView.setText(String.format("%.3fkm", meters/1000f));
		}
		mDistanceView.invalidate();
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
				mWaitingDialog.dismiss();
			}
			mPrevLocation = mCurrentLocation;
			mCurrentLocation = location;	// used for displaying where we are and where we're heading to.
			mPathOverlay.setCurrentLocation(location);
			mMapView.getController().animateTo(Util.locationToGeoPoint(location));

			if (mIsRunning) {
				mPathOverlay.appendLocation(location);
				refreshDistanceView();
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
		float dist = location.distanceTo(mPrevLocation);
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
	class TimerTask extends AsyncTask<Long, String, String> {

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
				publishProgress(Util.formatTime(mMillis));
				try {
					Thread.sleep(500);	// nur 1/2 Sekunde, damit ich sicher keine Sekunde verpasse
				} catch (InterruptedException e) {
				}
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			mTimerView.setText(values[0]);
		}

		public long getTimeMillis() {
			return mMillis;
		}
	}

	// -------------------------------------------------------------------------------- TachoTask
	
	/**
	 * Läuft parallel und berechnet die aktuelle Geschwindigkeit und postet sie ebenso schön formatiert
	 * 
	 * @author ridcully
	 */
	class TachoTask extends AsyncTask<Long, String, String> {

		@Override
		protected String doInBackground(Long... params) {
			float oldLength = mPathOverlay.getPathLength();
			while (mIsRunning) {
				try {
					Thread.sleep(CURRENT_SPEED_INTERVAL_MILLIS);
				} catch (InterruptedException e) {
				}
				float newLength = mPathOverlay.getPathLength();
				publishProgress(Util.formatSpeed(newLength-oldLength, CURRENT_SPEED_INTERVAL_MILLIS));
				oldLength = newLength;
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			mTachoView.setText(values[0]);
		}
	}	
}