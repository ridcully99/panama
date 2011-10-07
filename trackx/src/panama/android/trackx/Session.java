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

import android.content.ContentValues;

/**
 * @author ridcully
 *
 */
public class Session {

	public String name;
	public String notes;
	public long timestamp;
	public List<Position> positions;
	public long time;
	public float distance;
	
	public Session() {
		reset();
	}
	
	public Session(String name, String notes, long timestamp, long time, float distance, List<Position> positions) {
		this.name = name;
		this.notes = notes;
		this.timestamp = timestamp;
		this.time = time;
		this.distance = distance;
		this.positions = positions;
	}

	public void reset() {
		time = 0;
		distance = 0;
		if (positions != null) {
			positions.clear();
		} else {
			positions = new ArrayList<Position>();
		}
	}
	
	/** for persistence */
	public ContentValues getContentValues() {
		ContentValues cv = new ContentValues();
		cv.put(SessionPersistence.NAME, name);
		cv.put(SessionPersistence.NOTES, notes);
		cv.put(SessionPersistence.TIMESTAMP, timestamp);
		cv.put(SessionPersistence.TIME, time);
		cv.put(SessionPersistence.DISTANCE, distance);
		return cv;
	}
}
