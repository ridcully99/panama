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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;

/**
 * @author ridcully
 *
 */
public class StartDrawable extends MilestoneDrawable {

	public StartDrawable(MilestoneOverlay overlay) {
		super(overlay, "", "Start");
	}

	@Override
	public void drawContent(Canvas canvas) {
		Rect bounds = getBounds();
		float size = mTitlePaint.getTextSize();
		mPaint.setColor(Color.BLACK);
		Path path = new Path();
		path.moveTo(-size/2 + 1, bounds.top+PADDING + 1);
		path.lineTo(size/2 - 1, bounds.top+PADDING + 1 + size/2 - 1);
		path.lineTo(-size/2 + 1, bounds.top+PADDING+size - 2);
		path.close();
		canvas.drawPath(path, mPaint);
		canvas.drawText(mSnippet, 0, -(PADDING+POST_HEIGHT), mPaint);
	}
}
