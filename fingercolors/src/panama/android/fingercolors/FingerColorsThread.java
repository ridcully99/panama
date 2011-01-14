/*
 *  Copyright 2011 Robert Brandner (robert.brandner@gmail.com) 
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/**
 * 
 * @author ridcully
 */
public class FingerColorsThread extends Thread {
	
    private static final float TOUCH_TOLERANCE = 4;
	
	
	/** Handle to the application context, used to e.g. fetch Drawables. */
	private Context mContext;

    /** Message handler used by thread to interact with TextView */
    private Handler mHandler;
    
    /** Handle to the surface manager object we interact with */
    private SurfaceHolder mSurfaceHolder;
    
	private boolean mRun = false;

    private int mCanvasHeight = 1;
    private int mCanvasWidth = 1;	
    
    private Bitmap mBitmap = null;
    private Canvas mCanvas = null;
    private Paint mPaint = null;
    private int mPaperColor = Color.LTGRAY;
    private int mAlpha = 255;
    private int mRed = 0;
    private int mGreen = 0;
    private int mBlue = 0;
    private float prevX = -1;
    private float prevY = -1;
    private int mCurrAlpha = mAlpha;
    
	public FingerColorsThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
		// get handles to some important objects
		mSurfaceHolder = surfaceHolder;
		mHandler = handler;
		mContext = context;
		
		mPaint = new Paint();
		mPaint.setColor(Color.argb(mAlpha, mRed, mGreen, mBlue));
		mPaint.setStyle(Style.STROKE);
		mPaint.setStrokeWidth(30);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setAntiAlias(true);
	}

	@Override
	public void run() {
		while (mRun) {
			Canvas c = null;
			try {
				c = mSurfaceHolder.lockCanvas(null);
				if (c != null && mBitmap != null) {
					synchronized (mSurfaceHolder) {
						c.drawBitmap(mBitmap, 0, 0, null);
					}
				}
			} finally {
				if (c != null) {
					mSurfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
	}
	
	
	/**
	 * Used to signal the thread whether it should be running or not. Passing
	 * true allows the thread to run; passing false will shut it down if it's
	 * already running. Calling start() after this was most recently called with
	 * false will result in an immediate shutdown.
	 * 
	 * @param b
	 *            true to run, false to shut down
	 */
	public void setRunning(boolean b) {
		mRun = b;
	}

	/* Callback invoked when the surface dimensions change. */
	public void setSurfaceSize(int width, int height) {
		if (mCanvasWidth == 1) {
			// synchronized to make sure these all change atomically
			synchronized (mSurfaceHolder) {
				mCanvasWidth = width;
				mCanvasHeight = height;
				mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				mBitmap.eraseColor(mPaperColor);
				mCanvas = new Canvas(mBitmap);
			}
		}
	}
	
	public boolean handleTouchEvent(MotionEvent e) {
		if (mCanvas == null) {
			return true;
		}
		final float x = e.getX();
		final float y = e.getY();
		int action = e.getAction();
		if (action == MotionEvent.ACTION_DOWN) {
			mCurrAlpha = mAlpha;
			mPaint.setColor(Color.argb(mCurrAlpha, mRed, mGreen, mBlue));
			mCanvas.drawPoint(x, y, mPaint);
			prevX = x;
			prevY = y;
		}
		if (prevX != -1) {
			int dist = (int)Math.sqrt((double)((x-prevX)*(x-prevX)+(y-prevY)*(y-prevY)));
			mCurrAlpha -= Math.min(mCurrAlpha, dist);
			mPaint.setColor(Color.argb(mCurrAlpha, mRed, mGreen, mBlue));
			mCanvas.drawLine(prevX, prevY, x, y, mPaint);
		}
		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			// reset
			prevX = -1;
			prevY = -1;
		} else {
			prevX = x;
			prevY = y;
		}
		return true;
	}
	
	public void setColor(int a, int r, int g, int b) {
		mAlpha = a;
		mRed = r;
		mGreen = g;
		mBlue = b;
	}
}
