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
package panama.android.guitartuner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * @author ridcully
 *
 */
public class FrequenceView extends View {

	private Context mContext;
	private short[] mBuffer;
	private int mLength;
	private Paint mPaint;
	
	public FrequenceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mPaint = new Paint();
		mPaint.setColor(Color.GREEN);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mBuffer == null) {
			return;
		}
		canvas.drawColor(Color.BLACK);
		for (int x = 0; x < 320; x++) {
			float y = 240 + mBuffer[x] * 240f / 16000f;
			canvas.drawPoint(x, y, mPaint);
		}
	}
	
	public void update(short[] buffer, int length) {
		mBuffer = buffer;
		mLength = length;
		postInvalidate();
	}
}
