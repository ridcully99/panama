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
public class FingerColorsThreadWithPaths extends Thread {
	
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
    private Bitmap mBitmapTemp = null;
    private Canvas mCanvas = null;
    private Canvas mCanvasTemp = null;
    private Paint mPaint = null;
    private int mPaperColor = Color.LTGRAY;
    private int mAlpha = 255;
    private int mRed = 0;
    private int mGreen = 0;
    private int mBlue = 0;
    private Path mPath = new Path();
    private float[] mPathX = new float[100];
    private float[] mPathY = new float[100];
    private int[] mSegmentAlpha = new int[100];		// alpha to use for segment
    private int mVertexCount = 0;
    private int mPathLength = 0;
    private boolean mPainting = false;
    
	public FingerColorsThreadWithPaths(SurfaceHolder surfaceHolder, Context context, Handler handler) {
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
				if (c != null && mBitmapTemp != null && !mPainting) {
					synchronized (mSurfaceHolder) {
						c.drawBitmap(mBitmapTemp, 0, 0, null);
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
				mBitmapTemp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				mBitmap.eraseColor(mPaperColor);
				mBitmapTemp.eraseColor(mPaperColor);
				mCanvas = new Canvas(mBitmap);
				mCanvasTemp = new Canvas(mBitmapTemp);
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
			//mAlpha = 255;
			//mCanvas.drawPoint(x, y, mPaint);
			//prevX = x;
			//prevY = y;
		}
//		if (prevX != -1) {
//			// mix
////			final int currColor = mBitmap.getPixel((int)x, (int)y);
////			final int r = Color.red(currColor);
////			final int g = Color.green(currColor);
////			final int b = Color.blue(currColor);
//			int dist = (int)Math.sqrt((double)((x-prevX)*(x-prevX)+(y-prevY)*(y-prevY)));
//			mAlpha -= Math.min(mAlpha, dist);
//			mPaint.setColor(Color.argb(mAlpha, mRed, mGreen, mBlue));
//			mCanvas.drawLine(prevX, prevY, x, y, mPaint);
//		}
		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			// swap bitmap and canvas
			Bitmap bm = mBitmap;
			Canvas cn = mCanvas;
			mBitmap = mBitmapTemp;
			mCanvas = mCanvasTemp;
			mBitmapTemp = bm;
			mCanvasTemp = cn;
			
			// reset
			mVertexCount = 0;
			mPathLength = 0;
		} else {
			if ((mVertexCount == 0 || x != mPathX[mVertexCount] || y != mPathY[mVertexCount]) && mPathLength < mAlpha) {	// only if first point or there was really a measurable movement and not path too long (longer than mAlpha)
				mPathX[mVertexCount] = x;
				mPathY[mVertexCount] = y;
				if (mVertexCount > 0) {
					final double dx = mPathX[mVertexCount] - mPathX[mVertexCount-1];
					final double dy = mPathY[mVertexCount] - mPathY[mVertexCount-1];
					int length = (int)Math.sqrt(dx*dx+dy*dy);
					mPathLength += length;
					if (mVertexCount == 1) {
						mSegmentAlpha[0] = mAlpha;	// at first, the first segment has alpha of original color
					} else {
						mSegmentAlpha[mVertexCount-1] = mSegmentAlpha[mVertexCount-2];	// later segments first get alpha of previous segment
						for (int i = mVertexCount-1; i>=0; i--) {						// and going back alpha is reduced depending on length, thus the first segment gets lighter and lighter which is okay as it will be painted very often later on.
							mSegmentAlpha[i] -= length;
							if (mSegmentAlpha[i] < 1) {
								mSegmentAlpha[i] = 1;
							}
						}
					}
				}
				mVertexCount++;
				drawPath();
			}
		}
		return true;
	}
	
	private void drawPath() {
		mPainting = true;
		mCanvasTemp.drawBitmap(mBitmap, 0, 0, null);	// start with image as of before beginning of stroke
		if (mVertexCount == 0) {
			return;
		} else if (mVertexCount == 1) {
			mPaint.setColor(Color.argb(mAlpha, mRed, mGreen, mBlue));
			mCanvasTemp.drawPoint(mPathX[0], mPathY[0], mPaint);
		} else {
			mPath.reset();
			mPath.moveTo(mPathX[0], mPathY[0]);
			for (int i = 1; i < mVertexCount; i++) {
				mPaint.setColor(Color.argb(mSegmentAlpha[i-1], mRed, mGreen, mBlue));
				mPath.lineTo(mPathX[i], mPathY[i]);
				mCanvasTemp.drawPath(mPath, mPaint);
			}
		}
		mPainting = false;
	}
	
	public void setColor(int a, int r, int g, int b) {
		mAlpha = a;
		mRed = r;
		mGreen = g;
		mBlue = b;
	}
}
