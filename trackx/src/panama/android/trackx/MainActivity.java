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

	// UI
	private final static int COLOR_IDLE = 0xffffffff;					// white
	private final static int COLOR_RUNNING = 0xff00ff00;				// green	
	private final static int REQUEST_ENABLING_GPS = 1;
	private final static int DIALOG_ENABLE_GPS = 1;

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
	private final static int MILLIS = 1000;
	private final static int MIN_DISTANCE = 5; 						   // in meters; 5 ist in MyTracks empfohlen das von Google-Leuten gemacht wird
	private final static int MIN_TIME = 1*MILLIS;
	private final static int IDLE = 0;
	private final static int PAUSED = 1;
	private final static int RUNNING = 2;
	
	private LocationManager mLocationMgr;
	private Location mCurrentLocation;
	private Location mPrevLocation;
	private TimerTask mTimerTask;
	private PaceTask mPaceTask;
	private float mPathLength;
	private long mTime;
	private float mPace;
	private int mSessionState = IDLE;	// IDLE, PAUSED, RUNNING
	
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
			if (mSessionState == IDLE && !Util.isUpToDate(mCurrentLocation)) {
				mCurrentLocation = null;
				mWaitingDialog = ProgressDialog.show(this, "", "Finding current location...", true);
				mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			}
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (mSessionState == IDLE) {
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
		mPathOverlay.reset(mCurrentLocation);
		setPathLength(0);
		mSessionState = RUNNING;
		mTimerTask = new TimerTask();
		mTimerTask.execute(0L);	// TODO bei start/stop/start später andere Startzeit
		mPaceTask = new PaceTask();
		mPaceTask.execute((Long)null);
		mDistanceView.setTextColor(COLOR_RUNNING);
		mTimerView.setTextColor(COLOR_RUNNING);
		mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
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
		mSessionState = IDLE;
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
		setPathLength(0);
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
	private void setPathLength(float meters) {
		mPathLength = meters;
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

		if (isAcceptableLocation(location)) {
			if (mCurrentLocation == null) {
				// if current location is null, we probably came here from onResume() where immediate updates were requested.
				// we will reduce this request now a little bit, as we now have an up to date fix.
				mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
				mStartButton.setEnabled(true);	// we can enable the start button even if we were already running, as it is not visible then anyway
				if (mWaitingDialog != null && mWaitingDialog.isShowing()) {
					mWaitingDialog.dismiss();
				}
			}
			float dist = mPrevLocation != null ? location.distanceTo(mPrevLocation) : 0;
			mPrevLocation = mCurrentLocation;
			mCurrentLocation = location;				// used for displaying where we are and where we're heading to.
			mMapView.getController().animateTo(Util.locationToGeoPoint(location));
			mPathOverlay.setCurrentLocation(location);

			if (mSessionState == RUNNING) {
				mPathOverlay.appendLocation(location);
				setPathLength(mPathLength+dist);
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

	// -------------------------------------------------------------------------------- PaceTask
	
	private final static int CURRENT_SPEED_INTERVAL_MILLIS = 3*MILLIS;		// wie oft speed upgedatet wird  
	private final static int SAMPLE_SIZE = 3;

	/**
	 * Läuft parallel und berechnet die aktuelle Geschwindigkeit und postet sie.
	 * Wir merken und immer die letzten Werte und errechnen daraus den Durchschnitt, um zu starke Schwankungen abzufangen
	 * 
	 * @author ridcully
	 */
	class PaceTask extends AsyncTask<Long, Float, String> {

		@Override
		protected String doInBackground(Long... params) {
			float samples[] = new float[SAMPLE_SIZE];
			int sampleIdx = 0;
			float oldLength = mPathLength;
			while (mSessionState != IDLE) {
				try {
					Thread.sleep(CURRENT_SPEED_INTERVAL_MILLIS);
				} catch (InterruptedException e) {
				}
				float newLength = mPathLength;	// possibly changed while we were sleeping
				float metersPerSecond = newLength-oldLength/(CURRENT_SPEED_INTERVAL_MILLIS/1000f);
				sampleIdx = (sampleIdx+1) % SAMPLE_SIZE;
				samples[sampleIdx] = metersPerSecond;
				float avg = 0;
				for (float s : samples) {
					avg += s;
				}
				publishProgress(avg/SAMPLE_SIZE);
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