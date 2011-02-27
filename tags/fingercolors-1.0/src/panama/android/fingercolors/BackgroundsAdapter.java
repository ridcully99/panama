/*
 * Copyright 2011 Robert Brandner (robert.brandner@gmail.com)
 */
package panama.android.fingercolors;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;

/**
 * @author ridcully
 *
 */
public class BackgroundsAdapter extends BaseAdapter {

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
	private int 	mHeight;
	
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
        	Drawable galleryIcon = mContext.getResources().getDrawable(R.drawable.ic_gallery);
        	view.setBackgroundDrawable(galleryIcon);
        } else {
        	view.setBackgroundColor(color(position));
        }
        return view;
	}

}
