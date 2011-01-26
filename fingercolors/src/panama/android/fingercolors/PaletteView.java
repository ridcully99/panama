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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author ridcully
 *
 */
public class PaletteView extends View {

	private final static int[] COLOR_GRADES = new int[] {
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
		0x33, 0x66, 0x99, 0xCC, 
		0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
		0xCC, 0x99, 0x66, 0x33
	};
	
    private Bitmap  mBitmap;
    private Canvas  mCanvas;
    private Path    mPath;
    private Paint   mBitmapPaint;
    private Paint	mPaint;
    
    private int		mSelectedColor = Color.BLACK;
    
	public PaletteView(Context context, AttributeSet attrs) {
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
        mPaint.setStyle(Style.FILL);
        int boxWidth = w / COLOR_GRADES.length;
        int boxHeight = 2;
        for (int light = 255, top = 0; light>= -255; light -= 8, top += boxHeight) {
	        for (int i=0, left = 0; i < COLOR_GRADES.length; i++, left += boxWidth) {
	        	int r = Math.max(0, Math.min(255, COLOR_GRADES[i]+light));
	        	int g = Math.max(0, Math.min(255, COLOR_GRADES[(i+10)%COLOR_GRADES.length]+light));
	        	int b = Math.max(0, Math.min(255, COLOR_GRADES[(i+20)%COLOR_GRADES.length]+light));
	        	Log.i(Main.LOG_TAG, r+","+g+","+b);
	        	mPaint.setARGB(255, r, g, b);
	            mCanvas.drawRect(left, top, left+boxWidth, top+boxHeight, mPaint);
	        }
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(mSelectedColor);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
    	setMeasuredDimension(parentWidth, 128);
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
            	mSelectedColor = mBitmap.getPixel(x, y);
                //invalidate();
            	ToolsView tools = (ToolsView)((Main)this.getContext()).findViewById(R.id.tools);
            	tools.renderBrushes(mSelectedColor);
                break;
            case MotionEvent.ACTION_UP:
            	CanvasView canvas = (CanvasView)((Main)this.getContext()).findViewById(R.id.canvas);
            	canvas.setColor(mSelectedColor);
                break;
        }
        return true;
    }
}
