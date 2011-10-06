package panama.android.trackx;

import panama.android.trackx.TrackerService.LocalBinder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
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

public class MainActivity extends MapActivity implements TrackerService.Listener {

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

	private SessionPersistence mPersistence = new SessionPersistence(this);
	private TrackerService mService;
	private boolean mBound;	// whether bound to service
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Bind to TrackerService (asynchronous)
		Intent intent = new Intent(this, TrackerService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		
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

		View dataAreaLayout = findViewById(R.id.dataArea);	// a Layout is a View
		dataAreaLayout.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO groß/klein togglen (schön wäre mit Animation); 
				// TODO eventuell durch ändern des Style bei klein auch die Schrift kleiner machen 
				return false;
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		mPathOverlay = new PathOverlay();
		mMyLocationOverlay = new MyLocationOverlay();
		mMapView.getOverlays().add(mPathOverlay);
		mMapView.getOverlays().add(mMyLocationOverlay);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		//if (!mLocationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
		//	showDialog(DIALOG_ENABLE_GPS);
		//} else {
		//	if (sessionState == IDLE && !Util.isUpToDate(currentLocation)) {
		//		currentLocation = null;
		//		mWaitingDialog = ProgressDialog.show(this, "", "Finding current location...", true);
		//	}
		//}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (mBound && mService.sessionState == TrackerService.IDLE) {
			mService.stopTracking();
		}
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
			mService.reset();
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
		mService.startRecording();
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
		mService.stopRecording();
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}

	public void onDiscardClicked(View view) {
		mDiscardButton.setVisibility(View.GONE);
		mSaveButton.setVisibility(View.GONE);
		mStartButton.setVisibility(View.VISIBLE);
		mService.reset();
	}
	
	public void onSaveClicked(View view) {
		try {
			Session session = new Session(Util.createUniqueName(), "notizen", System.currentTimeMillis(), mService.timeMillis, mService.pathLength, mService.positions);
			mPersistence.save(session);
		} catch (Exception e) {
			Log.e("trackx", "saving failed", e);
			Toast.makeText(this, "saving failed: "+e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
// TODO eventuell wieder aktivieren
//		case REQUEST_ENABLING_GPS:
//			// see if user enabled GPS
//			if (!mLocationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//				showDialog(DIALOG_ENABLE_GPS);	// if not, once again ask to enable it.
//			}
			
		case REQUEST_LOAD_SESSION:
			if (resultCode == RESULT_OK) {
				long id = data.getLongExtra("id", -1);
				mService.reset();
				Session session = mPersistence.load(id);
				mService.setSession(session);
			}
		}
	}
	

	/** call from UI thread */
	private void setPathLength(float meters) {
		mDistanceView.setText(String.format("%.3f", meters/1000f));
		mDistanceView.invalidate();		
	}
	
	private void setPace(float metersPerSecond) {
		mPaceView.setText(Util.formatSpeed(metersPerSecond));
		mPaceView.invalidate();
	}

	private void setTime(long timeMillis) {
		mTimerView.setText(Util.formatTime(timeMillis));
		mTimerView.invalidate();
	}
	
	// -------------------------------------------------------------------------- TrackerService.Listener Implementation
	
	@Override
	public void onLocationChanged(Location location) {
		mMyLocationOverlay.setLocation(location);
		mPathOverlay.setPositions(mService.positions);
		mMapView.getController().animateTo(Util.locationToGeoPoint(location));
		mMapView.invalidate();		
		if (mService.sessionState == TrackerService.RUNNING) {
			setPathLength(mService.pathLength);
			setPace(mService.currentPace);
		}
	}
	
	@Override
	public void onTimerChanged(long timeMillis) {
		setTime(timeMillis);
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

	// ------------------------------------------------------------------------------------------------- service connection
	
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() { /* nice trick with anonymous class */

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.addListener(MainActivity.this);
    		mService.startTracking();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mBound = false;
        }
    };	

}