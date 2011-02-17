/*
 *  Copyright 2004-2010 Robert Brandner (robert.brandner@gmail.com) 
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License. 
 *  You may obtain a copy of the License at 
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *  
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License. 
 */
package panama.android.fingercolors;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;

/**
 * @author ridcully
 *
 */
public class BackgroundsAdapter extends BaseAdapter {

//	private final static int[] BACKGROUND_COLORS = new int [] {
//		Color.WHITE, 
//		Color.LTGRAY,
//		Color.DKGRAY,
//		Color.BLACK,
//		
//		0xFF8080FF,
//		0xFF80C0FF,
//		0xFF80FFFF,
//		0xFF80FFC0,
//		
//		0xFF80FF80,
//		0xFFC0FF80,
//		0xFFFFFF80,
//		0xFFFFC080,
//
//		0xFFFF8080,
//		0xFFFF80C0,
//		0xFFFF80FF,
//		0xFFC080FF,
//
//		Color.TRANSPARENT	// indicator for image selection
//	};

	private final static int[] BACKGROUND_COLORS = new int [] {
		Color.WHITE, 
		Color.LTGRAY,
		Color.DKGRAY,
		Color.BLACK,
		
		0xFF6060C0,
		0xFF6090C0,
		0xFF60C0C0,
		0xFF60C090,
		
		0xFF60C060,
		0xFF90C060,
		0xFFC0C060,
		0xFFC09060,

		0xFFC06060,
		0xFFC06090,
		0xFFC060C0,
		0xFF9060C0,

		Color.TRANSPARENT	// indicator for image selection
	};
	
	private Context mContext;
	private int mWidth;
	private int mHeight;
	
	public BackgroundsAdapter(Context context, int w, int h) {
		mContext = context;
		mHeight = (int)(h*0.66/5.0);	// compute height for one color patch depending on total screen height
	}
	
	/* (non-Javadoc)
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		return BACKGROUND_COLORS.length;
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int position) {
		return null;
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int position) {
		return 0;
	}

	public static int color(int position) {
        float[] hsv = new float[3];
    	Color.colorToHSV(BACKGROUND_COLORS[position], hsv);
    	hsv[1] *= 0.5;	// decrease saturation
    	Log.i("fingercolors", position+"->"+Color.HSVToColor(hsv));
		return BACKGROUND_COLORS[position];
	}
	
	/* (non-Javadoc)
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            view = new View(mContext);
            view.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.FILL_PARENT, mHeight));
            view.setPadding(0, 0, 0, 0);
        } else {
            view = (View) convertView;
        }
        if (BACKGROUND_COLORS[position] == Color.TRANSPARENT) {
        	Drawable galleryIcon = mContext.getResources().getDrawable(R.drawable.ic_menu_slideshow);
        	view.setBackgroundDrawable(galleryIcon);
        } else {
        	view.setBackgroundColor(color(position));
        	//imageView.setColorFilter(color, mode);
        }
        return view;
	}

}
