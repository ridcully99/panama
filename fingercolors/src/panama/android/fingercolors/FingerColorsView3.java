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
public class FingerColorsView3 extends View {

    private Bitmap  mBitmap;
    private Canvas  mCanvas;
    private Path    mPath;
    private Paint   mBitmapPaint;
    private Paint	mPaint;
    
    private int		mPaperColor = 0xFFAAAAAA;
    
    public FingerColorsView3(Context c, AttributeSet attrs) {
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

    private float lastX, lastY, lastSize, lastPressure;
    private static final float TOUCH_TOLERANCE = 1;
    private int alpha = 255, red = 0, green = 0, blue = 0;	/* paint color */
    
    private void touch_start(float x, float y, int p, float s) {
    	mPaint.setStrokeWidth(s);
    	mPaint.setColor(Color.argb(p, red, green, blue));
        mCanvas.drawPoint(x, y, mPaint);
    }
    
    private void touch_move(float x, float y, int p, float s) {
    	mPaint.setStrokeWidth(s);
    	mPaint.setColor(Color.argb(p, red, green, blue));
        mCanvas.drawLine(lastX, lastY, x, y, mPaint);
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
                touch_start(x, y, (int)(p * alpha), s);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
            	//mCanvas.clipRect(new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight()));
            	int historySize = event.getHistorySize();
            	float pTemp = lastPressure;
            	float sTemp = lastSize;
            	float deltaP = (p - lastPressure)/historySize;
            	float deltaS = (s - lastSize)/historySize;
            	for (int i = 0; i < historySize; i++) {
                	mCanvas.save();
                	Path circle = new Path();
                	circle.addCircle(lastX, lastY, lastSize/2f, Path.Direction.CCW);
                	mCanvas.clipPath(circle, Region.Op.DIFFERENCE);
            		pTemp += deltaP;
            		sTemp += deltaS;
            		float historicalX = event.getHistoricalX(i);
            		float historicalY = event.getHistoricalY(i);
                    touch_move(historicalX, historicalY, (int)(pTemp * alpha), sTemp);
                    lastX = historicalX;
                    lastY = historicalY;
                    mCanvas.restore();
            	}
            	mCanvas.save();
            	Path circle = new Path();
            	circle.addCircle(lastX, lastY, lastSize/2f, Path.Direction.CCW);
            	mCanvas.clipPath(circle, Region.Op.DIFFERENCE);
                touch_move(x, y, (int)(p * alpha), s);
                mCanvas.restore();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        lastX = x;
        lastY = y;
        lastPressure = p;
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
