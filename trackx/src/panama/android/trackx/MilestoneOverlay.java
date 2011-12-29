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

	private List<OverlayItem> items = new ArrayList<OverlayItem>();
	public boolean drawAsShadow = false;
	
	public MilestoneOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
		populate();
	}

	public void addMarker(OverlayItem marker) {
		items.add(marker);
		populate();
	}
	
	public void clear() {
		items.clear();
		populate();
	}
	
	@Override
	protected OverlayItem createItem(int i) {
		return items.get(i);
	}

	@Override
	public int size() {
		return items.size();
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		drawAsShadow = shadow;
		super.draw(canvas, mapView, shadow);
	}

	/**
	 * set milestone markers ever interval meters
	 * @param positions
	 * @param interval distance in meters between milestones
	 * @param unit 'km' or 'mi'
	 */
	public void reset(List<Position> positions, int interval, String unit) {
		clear();
		float intervals = 0;
		float dist = 0;
		Position prev = null;
		for (Position p : positions) {
			if (prev != null) {
				dist += prev.distance;
			}
			if (dist > interval) {
				dist = 0;
				intervals++;
				OverlayItem item = new OverlayItem(p.geoPoint, "", "");
				Drawable milestoneDrawable = new MilestoneDrawable(this, Util.formatMilestoneDistance(interval*intervals), unit);
				boundCenterBottom(milestoneDrawable);
				item.setMarker(milestoneDrawable);
				addMarker(item);
			}
			prev = p;
		}
	}
}
