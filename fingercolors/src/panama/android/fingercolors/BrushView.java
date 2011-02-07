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
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * A custom view for a Toast to show current brush-size and color.
 * 
 * @author ridcully
 *
 */
public class BrushView extends View {

	private final static int PADDING = 8;
	
	private RectF mBackgroundRect = new RectF();
	private Paint mBrushPaint;
	private Paint mBackgroundPaint;
	
	public BrushView(Context context) {
		this(context, null);
	}
		
	public BrushView(Context context, AttributeSet attrs) {
        super(context, attrs);
		mBackgroundPaint = new Paint();
		mBackgroundPaint.setStrokeWidth(1);
		mBackgroundPaint.setAntiAlias(true);
		mBackgroundPaint.setStyle(Style.FILL);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawColor(0x00FFFFFF);

		// draw background
		int color = mBrushPaint.getColor();
		float size = mBrushPaint.getStrokeWidth();
		//int luminance = (int)(0.3f*Color.red(color) + 0.59f*Color.green(color) + 0.11f*Color.blue(color));	// brightness
		//mBackgroundPaint.setColor(luminance < 128 ? 0xFFFFFFFF : 0xFF000000);
		//mBackgroundPaint.setAlpha(Color.alpha(color));
		mBackgroundPaint.setShader(new LinearGradient(0, 0, mBackgroundRect.right, 0, 0xFF666666, 0xFFFFFFFF, TileMode.CLAMP));
		canvas.drawRoundRect(mBackgroundRect, PADDING, PADDING, mBackgroundPaint);
		canvas.drawLine(PADDING+size/2, mBackgroundRect.centerY(), mBackgroundRect.right-size/2-PADDING, mBackgroundRect.centerY(), mBrushPaint);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
		int size = (int)mBrushPaint.getStrokeWidth();
        int width = parentWidth/2 + 2*PADDING;
		int height = Math.max(size, 4*PADDING) + 2*PADDING;
		mBackgroundRect.set(0, 0, width-1, height-1);
    	setMeasuredDimension(width, height);
	}
	
	public void setPaint(Paint paint) {
		mBrushPaint = paint;
	}
}
