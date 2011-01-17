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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author ridcully
 *
 */
public class FingerColorsView extends View {

    private Bitmap  mBitmap;
    private Canvas  mCanvas;
    private Path    mPath;
    private Paint   mBitmapPaint;
    private Paint	mPaint;
    
    private int		mPaperColor = 0xFFAAAAAA;
    
    public FingerColorsView(Context c, AttributeSet attrs) {
        super(c, attrs);
        setFocusableInTouchMode(true);
        mBitmap = Bitmap.createBitmap(320, 480, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFFFF0000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(24);        
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
    }

    private float lastX, lastY, lastSize;
    private static final float TOUCH_TOLERANCE = 1;
    private int alpha = 255, red = 0, green = 0, blue = 0;	/* paint color */
    private int currAlpha = alpha;
    private float currSize = 12;
    
    private void touch_move(float x, float y) {
    	if (currAlpha <= 0) {
    		return;
    	}
    	float dx = x - lastX;
    	float dy = y - lastY; 
    	double dist = Math.sqrt((double)(dx*dx+dy*dy));
    	currAlpha -= (int)dist/2;
    	if (currAlpha < 0) {
    		currAlpha = 0;
    	}
    	mCanvas.save();
    	Path circle = new Path();
    	circle.addCircle(lastX, lastY, currSize/2f, Path.Direction.CCW);
    	mCanvas.clipPath(circle, Region.Op.DIFFERENCE);
    	mPaint.setColor(Color.argb(currAlpha, red, green, blue));
        mCanvas.drawLine(lastX, lastY, x, y, mPaint);
        mCanvas.restore();
    }
    
    private void touch_up() {
    }
    
    private void setColor(int a, int r, int g, int b) {
    	alpha = a;
    	red = r;
    	green = g;
    	blue = b;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float p = event.getPressure();
        float s = getWidth()*event.getSize();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            	currSize = s;
            	if (p < 0.1) {
            		currAlpha = (int)(alpha * 0.33);
            	} else if (p < 0.175) {
            		currAlpha = (int)(alpha * 0.66);
            	} else {
            		currAlpha = alpha;
            	}
                Log.i(FingerColorsApp.LOG_TAG, "Pressure: "+p+", size="+event.getSize()+", currAlpha: "+currAlpha);
            	mPaint.setColor(Color.argb(currAlpha, red, green, blue));
            	mPaint.setStrokeWidth(currSize);
                mCanvas.drawPoint(x, y, mPaint);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
            	int historySize = event.getHistorySize();
            	for (int i = 0; i < historySize; i++) {
            		float historicalX = event.getHistoricalX(i);
            		float historicalY = event.getHistoricalY(i);
                    touch_move(historicalX, historicalY);
                    lastX = historicalX;
                    lastY = historicalY;
            	}
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        lastX = x;
        lastY = y;
        lastSize = s;
        return true;
    }
    
    @Override
    public boolean onTrackballEvent(MotionEvent event) {
    	Log.i(FingerColorsApp.LOG_TAG, "trackball-event");
    	super.onTrackballEvent(event);
    	setColor(255, (int)(Math.random()*255), (int)(Math.random()*255), (int)(Math.random()*255));
    	return false;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	Log.i(FingerColorsApp.LOG_TAG, "keydown-event");
    	return super.onKeyDown(keyCode, event);
    }
    
    public void clear() {
    	mBitmap.eraseColor(mPaperColor);
    	invalidate();
    }
}
