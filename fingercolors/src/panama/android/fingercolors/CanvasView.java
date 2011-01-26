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
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author ridcully
 *
 */
public class CanvasView extends View {
	
    private Bitmap  mBitmap;
    private Canvas  mCanvas;
    private Path    mPath;
    private Paint   mBitmapPaint;
    private Paint	mPaint;
    
    //private MaskFilter mBlur;
    
    private int		mPaperColor = 0xFFAAAAAA;
    
    private float lastX, lastY, lastSize;
    private static final float TOUCH_TOLERANCE = 1;
    private int alpha = 255, red = 0, green = 0, blue = 0;	/* paint color */
    private float size = 12;
    private Rect mDirtyRegion = new Rect(0,0,0,0);
    
    public CanvasView(Context c, AttributeSet attrs) {
        super(c, attrs);
        mBitmap = Bitmap.createBitmap(320, 480, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        //mBlur = new BlurMaskFilter(8, BlurMaskFilter.Blur.SOLID);
        
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFF000000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(size);        
        //mPaint.setMaskFilter(mBlur);

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
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        
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
            	mCanvas.drawPath(mPath, mPaint);
            	mPath.reset();
                invalidate(mDirtyRegion);
                break;
        }
        lastX = x;
        lastY = y;
        return true;
    }
    
    public void clear() {
    	mBitmap.eraseColor(mPaperColor);
    	invalidate();
    }
    
    public void setColor(int a, int r, int g, int b) {
    	alpha = a;
    	red = r;
    	green = g;
    	blue = b;
    	mPaint.setARGB(a, r, g, b);
    }
    
    public void setColor(int color) {
    	mPaint.setColor(color);
    }
    
    public void setBrushSize(int size) {
    	this.size = size;
    	mPaint.setStrokeWidth(size);
    }
    
    private void adjustDirtyRegion(float fx, float fy) {
    	int x = (int)fx;
    	int y = (int)fy;
    	int strokeWidthHalf = (int)(size/2f)+2;	// Hälfte der Strichbreite + 2 zur Sicherheit wg. Antialias ... bei Blur usw. entsprechend erweitern
    	mDirtyRegion.left = Math.min(mDirtyRegion.left, x-strokeWidthHalf);
    	mDirtyRegion.right = Math.max(mDirtyRegion.right, x+strokeWidthHalf);
    	mDirtyRegion.top = Math.min(mDirtyRegion.top, y-strokeWidthHalf);
    	mDirtyRegion.bottom = Math.max(mDirtyRegion.bottom, y+strokeWidthHalf);
    	Log.i(Main.LOG_TAG, mDirtyRegion.toString());
    }
}
