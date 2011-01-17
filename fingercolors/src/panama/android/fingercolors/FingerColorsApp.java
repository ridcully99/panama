package panama.android.fingercolors;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class FingerColorsApp extends Activity {

	public final static String LOG_TAG = "FingerColors";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//setContentView(new FingerColorsView(this));
	}

	@Override
	public void onBackPressed() {
		FingerColorsView sv = (FingerColorsView)findViewById(R.id.canvas);
		sv.clear();
		//sv.getThread().setColor(255, (int)(Math.random()*255), (int)(Math.random()*255), (int)(Math.random()*255));
	}
	
//	@Override
//	public boolean onTrackballEvent(MotionEvent event) {
//		Log.i(LOG_TAG, "trackballevent in MainApp");
//		return super.onTrackballEvent(event);
//	}
//	
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//    	Log.i(FingerColorsApp.LOG_TAG, "keydown-event in MainApp");
//    	return super.onKeyDown(keyCode, event);
//    }	
}