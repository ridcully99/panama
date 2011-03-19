/*
 * Copyright 2011 Robert Brandner (robert.brandner@gmail.com)
 */
package panama.android.fingercolors;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class Main extends Activity implements OnSeekBarChangeListener {

	public final static String LOG_TAG = "fingercolors";
	public final static int DIALOG_BACKGROUND_ID = 1;
	public final static int DIALOG_HELP_ID       = 2;
	public final static int SELECT_IMAGE         = 1;
	public final static String PREFS_SHOW_HELP_AT_STARTUP = "showHelpAtStartup";
	public final static int INITIAL_BRUSH_SIZE = 16;

	private final static SimpleDateFormat mDateFormat = new SimpleDateFormat("yyMMdd-HHmmss");
	
	private CanvasView mCanvas;
	private ViewGroup mColorPalette;
	private ViewGroup mBrushPalette;
	private ViewGroup mTransparencyPalette;
	private View mBrushBtn;
	private View mTransparencyBtn;
	private View mPaletteBtn;
	private SeekBar mBrushSizeSlider;
	private SeekBar mTransparencySlider;
	private Dialog mBackgroundDialog;
	
	private OnTouchListener mMyBtnListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
        	if (event.getAction() == MotionEvent.ACTION_DOWN) {
        		boolean wasPressed = v.isPressed();
        		hidePalettes(); 	// also unpresses all buttons
        		if (wasPressed) {
        			return true;	// if buttons was pressed, the user pressed it again to hide the palette, which is done now.
        		}
        		// a previously not pressed button was touched.
        		// mark it pressed and show it's palette (or enable color pick mode)
        		v.setPressed(true);
        		int id = v.getId();
        		switch(id) {
    			case R.id.paletteBtn:
					mCanvas.enableColorPickMode(true);
					mColorPalette.setVisibility(View.VISIBLE);
					mColorPalette.bringToFront();
					mColorPalette.requestFocus();
    				break;
    			case R.id.brushBtn:
					mBrushPalette.setVisibility(View.VISIBLE);
					mBrushPalette.bringToFront();
					mBrushPalette.requestFocus();
    				break;
    			case R.id.transparencyBtn:
					mTransparencyPalette.setVisibility(View.VISIBLE);
					mTransparencyPalette.bringToFront();
					mTransparencyPalette.requestFocus();
    				break;
        		}
        	}
            return true;
        }
    }; 
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,  WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.main);
		mCanvas              = (CanvasView)findViewById(R.id.canvas);
		mColorPalette        = (ViewGroup)findViewById(R.id.colorPalette);
		mBrushPalette        = (ViewGroup)findViewById(R.id.brushPalette);
		mTransparencyPalette = (ViewGroup)findViewById(R.id.transparencyPalette);
		mBrushBtn            = findViewById(R.id.brushBtn);
		mTransparencyBtn     = findViewById(R.id.transparencyBtn);
		mPaletteBtn          = findViewById(R.id.paletteBtn);
		mBrushSizeSlider     = (SeekBar)findViewById(R.id.brushSizeSlider);
		mTransparencySlider  = (SeekBar)findViewById(R.id.transparencySlider); 
		mBrushSizeSlider.setOnSeekBarChangeListener(this);
		mTransparencySlider.setOnSeekBarChangeListener(this);
		mBrushSizeSlider.setProgress(INITIAL_BRUSH_SIZE);	// also sets brushSize in CanvasView
		mBrushBtn.setOnTouchListener(mMyBtnListener);
		mTransparencyBtn.setOnTouchListener(mMyBtnListener);
		mPaletteBtn.setOnTouchListener(mMyBtnListener);
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
	    switch(id) {
	    case DIALOG_BACKGROUND_ID:
		    Dialog dialog = new Dialog(this);
	    	mBackgroundDialog = dialog;
	    	dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    	dialog.setContentView(R.layout.new_dialog);
	    	GridView gv = (GridView)dialog.findViewById(R.id.backgroundsGrid);
			DisplayMetrics metrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);
	    	gv.setAdapter(new BackgroundsAdapter(this, metrics.widthPixels, metrics.heightPixels));
	    	gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					int color = BackgroundsAdapter.color(position);
					if (color == BackgroundsAdapter.PICK_IMAGE) {
						Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
						intent.setType("image/*");		// images only, no videos ...
						startActivityForResult(intent, SELECT_IMAGE);
					} else {
						mCanvas.reset(color);
						mBackgroundDialog.dismiss();
					}
				}
			});
	    	return dialog;
	    case DIALOG_HELP_ID:
	    	AlertDialog.Builder builder;
	    	AlertDialog helpDialog;
	    	
	    	LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
	    	View layout = inflater.inflate(R.layout.help_dialog, (ViewGroup) findViewById(R.id.helpDialog));

	    	WebView wv = (WebView)layout.findViewById(R.id.helpWebView);
	    	final CheckBox cb = (CheckBox)layout.findViewById(R.id.showHelpOnStart);
	    	try {
		    	InputStream is = getResources().openRawResource(R.raw.minihelp);
		    	BufferedReader r = new BufferedReader(new InputStreamReader(is, "utf-8"));
		    	StringBuilder txt = new StringBuilder();
		    	String line;
		    	while ((line = r.readLine()) != null) {
		    	    txt.append(line);
		    	}
		    	// base-url needed to be able to reference to assets-images in the html
		    	wv.loadDataWithBaseURL("fake://not/needed", txt.toString(), "text/html", "utf-8", "");
	    	} catch (IOException e) {
	    		wv.loadData(getResources().getString(R.string.error_reading_help), "text/plain", "utf-8");
	    	}
	    	
	    	builder = new AlertDialog.Builder(this);
	    	builder.setInverseBackgroundForced(true);
	    	builder.setView(layout);
	    	builder.setTitle(R.string.help_dialog_title);
	    	builder.setIcon(R.drawable.ic_launcher);
	    	builder.setNeutralButton(R.string.ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SharedPreferences settings = getSharedPreferences(Main.class.getName(), MODE_PRIVATE);
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean(PREFS_SHOW_HELP_AT_STARTUP, cb.isChecked());
					editor.commit();
					dialog.dismiss();
				}
			});
	    	helpDialog = builder.create();	    	
	    	
	    	return helpDialog;
	    }
	    return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		if (id == DIALOG_HELP_ID) {
			SharedPreferences settings = getSharedPreferences(Main.class.getName(), MODE_PRIVATE);
	    	CheckBox cb = (CheckBox)dialog.findViewById(R.id.showHelpOnStart);
	    	cb.setChecked(settings.getBoolean(PREFS_SHOW_HELP_AT_STARTUP, true));
		}		
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
			case R.id.miSave:
				saveImage();
				return true;
			case R.id.miHelp:
				showDialog(DIALOG_HELP_ID);
				return true;
		}
		return false;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		int size;
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			size = mBrushSizeSlider.getProgress();
			size *= 2;
			if (size == 0) {
				size = 1;
			}
			mBrushSizeSlider.setProgress(size);
			mCanvas.setBrushSize(mBrushSizeSlider.getProgress());	// necessary to do this too, to show the BrushToast
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			size = mBrushSizeSlider.getProgress();
			mBrushSizeSlider.setProgress(size/2);
			mCanvas.setBrushSize(mBrushSizeSlider.getProgress());	// necessary to do this too, to show the BrushToast
			return true;
		case KeyEvent.KEYCODE_BACK:
			mCanvas.undo();
			return true;
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
	
	public void hidePalettes() {
		mColorPalette.setVisibility(View.GONE);
		mBrushPalette.setVisibility(View.GONE);
		mTransparencyPalette.setVisibility(View.GONE);
		mBrushBtn.setPressed(false);
		mTransparencyBtn.setPressed(false);
		mPaletteBtn.setPressed(false);
    	mCanvas.enableColorPickMode(false);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SELECT_IMAGE) {
		    if (resultCode == Activity.RESULT_OK) {
		    	try {
			    	loadImage(data);
		    	} catch(Exception e) {
		    		Toast.makeText(this, getResources().getString(R.string.error_reading_image), Toast.LENGTH_SHORT).show();
		    	} finally {
		    		mBackgroundDialog.dismiss();
		    	}
		    } 
		}
	}

	private void loadImage(Intent data) throws Exception {
    	Uri imageUri = data.getData();
    	InputStream imageStream = null;
		imageStream = getContentResolver().openInputStream(imageUri);
    	BitmapFactory.Options options = new BitmapFactory.Options();
    	options.inJustDecodeBounds = true;
    	BitmapFactory.decodeStream(imageStream, null, options);
    	if (options.outWidth == -1) {
    		throw new Exception();
    	}
    	options.inJustDecodeBounds = false;
    	int width = options.outWidth;
    	int height = options.outHeight;
    	if (width > height) {	// landscape? swap width & height for sampleSize calculation
    		int tmp = width;
    		width = height;
    		height = tmp;
    	}
    	int displayWidth = mCanvas.getWidth();
    	int displayHeight = mCanvas.getHeight();
    	int sampleX = width/displayWidth;
    	int sampleY = height/displayHeight;
    	int sampleSize = Math.min(sampleX, sampleY);
    	// find largest power of 2, smaller than sampleSize
    	options.inSampleSize = 1;
    	while (options.inSampleSize*2 < sampleSize) {
    		options.inSampleSize *= 2;
    	}
    	imageStream.close();
    	imageStream = getContentResolver().openInputStream(imageUri);
    	Bitmap bm = BitmapFactory.decodeStream(imageStream, null, options);
    	imageStream.close();
		mCanvas.reset(bm);
	}
	
	// based on http://developer.android.com/reference/android/os/Environment.html
	private void saveImage() {
		String fileName = "fingercolors-"+mDateFormat.format(new Date())+".png";
		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
	    File file = new File(path, fileName);
		try {
			path.mkdirs();	// make sure the Pictures folder exists.
			BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(file));
			boolean success = mCanvas.saveBitmap(outStream);
			outStream.flush();
			outStream.close();
			if (success) {
				String msg = getResources().getString(R.string.image_saved_as, file.getAbsolutePath());
				Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
				// Tell the media scanner about the new file so that it is
				// immediately available to the user.
				MediaScannerConnection.scanFile(this,
						new String[] { file.toString() }, null,
						new MediaScannerConnection.OnScanCompletedListener() {
							public void onScanCompleted(String path, Uri uri) {
							}
						});
			} else {
				throw new Exception("canvas.saveBitmap did not succeed.");
			}
		} catch (Exception e) {
			Toast.makeText(this, getResources().getString(R.string.error_saving_image), Toast.LENGTH_LONG).show();
		}
	}

	/* -- OnSeekBarListener methods ------------------------------------------------------------------------------ */
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		switch (seekBar.getId()) {
		case R.id.brushSizeSlider:
			mCanvas.setBrushSize(progress, fromUser);
			break;
		case R.id.transparencySlider:
			mCanvas.setAlpha(progress);
			break;
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		hidePalettes();
	}
}