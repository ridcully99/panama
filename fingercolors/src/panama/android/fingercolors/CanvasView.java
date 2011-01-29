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
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
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
	private final static int UNDO_BUFFERS = 10;
	
    private Bitmap  	mBitmap;
    private Canvas  	mCanvas;
    private Path    	mPath;
    private MaskFilter	mBlur;
    private Paint   	mBitmapPaint;
    private Paint		mPaint;
    private BrushView 	mBrushView;
    private Toast		mBrushToast;
    
    private int			mPaperColor = 0xFFAAAAAA;
    
    private int 		mColor = 0xFF000000;
    private int			mAlpha = 255;
    private int 		mSize = 16;
    private Rect 		mDirtyRegion = new Rect(0,0,0,0);
    
    private Bitmap[]	mUndoBitmaps = new Bitmap[UNDO_BUFFERS];
    private int			mStep = 0, mOldestUndo = 0;
    
    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mBitmap = Bitmap.createBitmap(320, 480, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPath = new Path();
        mBitmapPaint = new Paint();

        mPaint = new Paint(Paint.DITHER_FLAG);
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
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(mPaperColor);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mPath, mPaint);
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				if (mStep > mOldestUndo) {
					mStep--;
					mCanvas.drawColor(mPaperColor);
					mCanvas.drawBitmap(mUndoBitmaps[mStep%UNDO_BUFFERS], 0, 0, mBitmapPaint);
					//mCanvas.drawLine(0,0, 100, 100*(mStep%UNDO_BUFFERS), mBitmapPaint);
					invalidate();
				}
				return true;
			case KeyEvent.KEYCODE_SEARCH:
				remember();
				clear();
				return true;
		}
		return false;
	}
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        
        mBrushToast.cancel();
        
        switch (event.getAction()) {
        	case MotionEvent.ACTION_DOWN:
        		mPath.reset();
        		mPath.moveTo(x, y);
        		mPath.lineTo(x+0.5f, y+0.5f);
        		mDirtyRegion.set((int)x, (int)y, (int)x, (int)y);
            	adjustDirtyRegion(x, y);
                invalidate(mDirtyRegion);
                break;
            case MotionEvent.ACTION_MOVE:
            	int historySize = event.getHistorySize();
            	for (int i = 0; i < historySize; i++) {
            		float historicalX = event.getHistoricalX(i);
            		float historicalY = event.getHistoricalY(i);
            		mPath.lineTo(historicalX, historicalY);
            		adjustDirtyRegion(historicalX, historicalY);
            	}
            	mPath.lineTo(x, y);
            	adjustDirtyRegion(x, y);
                invalidate(mDirtyRegion);
                break;
            case MotionEvent.ACTION_UP:
            	remember();
            	mCanvas.drawPath(mPath, mPaint);
            	mPath.reset();
                invalidate(mDirtyRegion);
                break;
        }
        return true;
    }
    
    public void clear() {
    	mBitmap.eraseColor(mPaperColor);
    	invalidate();
    }
    
    public void setColor(int a, int r, int g, int b) {
    	setColor(Color.argb(a, r, g, b));
    }
    
    public void setColor(int color) {
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
    	mAlpha = Math.max(0, mAlpha);
    	mPaint.setAlpha(mAlpha);
    	mColor = mPaint.getColor();
    	toastBrush();
    }
    
    public void decreaseAlpha(int delta) {
    	increaseAlpha(-delta);
    }
    
    public void increaseBrightness(int delta) {
    	int[] rgb = new int[3];
    	rgb[0] = Color.red(mColor);
    	rgb[1] = Color.green(mColor);
    	rgb[2] = Color.blue(mColor);
    	for (int i=0; i< 3; i++) {
    		rgb[i] += delta;
    		rgb[i] = Math.min(255, rgb[i]);
    		rgb[i] = Math.max(0, rgb[i]);
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
    	if (mSize < BLUR_FACTOR) {
    		mBlur = null;
    		mPaint.setMaskFilter(null);
    	} else {
            mBlur = new BlurMaskFilter(mSize/BLUR_FACTOR, BlurMaskFilter.Blur.NORMAL);
    		mPaint.setMaskFilter(mBlur);
    	}
    }

    private void adjustDirtyRegion(float fx, float fy) {
    	int x = (int)fx;
    	int y = (int)fy;
    	int strokeWidthHalf = (int)(mSize/2f)+2+(mSize/BLUR_FACTOR)/2+2;	// Hälfte der Strichbreite + 2 zur Sicherheit wg. Antialias ... bei Blur usw. entsprechend erweitern
    	mDirtyRegion.left = Math.min(mDirtyRegion.left, x-strokeWidthHalf);
    	mDirtyRegion.right = Math.max(mDirtyRegion.right, x+strokeWidthHalf);
    	mDirtyRegion.top = Math.min(mDirtyRegion.top, y-strokeWidthHalf);
    	mDirtyRegion.bottom = Math.max(mDirtyRegion.bottom, y+strokeWidthHalf);
    	Log.i(Main.LOG_TAG, mDirtyRegion.toString());
    }

    /**
     * remember for undo
     */
    private void remember() {
    	mUndoBitmaps[mStep % UNDO_BUFFERS] = Bitmap.createBitmap(mBitmap);
    	mStep++;
    	if (mOldestUndo+UNDO_BUFFERS < mStep) {
    		mOldestUndo++;
    	}
    }
}
