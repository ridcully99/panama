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

import java.util.List;

import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * @author ridcully
 *
 */
public class PathOverlay extends Overlay {

	private List<Position> mPositions;
	private Path mPath;
	private Paint mPathPaint;
	private Point mHelperPoint = new Point();
	
	public PathOverlay() {
		mPath = new Path();
		mPathPaint = new Paint();
		mPathPaint.setAntiAlias(true);
		mPathPaint.setColor(0xAA00AA00);	// transparent green
		mPathPaint.setStyle(Paint.Style.STROKE);
		mPathPaint.setStrokeJoin(Paint.Join.ROUND);
		mPathPaint.setStrokeCap(Paint.Cap.ROUND);
		mPathPaint.setStrokeWidth(5);
		mPathPaint.setMaskFilter(new BlurMaskFilter(2f, BlurMaskFilter.Blur.NORMAL));
	}
	
	public void setPositions(List<Position> positions) {
		mPositions = positions;
	}
	
	/**
	 * @return two GeoPoint representing top-left and bottom-right of bounding box of path.
	 */
	public GeoPoint[] getBoundingBox() {
		if (mPositions == null || mPositions.isEmpty()) {
			return null;
		}
		int latMin = Integer.MAX_VALUE, latMax = Integer.MIN_VALUE, lonMin = Integer.MAX_VALUE, lonMax = Integer.MIN_VALUE;
		for (Position p : mPositions) {
			GeoPoint gp = p.geoPoint;
			int lat = gp.getLatitudeE6();
			int lon = gp.getLongitudeE6();
			latMin = Math.min(latMin, lat);
			latMax = Math.max(latMax, lat);
			lonMin = Math.min(lonMin, lon);
			lonMax = Math.max(lonMax, lon);
		}
		return new GeoPoint[] { new GeoPoint(latMin, lonMin), new GeoPoint(latMax, lonMax) };
	}
	
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (shadow) {
			return;	// we don't have a shadow.
		}
		if (mPositions != null && mPositions.size() > 0) {
			//float width = mapView.getZoomLevel() / 2;
			//mPathPaint.setStrokeWidth(width);

			// path jedesmal neu aufbauen weil sich zoom und scroll verändert haben könnten
			mPath.reset();
			boolean start = true;
			for (Position p : mPositions) {
				mapView.getProjection().toPixels(p.geoPoint, mHelperPoint);
				if (start) {
					mPath.moveTo(mHelperPoint.x, mHelperPoint.y);
					start = false;
				} else {
					mPath.lineTo(mHelperPoint.x, mHelperPoint.y);
				}
			}
			canvas.drawPath(mPath, mPathPaint);
		}
	}
}
