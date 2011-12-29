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
package panama.android.trackx;

import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/**
 * @author ridcully
 *
 */
public class MilestoneDrawable extends Drawable {

	private final static int TITLE_FONT_SIZE = 20;
	private final static int SNIPPET_FONT_SIZE = 12;
	private final static int PADDING = 4;
	private final static int POST_HEIGHT = 15;
	
	private MilestoneOverlay mOverlay;
	private String mTitle;
	private String mSnippet;
	public static MaskFilter blurMaskFilter = new BlurMaskFilter(3, Blur.SOLID);
	public static Paint mPaint; 	// reuse for all markers
	public static Paint mTitlePaint;
	static {
		mPaint = new Paint();
		mPaint.setStrokeWidth(2);
		mPaint.setAntiAlias(true);
		mPaint.setTextSize(SNIPPET_FONT_SIZE);
		mPaint.setTextAlign(Align.CENTER);
		mPaint.setStrokeJoin(Join.ROUND);
		mTitlePaint = new Paint(mPaint);
		mTitlePaint.setTextSize(TITLE_FONT_SIZE);
		mTitlePaint.setTypeface(Typeface.DEFAULT_BOLD);
		mTitlePaint.setColor(Color.BLACK);
	}
	private RectF mRectF = new RectF();
	
	public MilestoneDrawable(MilestoneOverlay overlay, String title, String snippet) {
		super();
		mOverlay = overlay;
		mTitle = title;
		mSnippet = snippet;
	}

	@Override
	public int getIntrinsicWidth() {
		float w = Math.max(mTitlePaint.measureText(mTitle), mPaint.measureText(mSnippet));
		return (int)w + PADDING * 2;
	}
	
	@Override
	public int getIntrinsicHeight() {
		return (int)(mTitlePaint.getTextSize() + mPaint.getTextSize() + PADDING * 2 + POST_HEIGHT);
	}
	
	@Override
	public void setBounds(int left, int top, int right, int bottom) {
		super.setBounds(left, top, right, bottom);
	}
	
	@Override
	public void draw(Canvas canvas) {
		if (mOverlay != null && mOverlay.drawAsShadow) {
			mPaint.setMaskFilter(blurMaskFilter);
		} else {
			mPaint.setMaskFilter(null);
		}
		mPaint.setStyle(Style.FILL);
		mPaint.setColor(Color.WHITE);
		mRectF.set(getBounds());
		mRectF.bottom -= POST_HEIGHT;
		
		Rect bounds = getBounds();
		Path p = new Path();
		p.moveTo(0, 0);
		p.lineTo(bounds.width()/4f, -POST_HEIGHT);
		p.lineTo(bounds.width()/2f, -POST_HEIGHT);
		p.lineTo(bounds.width()/2f, -bounds.height());
		p.lineTo(-bounds.width()/2f, -bounds.height());
		p.lineTo(-bounds.width()/2f, -POST_HEIGHT);
		p.lineTo(-bounds.width()/4f, -POST_HEIGHT);
		p.close();
		canvas.drawPath(p, mPaint);
		//canvas.drawRoundRect(mRectF, PADDING, PADDING, mPaint);
		if (mOverlay != null && !mOverlay.drawAsShadow) {
			mPaint.setColor(Color.BLACK);
			canvas.drawText(mSnippet, 0, -(PADDING+POST_HEIGHT), mPaint);
			mPaint.setStyle(Style.STROKE);
			mPaint.setStrokeWidth(1.5f);
			canvas.drawText(mTitle, 0, -(POST_HEIGHT+PADDING+mPaint.getTextSize()+PADDING), mTitlePaint);
			canvas.drawPath(p, mPaint);
			//canvas.drawRoundRect(mRectF, PADDING, PADDING, mPaint);
		}
		//mPaint.setStrokeWidth(4f);
		//canvas.drawLine(0, 0, 0, -POST_HEIGHT, mPaint);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha(int a) {
		mPaint.setAlpha(a);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		mPaint.setColorFilter(cf);
	}
}
