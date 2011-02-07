package panama.android.fingercolors;

import java.io.InputStream;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

public class Main extends Activity implements OnClickListener {

	public final static String LOG_TAG = "fingercolors";
	public final static int DIALOG_BACKGROUND_ID = 1;
	public final static int SELECT_IMAGE = 1;
	
	private CanvasView mCanvas;
	private PaletteView mPalette;
	private Dialog mBackgroundDialog;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mCanvas = (CanvasView)findViewById(R.id.canvas);
		mPalette = (PaletteView)findViewById(R.id.palette);
		
		//((Button)findViewById(R.id.paletteBtn)).setOnClickListener(this);
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
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
	    Dialog dialog;
	    switch(id) {
	    case DIALOG_BACKGROUND_ID:
	    	dialog = new Dialog(this);
	    	mBackgroundDialog = dialog;
	    	dialog.setContentView(R.layout.new_dialog);
	    	dialog.setTitle(R.string.new_dialog_title);
	    	GridView gv = (GridView)dialog.findViewById(R.id.backgroundsGrid);
	    	gv.setAdapter(new BackgroundsAdapter(this, mCanvas.getWidth(), mCanvas.getHeight()));
	    	gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					int color = BackgroundsAdapter.BACKGROUND_COLORS[position];
					if (color == Color.TRANSPARENT) {
						Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
						intent.setType("image/*");	// images only, no videos ...
						startActivityForResult(intent, SELECT_IMAGE);
					} else {
						mCanvas.reset(color);	// TODO ... add selecting color or picture here.
						mBackgroundDialog.dismiss();
					}
				}
			});
	    	break;
	    default:
	        dialog = null;
	    }
	    return dialog;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.miNew:
				showDialog(DIALOG_BACKGROUND_ID);
				return true;
			case R.id.miRedo:
				mCanvas.redo();
				return true;
		}
		return false;
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
				if (mPalette.getVisibility() == View.VISIBLE) {
					mPalette.setVisibility(View.INVISIBLE);
				}else {
					mPalette.requestFocus();
					mPalette.setVisibility(View.VISIBLE);
					mPalette.bringToFront();
				}
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
			case KeyEvent.KEYCODE_DPAD_CENTER:
				if (mPalette.getVisibility() == View.VISIBLE) {
					mPalette.requestFocus();
					mPalette.bringToFront();					
				}
		}
		return false;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SELECT_IMAGE) {
		    if (resultCode == Activity.RESULT_OK) {
		    	Uri imageUri = data.getData();
		    	InputStream imageStream = null;
		    	try {
		    		imageStream = getContentResolver().openInputStream(imageUri);
			    	BitmapFactory.Options options = new BitmapFactory.Options();
			    	options.inSampleSize = 2;	// MUST down-size as photos are so large we get an OutOfMemory Error otherwise
			    	Bitmap bm = BitmapFactory.decodeStream(imageStream, null, options);
			    	imageStream.close();
		    		mCanvas.reset(bm);
		    		mBackgroundDialog.dismiss();
		    	} catch(Exception e) {
		    		Toast.makeText(this, "failed to open image", Toast.LENGTH_SHORT).show();
		    		return;
		    	}
		    } 
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
//			case R.id.paletteBtn:
//				if (mPalette.getVisibility() == View.VISIBLE) {
//					mPalette.setVisibility(View.INVISIBLE);
//				} else {
//					mPalette.requestFocus();
//					mPalette.setVisibility(View.VISIBLE);
//					mPalette.bringToFront();
//				}
		}
	}
}