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
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author ridcully
 *
 */
public class PaletteView extends View {

	private final static int GRAY_BAND_HEIGHT = 16;
	private final static int COLOR_BAND_HEIGHT = 128;
	private final static int[] COLORS = new int[] { 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000, 0xFFFF00FF, 0xFF0000FF};
	
	
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
        // horizontal gray band
        mPaint.setShader(new LinearGradient(0, 0, w-1, 0, 0xFFFFFFFF, 0xFF000000, TileMode.CLAMP));
        mCanvas.drawRect(0, 0, w, GRAY_BAND_HEIGHT, mPaint);
        // horizontal rainbow gradient
        mPaint.setShader(new LinearGradient(0, 0, w-1, 0, COLORS, null, TileMode.CLAMP));
        mCanvas.drawRect(0, GRAY_BAND_HEIGHT, w, h, mPaint);
        // vertical white to invisible gradient to half
        mPaint.setShader(new LinearGradient(0, GRAY_BAND_HEIGHT, 0, GRAY_BAND_HEIGHT+COLOR_BAND_HEIGHT/2, 0xFFFFFFFF, 0x00FFFFFF, TileMode.CLAMP));
        mCanvas.drawRect(0, GRAY_BAND_HEIGHT, w, GRAY_BAND_HEIGHT+COLOR_BAND_HEIGHT/2, mPaint);
        // vertical invisible to black gradient form half
        mPaint.setShader(new LinearGradient(0, GRAY_BAND_HEIGHT+COLOR_BAND_HEIGHT/2, 0, h, 0x00000000, 0xFF000000, TileMode.CLAMP));
        mCanvas.drawRect(0, GRAY_BAND_HEIGHT+COLOR_BAND_HEIGHT/2, w, h, mPaint);
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
    	setMeasuredDimension(parentWidth, GRAY_BAND_HEIGHT+COLOR_BAND_HEIGHT);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();
        x = Math.max(0, x);
        x = Math.min(x, mBitmap.getWidth()-1);
        y = Math.max(0, y);
        y = Math.min(y, mBitmap.getHeight()-1);
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