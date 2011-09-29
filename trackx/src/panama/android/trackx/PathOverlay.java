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

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * @author ridcully
 *
 */
public class PathOverlay extends Overlay {

	private MainActivity mMainActivity;
	private ArrayList<GeoPoint> mPoints = new ArrayList<GeoPoint>();
	private Location mCurrentLocation;
	private Path mPath;
	private Paint mPathPaint;
	private Paint mArrowPaint;
	private Point mHelperPoint = new Point();
	
	public PathOverlay(MainActivity activity, Bundle savedInstanceState) {
		mMainActivity = activity;
		mPath = new Path();
		mPathPaint = new Paint();
		mPathPaint.setAntiAlias(true);
		mPathPaint.setColor(0xAA00AA00);	// transparent green
		mPathPaint.setStyle(Paint.Style.STROKE);
		mPathPaint.setStrokeJoin(Paint.Join.ROUND);
		mPathPaint.setStrokeCap(Paint.Cap.ROUND);
		mPathPaint.setStrokeWidth(5);

		mArrowPaint = new Paint();
		mArrowPaint.setAntiAlias(true);
		mArrowPaint.setColor(0x880000AA);	// slightly transparent blue
		mArrowPaint.setStyle(Paint.Style.STROKE);
		mArrowPaint.setStrokeJoin(Paint.Join.BEVEL);
		mArrowPaint.setStrokeCap(Paint.Cap.BUTT);

		// TODO rebuild mPoints from savedInstanceState (if not null)
	}
	
	/**
	 * Set current location directly (to show it even before start)
	 * @param location
	 */
	public void setCurrentLocation(Location location) {
		mCurrentLocation = location;
	}
	
	public void clear() {
		mPoints.clear();
	}
	
	/**
	 * Appends specified location.
	 * 
	 * @param location
	 */
	public void appendLocation(Location location) {
		mPoints.add(Util.locationToGeoPoint(location));
	}
	
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (shadow) {
			return;	// we don't have a shadow.
		}
		// draw path
		if (mPoints.size() > 0) {
			// path jedesmal neu aufbauen weil sich zoom und scroll verändert haben könnten
			// TODO? hier ist ggf. Optimierungspotential -- so lang sich zoom und scroll nicht ändern müsste der Pfad nicht neu gebaut werden, sondern es könnte die letzte Position einfach angehängt werden
			mPath.reset();
			boolean start = true;
			for (GeoPoint p : mPoints) {
				mapView.getProjection().toPixels(p, mHelperPoint);
				if (start) {
					mPath.moveTo(mHelperPoint.x, mHelperPoint.y);
					start = false;
				} else {
					mPath.lineTo(mHelperPoint.x, mHelperPoint.y);
				}
			}
			canvas.drawPath(mPath, mPathPaint);
		}
		// draw current location
		if (mCurrentLocation != null) {
			mapView.getProjection().toPixels(Util.locationToGeoPoint(mCurrentLocation), mHelperPoint);
			canvas.save();
			canvas.translate(mHelperPoint.x, mHelperPoint.y);
			canvas.rotate(mCurrentLocation.getBearing());
			canvas.scale(0.5f, -0.5f);	// scale to mirror on x-axis and adopt size -- easier than changing all coords below ;-)
			mPath.reset();
			mPath.moveTo(0, 0);
			if (!mCurrentLocation.hasBearing() || mCurrentLocation.getSpeed() == 0.0) {
				mPath.addCircle(0, 0, 45f, Direction.CCW);	// if no movement, draw circle
			} else {
				mPath.lineTo(-45f, -30f);
				mPath.lineTo(0, 60);
				mPath.lineTo(45f, -30f);
				mPath.lineTo(0, 0);
			}
			if (mMainActivity.mSessionState != MainActivity.IDLE) {
				mArrowPaint.setStyle(Style.FILL);
				canvas.drawPath(mPath, mArrowPaint);
				mArrowPaint.setStyle(Style.STROKE);
			}
			canvas.drawPath(mPath, mArrowPaint);
			canvas.restore();
		}
	}
}
