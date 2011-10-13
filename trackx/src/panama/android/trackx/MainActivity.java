package panama.android.trackx;

import java.util.List;

import panama.android.trackx.TrackerService.LocalBinder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.RectF;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class MainActivity extends MapActivity implements TrackerService.Listener {

	public final static String TAG = "trackx";
	
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
	private TextView mAveragePaceView;
	private Button mNewSessionButton;
	private Button mStartButton;
	private Button mPauseButton;
	private Button mResumeButton;
	private Button mStopButton;
	private Button mDiscardButton;
	private Button mSaveButton;
	private ProgressDialog mWaitingDialog;

	private PowerManager.WakeLock mWakeLock;
	private LocationManager mLocationMgr;
	private Vibrator mVibrator;

	private SessionPersistence mPersistence = new SessionPersistence(this);
	private TrackerService mService;
	private boolean mBound;	// whether bound to service
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLocationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		setContentView(R.layout.main);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);

		mDistanceView = (TextView)findViewById(R.id.distance);
		mTimerView = (TextView)findViewById(R.id.time);
		mPaceView = (TextView)findViewById(R.id.pace);
		mAveragePaceView = (TextView)findViewById(R.id.averagePace);
		
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
		}
		return super.onOptionsItemSelected(item);
	}

	public void onSwitchViewClicked(View view) {
		// TODO später animiert machen
		ViewAnimator container = (ViewAnimator)findViewById(R.id.container);
		int displayed = container.getDisplayedChild();
		container.setDisplayedChild(1-displayed);
		if (displayed == 0) { // previously displayed
			((Button)view).setText("<- Clock");
		} else {
			((Button)view).setText("Map ->");
		}
	}
	
	public void onShowMapClicked(View view) {
		// TODO später animiert machen
		ViewAnimator container = (ViewAnimator)findViewById(R.id.container);
		container.setDisplayedChild(1);
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
		mWaitingDialog = ProgressDialog.show(this, "", "waiting for better fix...", true, true, new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				mService.stopTracking();
				mService.reset();
			}
		});
	}

	public void onStartClicked(View view) {
		Log.d(TAG, "onStartClicked begin");
		mStartButton.setVisibility(View.GONE);
		mPauseButton.setVisibility(View.VISIBLE);
		mStopButton.setVisibility(View.VISIBLE);
		mService.startRecording();
		mWakeLock.acquire();										// standby verhindern
		Log.d(TAG, "onStartClicked done");
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
			Session session = new Session(Util.createUniqueName(), "notizen", System.currentTimeMillis(), mService.elapsedTimeMillis, mService.pathLength, mService.positions);
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
		}
	}

	/** 
	 * zoom/move map so that start and end point are visible.
	 * based on http://stackoverflow.com/questions/7513247/how-to-zoom-a-mapview-so-it-always-includes-two-geopoints
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

		//double currentLatitudeSpan = (double)mMapView.getLatitudeSpan();
		//double currentLongitudeSpan = (double)mMapView.getLongitudeSpan();

		//double ratio = currentLongitudeSpan/currentLatitudeSpan;
		//if(longitudeSpan < (double)(latitudeSpan+2E7) * ratio){
		//    longitudeSpan = ((double)(latitudeSpan+2E7) * ratio);
		//}
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
		Log.d(TAG, "onLocationChanged begin");
		if (mWaitingDialog != null && mWaitingDialog.isShowing() && Util.isOKforStart(location)) {
			mWaitingDialog.dismiss();
			mNewSessionButton.setVisibility(View.GONE);
			mStartButton.setVisibility(View.VISIBLE);
			//mVibrator.vibrate(50); //vibrieren damit User weiss dass er jetzt starten kann (ohne immer hinsehen zu müssen)
			//mVibrator.cancel();
		}		
		
		mMyLocationOverlay.setLocation(mService.currentLocation);
		mPathOverlay.setPositions(mService.positions);
		if (location != null) {
			mMapView.getController().animateTo(Util.locationToGeoPoint(location));
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
}