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

import java.io.BufferedOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.MaskFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Bitmap.CompressFormat;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * @author ridcully
 *
 */
public class CanvasView extends View {
	
	private final static int BLUR_FACTOR = 16;	/* size/BLUR_FACTOR */
	private final static int UNDO_BUFFERS = 5;
	
    private Bitmap  	mBitmap;
    private Canvas  	mCanvas;
    private Path    	mPath;
    private MaskFilter	mBlurFilter;
    private Paint   	mBitmapPaint;
    private Paint		mPaint;
    private BrushView 	mBrushView;
    private Toast		mBrushToast;
    
    private int 		mColor = 0xFF000000;
    private int[]		mColorRGB = new int[] {0, 0, 0};	// original RGB values of color, used to change brightness
    private int			mAlpha = 255;
    private int 		mSize = 16;
    private float		mBlur = 0.2f;	// factor to multiply mSize with for blur-radius	
    private int			mBrightness = 0;
    private Rect 		mDirtyRegion = new Rect(0,0,0,0);
    private int			mLastX = -1;
    private int			mLastY = -1;
    
    private Bitmap[]	mUndoBitmaps = new Bitmap[UNDO_BUFFERS];
    private int			mUndoNewest = 0;
    private int			mUndoCurrent = 0; 
    private int			mUndoOldest = 0;
    
    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPath = new Path();
        mBitmapPaint = new Paint();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(mSize);
        setBlur();

        LayoutInflater inflater = ((Main)context).getLayoutInflater();
        View layout = inflater.inflate(R.layout.brushtoast, (ViewGroup) findViewById(R.id.brushtoast_layout_root));        
        
