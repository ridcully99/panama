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
import java.util.List;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

/**
 * @author ridcully
 *
 */
public class MilestoneOverlay extends ItemizedOverlay<OverlayItem> {

	public boolean drawShadow = false;

	private List<Position> mPositions;
	private List<OverlayItem> mItems = new ArrayList<OverlayItem>();
	private int mInterval = 1000;	// interval in meters between milestones
	private String mUnit = "km";	// km or mi
	
	public MilestoneOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
		populate();
	}

	public void setPositions(List<Position> positions) {
		mPositions = positions;
	}
	
	public void setInterval(int interval) {
		mInterval = interval;
	}
	
	public void setUnit(String unit) {
		mUnit = unit;
	}
	
	/**
	 * 
	 * @param p
	 * @param distance distance in meters
	 * @param unit
	 */
	private void addMarker(Position p, Drawable drawable) {
		OverlayItem item = new OverlayItem(p.geoPoint, "", "");
		boundCenterBottom(drawable);
		item.setMarker(drawable);
		mItems.add(item);
		populate();
	}
	
	public void clear() {
		mItems.clear();
		populate();
	}
	
	@Override
	protected OverlayItem createItem(int i) {
		return mItems.get(i);
	}

	@Override
	public int size() {
		return mItems.size();
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		drawShadow = shadow;
		if (shadow) {				// shadow wird immer zuerst gemalt
			rebuildMilestones();	// jedesmal neu bauen (aber wenigstens für shadow und nicht shadow gleiche verwenden)
		}
		super.draw(canvas, mapView, shadow);
	}

	/**
	 * creates milestone markers every interval meters
	 */
	public void rebuildMilestones() {
		clear();
		if (mPositions == null) {
			return;
		}
		float intervals = 0;
		float dist = 0;
		Position prev = null;
		for (Position p : mPositions) {
			if (prev != null) {
				dist += prev.distance;
			}
			if (dist > mInterval) {
				dist = 0;
				intervals++;
				addMarker(p, new MilestoneDrawable(this, Util.formatMilestoneDistance(mInterval*intervals), mUnit));
			}
			if (p.type == Position.TYPE_START) {
				addMarker(p, new StartDrawable(this));
			}
			if (p.type == Position.TYPE_FINISH) {
				addMarker(p, new FinishDrawable(this));
			}
			prev = p;
		}
	}
}
