package panama.android.trackx;

import java.util.List;

import panama.android.trackx.TrackerService.LocalBinder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class MainActivity extends MapActivity implements TrackerService.Listener {

	public final static String TAG = "trackx";
	
	// UI
	public final static int REQUEST_ENABLING_GPS = 1;
	public final static int REQUEST_LOAD_SESSION  = 2;
	public final static int REQUEST_SETTINGS  = 3;
	public final static int DIALOG_ENABLE_GPS = 1;

	private MapView mMapView;
	private PathOverlay mPathOverlay;
	private MyLocationOverlay mMyLocationOverlay;
	private TextView mSessionStartView;
	private TextView mDistanceView;
	private TextView mTimerView;
	private TextView mPaceView;
	private TextView mAveragePaceView;
	private TextView mCaloriesView;
	private Button mNewSessionButton;
	private Button mStartButton;
	private Button mPauseButton;
	private Button mResumeButton;
	private Button mStopButton;
	private Button mDiscardButton;
	private Button mSaveButton;
	private ImageButton mToggleMapButton;
	private ProgressDialog mWaitingDialog;

	private PowerManager.WakeLock mWakeLock;
	private LocationManager mLocationMgr;

	private SessionPersistence mPersistence = new SessionPersistence(this);
	private TrackerService mService;
	private boolean mBound;	// whether bound to service
	
	private SharedPreferences mPrefs;
	private int mGender = Util.GENDER_UNKNOWN;
	private float mWeight = Util.DEFAULT_WEIGHT;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLocationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		setContentView(R.layout.main);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);

		mSessionStartView = (TextView)findViewById(R.id.sessionDateTime);
		mDistanceView = (TextView)findViewById(R.id.distance);
		mTimerView = (TextView)findViewById(R.id.time);
		mPaceView = (TextView)findViewById(R.id.pace);
		mAveragePaceView = (TextView)findViewById(R.id.averagePace);
		mCaloriesView = (TextView)findViewById(R.id.calories);
		
