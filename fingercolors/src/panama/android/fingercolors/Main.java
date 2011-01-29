package panama.android.fingercolors;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

public class Main extends Activity {

	public final static String LOG_TAG = "FingerColors";
	public final static int COLOR_DIALOG_ID = 1;
	public final static int TOOL_DIALOG_ID = 2;
	
	private CanvasView mCanvas;
	private PaletteView mPalette;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mCanvas = (CanvasView)findViewById(R.id.canvas);
		mPalette = (PaletteView)findViewById(R.id.palette);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		Log.i(LOG_TAG, "Main on Trackball Event: x="+event.getX()+", "+event.getY());
		float x = event.getX();
		float y = event.getY();
		if (x > 0) {
			mCanvas.increaseAlpha((int)(255*x)/10);
		}
		if (x < 0) {
			mCanvas.decreaseAlpha((int)(-255*x)/10);
		}
		if (y > 0) {
			mCanvas.decreaseBrightness((int)(255*y)/10);
		}
		if (y < 0) {
			mCanvas.increaseBrightness((int)(-255*y)/10);
		}
		return false;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP:
				mCanvas.increaseBrushSize();
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				mCanvas.decreaseBrushSize();
				return true;
			case KeyEvent.KEYCODE_DPAD_CENTER:
				mPalette.setVisibility(mPalette.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
				return true;
			default:;
		}
		return false;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				return true;	// avoid changing any sound volumes
		}
		return false;
	}
}