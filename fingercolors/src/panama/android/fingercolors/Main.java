package panama.android.fingercolors;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class Main extends Activity {

	public final static String LOG_TAG = "FingerColors";
	public final static int COLOR_DIALOG_ID = 1;
	public final static int TOOL_DIALOG_ID = 2;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.miColor:
	    	showDialog(COLOR_DIALOG_ID);
	        return true;
	    case R.id.miTool:
	    	showDialog(TOOL_DIALOG_ID);
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
	    switch(id) {
		    case COLOR_DIALOG_ID:
		    	dialog = createColorDialog();
		        break;
		    case TOOL_DIALOG_ID:
		    	dialog = createToolDialog();
		        break;
		    default:
		        dialog = null;
		    }
	    return dialog;
	}
	
	private Dialog createColorDialog() {
		AlertDialog.Builder builder;
		AlertDialog colorDialog;

		Context mContext = this;	// das geht nicht --> getApplicationContext();
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.color_dialog,
		                               (ViewGroup) findViewById(R.id.layout_root));
		builder = new AlertDialog.Builder(mContext);
		builder.setView(layout);
		colorDialog = builder.create();		
		return colorDialog;
	}
	
	private Dialog createToolDialog() {
		return null;
	}
	
	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		Log.i(LOG_TAG, "Main on Trackball Event");
		View paletteView = this.findViewById(R.id.palette);
		paletteView.setVisibility(View.VISIBLE);
		return true;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.i(LOG_TAG, "Main on Keydown Event");
		View paletteView = this.findViewById(R.id.palette);
		View toolsView = this.findViewById(R.id.tools);
		if (paletteView.getVisibility() == View.INVISIBLE) {
			paletteView.setVisibility(View.VISIBLE);
			toolsView.setVisibility(View.VISIBLE);
		} else {
			paletteView.setVisibility(View.INVISIBLE);
			toolsView.setVisibility(View.INVISIBLE);
		}
		return true;
	}
}