    	mBrushToast = new Toast(this.getContext());
    	mBrushToast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL, 0, -40);
    	mBrushToast.setDuration(Toast.LENGTH_SHORT);
        mBrushToast.setView(layout);
        
        mBrushView = (BrushView)layout.findViewById(R.id.brushview);
        mBrushView.setPaint(mPaint);
        
        setFocusableInTouchMode(true);	
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recycleBitmap(mBitmap);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        for (int i=0; i<mUndoBitmaps.length; i++) {
        	mUndoBitmaps[i] = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        }
        mCanvas = new Canvas(mBitmap);
        mBitmap.eraseColor(0xFFCCCCCC);
        remember();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
		mCanvas.drawBitmap(mUndoBitmaps[(mUndoCurrent-1)%UNDO_BUFFERS], 0, 0, mBitmapPaint);
		mCanvas.drawPath(mPath, mPaint);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				undo();
				return true;
		}
		return false;
	}
    
    
	@Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int ix = (int)x;
        int iy = (int)y;
        
        mBrushToast.cancel();

        switch (event.getAction()) {
        	case MotionEvent.ACTION_DOWN:
        		mPath.reset();
        		mPath.moveTo(x, y);
        		mPath.lineTo(x+0.5f, y+0.5f);
        		mDirtyRegion.set(ix, iy, ix, iy);
            	adjustDirtyRegion(ix, iy);
                invalidate(mDirtyRegion);
                mLastX = (int)x;
                mLastY = (int)y;
                break;
            case MotionEvent.ACTION_MOVE:
            	boolean redraw = false;	// only invalidate if actually something gets drawn
            	int historySize = event.getHistorySize();
            	for (int i = 0; i < historySize; i++) {
            		float historicalX = event.getHistoricalX(i);
            		float historicalY = event.getHistoricalY(i);
            		int ihx = (int)historicalX;
            		int ihy = (int)historicalY;
                    if (ihx != mLastX || ihy != mLastY) {
                    	mPath.lineTo(historicalX, historicalY);
                    	adjustDirtyRegion(ihx, ihy);
                    	redraw = true;
                    	mLastX = ihx;
                    	mLastY = ihy;
                    }
            	}
                if (ix != mLastX || iy != mLastY) {
                	mPath.lineTo(x, y);
                	adjustDirtyRegion(ix, iy);
                	redraw = true;
                    mLastX = (int)x;
                    mLastY = (int)y;
                }
                if (redraw) {
                	invalidate(mDirtyRegion);
                }
                break;
            case MotionEvent.ACTION_UP:
            	remember();
            	mCanvas.drawPath(mPath, mPaint);
            	mPath.reset();
                invalidate(mDirtyRegion);
                mLastX = mLastY = -1;
                break;
        }
        return true;
    }
    
    public void reset(int color) {
    	remember();
    	mBitmap.eraseColor(color);
    	remember();
    	invalidate();
    }
    
    public void reset(Bitmap bm) {
    	remember();
    	if (false || bm.getWidth() > bm.getHeight()) {	// landscape? rotate 
    		bm = Bitmap.createScaledBitmap(bm, mBitmap.getHeight(), mBitmap.getWidth(), true);
        	Matrix matrix = new Matrix();
        	matrix.postRotate(90f);
        	bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
    	} else {
    		bm = Bitmap.createScaledBitmap(bm, mBitmap.getWidth(), mBitmap.getHeight(), true);
    	}	
    	mCanvas.drawBitmap(bm, 0, 0, mBitmapPaint);
        bm.recycle();
        remember();
    	invalidate();
    }
    
    public void setColor(int a, int r, int g, int b) {
    	setColor(Color.argb(a, r, g, b));
    }
    
    public void setColor(int color) {
    	mColorRGB[0] = Color.red(color);
    	mColorRGB[1] = Color.green(color);
    	mColorRGB[2] = Color.blue(color);
    	mBrightness = brightness(mColorRGB);
    	mPaint.setColor(color);
    	mPaint.setAlpha(mAlpha);
    	mColor = mPaint.getColor();
    	toastBrush();
    }
    
    public void setBrushSize(int size) {
    	size = Math.max(2, size);
    	size = Math.min(mBitmap.getWidth()/4, size);
    	mSize = size;
    	mPaint.setStrokeWidth(size);
    	setBlur();
    	toastBrush();
    }
    
    public void increaseBrushSize() {
    	setBrushSize(mSize*2/*SIZE_STEP*/);
    }

    public void decreaseBrushSize() {
    	setBrushSize(mSize/2);
//    	if (mSize > SIZE_STEP) {
//    		setBrushSize(mSize-SIZE_STEP);
//    	} else {
//    		setBrushSize(0);
//    	}
    }
    
    public void increaseAlpha(int delta) {
    	mAlpha = mPaint.getAlpha()+delta;
    	mAlpha = Math.min(255, mAlpha);
    	mAlpha = Math.max(5, mAlpha);
    	mPaint.setAlpha(mAlpha);
    	mColor = mPaint.getColor();
    	toastBrush();
    }
    
    public void decreaseAlpha(int delta) {
    	increaseAlpha(-delta);
    }
    
    public void increaseBrightness(int delta) {
    	mBrightness += delta;
    	mBrightness = Math.min(mBrightness, 255);
    	mBrightness = Math.max(mBrightness, -255);
    	int[] rgb = new int[3];
    	for (int i=0; i< 3; i++) {
    		rgb[i] = mColorRGB[i] + mBrightness;
        	rgb[i] = Math.min(rgb[i], 255);
        	rgb[i] = Math.max(rgb[i], 0);
    	}
    	mPaint.setARGB(mAlpha, rgb[0], rgb[1], rgb[2]);
    	mColor = mPaint.getColor();
    	toastBrush();
    }
    
    public void decreaseBrightness(int delta) {
    	increaseBrightness(-delta);
    }
    
    private void toastBrush() {
    	mBrushView.requestLayout();
    	mBrushView.invalidate();
    	mBrushToast.show();
    }
    
    private void setBlur() {
    	if (mSize*mBlur < 1) {
    		mBlurFilter = null;
    		mPaint.setMaskFilter(null);
    	} else {
            mBlurFilter = new BlurMaskFilter(mSize*mBlur, BlurMaskFilter.Blur.NORMAL);
    		mPaint.setMaskFilter(mBlurFilter);
    	}
    }

    private void adjustDirtyRegion(int x, int y) {
    	int strokeWidthHalf = (int)(mSize/2f+mSize*mBlur)+4;	// Hälfte der Strichbreite + 2 zur Sicherheit wg. Antialias ... bei Blur usw. entsprechend erweitern
    	mDirtyRegion.left = Math.min(mDirtyRegion.left, x-strokeWidthHalf);
    	mDirtyRegion.right = Math.max(mDirtyRegion.right, x+strokeWidthHalf);
    	mDirtyRegion.top = Math.min(mDirtyRegion.top, y-strokeWidthHalf);
    	mDirtyRegion.bottom = Math.max(mDirtyRegion.bottom, y+strokeWidthHalf);
    }

    /**
     * remember for undo
     */
    private void remember() {
    	recycleBitmap(mUndoBitmaps[mUndoCurrent % UNDO_BUFFERS]);
    	mUndoBitmaps[mUndoCurrent % UNDO_BUFFERS] = Bitmap.createBitmap(mBitmap);
    	mUndoCurrent++;
    	if (mUndoOldest+UNDO_BUFFERS < mUndoCurrent) {
    		mUndoOldest++;
    	}
    	mUndoNewest = mUndoCurrent;
    }
    
    public void undo() {
		if (mUndoCurrent > mUndoOldest+1) {	// need one UNDO bitmap for proper painting
			mUndoCurrent--;
			//mCanvas.drawColor(mBackgroundColor);
			mCanvas.drawBitmap(mUndoBitmaps[mUndoCurrent%UNDO_BUFFERS], 0, 0, mBitmapPaint);
			invalidate();
		}
	}
    
    public void redo() {
		if (mUndoCurrent < mUndoNewest) {
			mUndoCurrent++;
			mCanvas.drawBitmap(mUndoBitmaps[mUndoCurrent%UNDO_BUFFERS], 0, 0, mBitmapPaint);
			invalidate();
		}
	}

	public boolean saveBitmap(BufferedOutputStream outStream) {
		return mBitmap.compress(CompressFormat.PNG, 100, outStream);
	}
    
    private void recycleBitmap(Bitmap bm) {
    	if (bm != null) {
    		bm.recycle();
    	}
    }
    
    // compute perceived brightness of a color in the range of 0 to 255
    // from http://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
    private int brightness(int[] rgb) {
    	return (int)FloatMath.sqrt(rgb[0]*rgb[0]*0.241f+
    						  rgb[1]*rgb[1]*0.691f+
    						  rgb[2]*rgb[2]*0.068f);
    }

}
