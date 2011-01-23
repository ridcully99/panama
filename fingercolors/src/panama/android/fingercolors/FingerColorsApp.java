package panama.android.fingercolors;

import android.app.Activity;
import android.os.Bundle;

public class FingerColorsApp extends Activity {

	public final static String LOG_TAG = "FingerColors";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}
}