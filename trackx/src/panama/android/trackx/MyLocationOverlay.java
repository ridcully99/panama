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
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;

import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * @author ridcully
 *
 */
public class MyLocationOverlay extends Overlay {

	private MainActivity mMainActivity;
	private Paint mArrowPaint;
	private Path mPath;
	private Point mHelperPoint = new Point();
	
	public MyLocationOverlay(MainActivity activity, Bundle savedInstanceState) {
		mMainActivity = activity;

		mPath = new Path();
		mArrowPaint = new Paint();
		mArrowPaint.setAntiAlias(true);
		mArrowPaint.setColor(0x880000AA);	// slightly transparent blue
		mArrowPaint.setStyle(Paint.Style.STROKE);
		mArrowPaint.setStrokeJoin(Paint.Join.BEVEL);
		mArrowPaint.setStrokeCap(Paint.Cap.BUTT);
	}
	
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (shadow) {
			return;	// we don't have a shadow.
		}
		Location currentLocation = mMainActivity.mCurrentLocation;
		if (currentLocation != null) {
			mapView.getProjection().toPixels(Util.locationToGeoPoint(currentLocation), mHelperPoint);
			canvas.save();
			canvas.translate(mHelperPoint.x, mHelperPoint.y);
			canvas.rotate(currentLocation.getBearing());
			canvas.scale(0.5f, -0.5f);	// scale to mirror on x-axis and adopt size -- easier than changing all coords below ;-)

			// draw accuray circle
			float radius = Util.metersToRadius(currentLocation.getAccuracy(), mapView, currentLocation.getLatitude());
			canvas.drawCircle(0, 0, radius, mArrowPaint);

			// draw filled arrow with border
			mPath.reset();
			mPath.moveTo(0, 0);
			mPath.lineTo(-45f, -30f);
			mPath.lineTo(0, 60);
			mPath.lineTo(45f, -30f);
			mPath.lineTo(0, 0);
			mArrowPaint.setStyle(Style.FILL);
			canvas.drawPath(mPath, mArrowPaint);
			mArrowPaint.setStyle(Style.STROKE);
			canvas.drawPath(mPath, mArrowPaint);
			
			canvas.restore();
		}
	}	
}
