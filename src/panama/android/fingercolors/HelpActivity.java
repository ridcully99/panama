/*
 *  Copyright 2011 Robert Brandner (robert.brandner@gmail.com) 
 */
package panama.android.fingercolors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebView;

/**
 * @author ridcully
 *
 */
public class HelpActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,  WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.help);
    	WebView wv = (WebView)findViewById(R.id.helpWebView);
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
	}
}