//		Typeface numberFont = Typeface.createFromAsset(this.getAssets(), "fonts/LEXIB___.ttf");
//		mDistanceView.setTypeface(numberFont);
//		mTimerView.setTypeface(numberFont);
//		mPaceView.setTypeface(numberFont);
//		mAveragePaceView.setTypeface(numberFont);
		
		mNewSessionButton = (Button)findViewById(R.id.newSession);
		mNewSessionButton.setEnabled(false);	// disabled bis wir mit Service verbunden sind.
		mStartButton = (Button)findViewById(R.id.start);
		mPauseButton = (Button)findViewById(R.id.pause);
		mResumeButton = (Button)findViewById(R.id.resume);
		mStopButton = (Button)findViewById(R.id.stop);
		mDiscardButton = (Button)findViewById(R.id.discard);
		mSaveButton = (Button)findViewById(R.id.save);

		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.setBuiltInZoomControls(true);
		mMapView.getController().setZoom(19);
		mPathOverlay = new PathOverlay();
		mMyLocationOverlay = new MyLocationOverlay();
		mMapView.getOverlays().add(mPathOverlay);
		mMapView.getOverlays().add(mMyLocationOverlay);
		mToggleMapButton = (ImageButton)findViewById(R.id.toggleMap);
		mToggleMapButton.setOnTouchListener(mToggleMapButtonListener);	// eigener Listener um pressed Status fix setzen zu können, sodass er auch bleibt.
		
		// Bind to TrackerService (asynchronous)
		Intent intent = new Intent(this, TrackerService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);	// this is done asynchronously
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unbind from the service
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
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
					.setNegativeButton("Cancel", 
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									Toast.makeText(MainActivity.this, "Sorry, no tracking without GPS.", Toast.LENGTH_SHORT).show();
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
		case R.id.miLoad:
			Intent intent = new Intent(this, SessionListActivity.class);
			startActivityForResult(intent, REQUEST_LOAD_SESSION);
			return true;
		case R.id.miSettings:
			intent = new Intent(this, SettingsActivity.class);
			startActivityForResult(intent, REQUEST_SETTINGS); 
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case REQUEST_ENABLING_GPS:
			// see if user enabled GPS
			if (!mLocationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				showDialog(DIALOG_ENABLE_GPS);	// if not, once again ask to enable it.
			} else {
				waitForSatisfactoryStartingLocation();
			}
			return;
		case REQUEST_LOAD_SESSION:
			if (resultCode == RESULT_OK) {
				long id = data.getLongExtra("id", -1);
				Session session = mPersistence.load(id);
				mService.setSession(session);
				adjustMap(session.positions);
			}
			return;
		case REQUEST_SETTINGS:
			mGender = Integer.parseInt(mPrefs.getString(SettingsActivity.GENDER_KEY, ""+Util.GENDER_UNKNOWN));
			mWeight = Float.parseFloat(mPrefs.getString(SettingsActivity.WEIGHT_KEY, ""+Util.DEFAULT_WEIGHT));
			// TODO check if units preference has changed and adapt labels and value displays.
			return;
		}
	}
	
	@Override
	public void onBackPressed() {
		/* undo "New Session" using back button */
		if (mStartButton.getVisibility() == View.VISIBLE) {
			mStartButton.setVisibility(View.GONE);
			mNewSessionButton.setVisibility(View.VISIBLE);
			mService.stopTracking();
			mService.reset();
		} else {
			super.onBackPressed();
		}
	}
	
	public void onNewSessionClicked(View view) {
		if (!mLocationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			showDialog(DIALOG_ENABLE_GPS);
		} else {
			// wir sind schon mit service verbunden (da Button erst dann aktiv)
			waitForSatisfactoryStartingLocation();
		}
	}

	private void waitForSatisfactoryStartingLocation() {
		mService.startTracking();
		mService.reset();
		mWaitingDialog = ProgressDialog.show(this, "", "Let's see were we are right now...", true, true, new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				mService.stopTracking();
				mService.reset();
			}
		});
	}

	public void onStartClicked(View view) {
		mStartButton.setVisibility(View.GONE);
		mPauseButton.setVisibility(View.VISIBLE);
		mStopButton.setVisibility(View.VISIBLE);
		mService.startRecording();
		mWakeLock.acquire();										// standby verhindern
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
		Log.d(TAG, "onStopClicked begin");
		mPauseButton.setVisibility(View.GONE);
		mResumeButton.setVisibility(View.GONE);
		mStopButton.setVisibility(View.GONE);
		mDiscardButton.setVisibility(View.VISIBLE);
		mSaveButton.setVisibility(View.VISIBLE);
		mService.stopRecording();
		mService.stopTracking();	// versuchsweise mal
		setCurrentPace(0);

		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
		Log.d(TAG, "onStopClicked done");
	}

	public void onDiscardClicked(View view) {
		mDiscardButton.setVisibility(View.GONE);
		mSaveButton.setVisibility(View.GONE);
		mNewSessionButton.setVisibility(View.VISIBLE);
		mService.reset();
	}
	
	public void onSaveClicked(View view) {
		try {
			Session session = new Session("title-unused", "notes-unused", mService.sessionStartedAtMillis, mService.elapsedTimeMillis, mService.pathLength, mService.positions);
			mPersistence.save(session);
			Toast.makeText(this, "session saved", Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Log.e("trackx", "saving failed", e);
			Toast.makeText(this, "saving failed: "+e.getMessage(), Toast.LENGTH_LONG).show();
		}
		mDiscardButton.setVisibility(View.GONE);
		mSaveButton.setVisibility(View.GONE);
		mNewSessionButton.setVisibility(View.VISIBLE);
	}
	
	/** 
	 * zoom/move map so that start and end point are visible.
	 * based on http://stackoverflow.com/questions/7513247/how-to-zoom-a-mapview-so-it-always-includes-two-geopoints
	 *   hab aber die komische ratio Anpassung entfernt, sonst gings nicht
	 */
	private void adjustMap(List<Position> positions) {
		if (positions == null || positions.isEmpty()) {
			return;
		}
		GeoPoint[] bounds = mPathOverlay.getBoundingBox();
		GeoPoint a = bounds[0];
		GeoPoint b = bounds[1];
		GeoPoint center = new GeoPoint(a.getLatitudeE6()+(b.getLatitudeE6()-a.getLatitudeE6())/2, 
									   a.getLongitudeE6()+(b.getLongitudeE6()-a.getLongitudeE6())/2);
		
		double latitudeSpan = Math.round(Math.abs(a.getLatitudeE6() - b.getLatitudeE6()));
		double longitudeSpan = Math.round(Math.abs(a.getLongitudeE6() - b.getLongitudeE6())); 

		mMapView.getController().setCenter(center);
		mMapView.getController().zoomToSpan((int)(latitudeSpan*1.5), (int)(longitudeSpan*1.5));                
		mMapView.invalidate();
	}

	private void setPathLength(float meters) {
		mDistanceView.setText(Util.formatDistance(meters));
		mDistanceView.invalidate();		
	}
	
	private void setCurrentPace(float metersPerSecond) {
		mPaceView.setText(Util.formatSpeed(metersPerSecond));
		mPaceView.invalidate();
	}

	private void setAveragePace(float metersPerSecond) {
		mAveragePaceView.setText(Util.formatSpeed(metersPerSecond));
		mAveragePaceView.invalidate();
	}
	
	private void setTime(long timeMillis) {
		mTimerView.setText(Util.formatTime(timeMillis));
		mTimerView.invalidate();
	}
	
	private void setCalories(int kcal) {
		mCaloriesView.setText(""+kcal);
		mCaloriesView.invalidate();
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
	
	// -------------------------------------------------------------------------- TrackerService.Listener Implementation
	
	@Override
	public void onLocationChanged(Location location) {
		if (mWaitingDialog != null && mWaitingDialog.isShowing() && mService.trackingFoundLocation && Util.isOKforStart(location)) {
			mWaitingDialog.dismiss();
			mNewSessionButton.setVisibility(View.GONE);
			mStartButton.setVisibility(View.VISIBLE);
			Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(40); //vibrieren damit User weiss dass er jetzt starten kann (ohne immer hinsehen zu müssen)
		}		
		
		// EXPERIMENTAL
		if (mStartButton.getVisibility() == View.VISIBLE) {
			mStartButton.setText("Start " + ((int)location.getAccuracy())+"m");
		}
		
		mMyLocationOverlay.setLocation(mService.currentLocation);
		mPathOverlay.setPositions(mService.positions);
		if (location != null) {
			mMapView.getController().setCenter(Util.locationToGeoPoint(location));
		}
		mMapView.invalidate();		
		setPathLength(mService.pathLength);
	}
	
	@Override
	public void onPaceChanged(float currentPace, float averagePace) {
		setCurrentPace(currentPace);
		setAveragePace(averagePace);
	}
	
	@Override
	public void onTimerChanged(long timeMillis) {
		setTime(mService.elapsedTimeMillis);
		setCalories(Util.calculateCalories(mGender, mWeight, mService.elapsedTimeMillis, mService.pathLength, 0f));
	}
	
	@Override
	public void onRefreshAll() {
		onLocationChanged(null);
		onPaceChanged(mService.currentPace, mService.averagePace);
		onTimerChanged(mService.elapsedTimeMillis);
		if (mService.sessionStartedAtMillis > 0) {
			mSessionStartView.setText("Session of "+Util.formatDateShort(mService.sessionStartedAtMillis));
		} else {
			mSessionStartView.setText("");
		}
	}
	
	// ------------------------------------------------------------------------------------------------- service connection
	
	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() { /* nice trick with anonymous class */

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
			mService.addListener(MainActivity.this);	// add ourselves as listeners
			// reset NICHT machen -- kann sein dass schon recording läuft
			// mService.reset();
			mNewSessionButton.setEnabled(true);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mBound = false;
		}
	};

	/** eigener OnTouchListener um Button.pressed Status fix setzen und entfernen zu können (wie bei FingerColors) */
	private OnTouchListener mToggleMapButtonListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				if (mMapView.getVisibility() == View.GONE) {
					mMapView.bringToFront();
					Animation in = AnimationUtils.loadAnimation(MainActivity.this, R.anim.shift_in_from_bottom);
					in.setZAdjustment(Animation.ZORDER_TOP);
					mMapView.setVisibility(View.VISIBLE);
					mMapView.startAnimation(in);
					v.setPressed(true);
				} else {
					mMapView.getZoomButtonsController().setVisible(false);	// remove zoom buttons immediately
					Animation out = AnimationUtils.loadAnimation(MainActivity.this, R.anim.shift_out_to_bottom);
					mMapView.startAnimation(out);
					mMapView.setVisibility(View.GONE);
					v.setPressed(false);
				}
			}
			return true;
		}
	};
}