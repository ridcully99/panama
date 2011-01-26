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
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author ridcully
 *
 */
public class ToolsView extends View {

	private final static int PADDING = 4;
	
    private Bitmap  mBitmap;
    private Canvas  mCanvas;
    private Path    mPath;
    private Paint   mBitmapPaint;
    private Paint	mPaint;
    
    private int 	mMaxBrushSize = 1;
    
	public ToolsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
        mBitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        
        mPaint = new Paint();
        mPaint.setDither(true);
        setFocusableInTouchMode(true);
	}
	
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        renderBrushes(Color.BLACK);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFFFFFFFF);	// TODO später Papierfarbe verwenden
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
    	mMaxBrushSize = parentHeight/8;
    	setMeasuredDimension(parentWidth, mMaxBrushSize+2*PADDING);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();
        if (x < 0 || x >= mBitmap.getWidth() || y < 0 || y >= mBitmap.getHeight()) {
        	return false;
        }
        
        switch (event.getAction()) {
        	case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
            	for (float f = 1, cx = PADDING+mMaxBrushSize + PADDING/2; f >= 0.25; f -= 0.25, cx += mMaxBrushSize + PADDING) {
            		if (x < cx) {
                    	CanvasView canvas = (CanvasView)((Main)this.getContext()).findViewById(R.id.canvas);
                    	int size = (int)FloatMath.floor(mMaxBrushSize * f + 0.5f);	// floor(...+0.5f) is for rounding
                    	canvas.setBrushSize(size);
                    	break;
            		}
            	}
                break;
        }
        return true;
    }
    
    public void renderBrushes(int color) {
    	mPaint.setColor(color);
    	for (float f = 1, x = PADDING+mMaxBrushSize/2; f >= 0.25; f -= 0.25, x += mMaxBrushSize + PADDING) {
    		mCanvas.drawCircle(x, PADDING+mMaxBrushSize/2, (mMaxBrushSize/2)*f, mPaint);
    	}
    	invalidate();
    }
}
