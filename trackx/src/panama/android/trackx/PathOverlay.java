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
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

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
		mPathPaint.setStrokeWidth(7);
		mPathPaint.setMaskFilter(new BlurMaskFilter(2f, BlurMaskFilter.Blur.NORMAL));

		// TODO rebuild mPoints from savedInstanceState (if not null)
	}
	
	public void setPositions(List<Position> positions) {
		mPositions = positions;
	}
	
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (shadow) {
			return;	// we don't have a shadow.
		}
		if (mPositions != null && mPositions.size() > 0) {
			// path jedesmal neu aufbauen weil sich zoom und scroll ver�ndert haben k�nnten
			// TODO? hier ist ggf. Optimierungspotential -- so lang sich zoom und scroll nicht �ndern m�sste der Pfad nicht neu gebaut werden, sondern es k�nnte die letzte Position einfach angeh�ngt werden
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
