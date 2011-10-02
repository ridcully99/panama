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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.os.Environment;

/**
 * Save/Load session information internally using JSON
 * @author ridcully
 */
public class SessionPersistence {

	public static boolean save(String name, String notes, long time, float distance, List<Position> positions, boolean replaceExisting) throws JSONException, IOException {
		JSONObject jo = new JSONObject();
		jo.put("notes", notes);
		jo.put("time", time);
		jo.put("distance", distance);
		JSONArray arr = new JSONArray();
		jo.put("positions", arr);
		for (Position p : positions) {
			JSONObject jp = new JSONObject();
			jp.put("lat", p.location.getLatitude());
			jp.put("lon", p.location.getLongitude());
			jp.put("tim", p.location.getTime());
			jp.put("alt", p.location.getAltitude());
			jp.put("acc", p.location.getAccuracy());
			jp.put("spe", p.location.getSpeed());
			jp.put("bea", p.location.getBearing());
			jp.put("pro", p.location.getProvider());
			arr.put(jp);
		}
		
		File root = Environment.getExternalStorageDirectory();
		File dest = new File(new File(root, Environment.DIRECTORY_DOWNLOADS), name);
		if (dest.exists()) {
			if (!replaceExisting) {
				return false;
			}
			if (!dest.delete()) {
				return false;
			}
		}
		BufferedWriter output = new BufferedWriter(new FileWriter(dest));
		output.write(jo.toString());
		output.close();
		return true;
	}
	
	public static Session load(File src) throws JSONException, IOException {
		BufferedReader input = new BufferedReader(new FileReader(src));
		StringBuffer b = new StringBuffer();
		String s;
	    while ((s = input.readLine()) != null) {
	        b.append(s);
	    }
		input.close();
	    JSONObject jo = new JSONObject(b.toString());
		Session session = new Session();
		session.name = src.getName();
		session.notes = jo.getString("notes");
		session.distance = (float)jo.getDouble("distance");
		session.time = jo.getLong("time");
		JSONArray arr = jo.getJSONArray("positions");
		session.positions = new ArrayList<Position>(arr.length());
		for (int i = 0; i<arr.length(); i++) {
			JSONObject jp = arr.getJSONObject(i);
			Location l = new Location(jp.getString("pro"));
			l.setAccuracy((float)jp.getDouble("acc"));
			l.setAltitude(jp.getDouble("alt"));
			l.setBearing((float)jp.getDouble("bea"));
			l.setLatitude(jp.getDouble("lat"));
			l.setLongitude(jp.getDouble("lon"));
			l.setProvider(jp.getString("pro"));
			l.setSpeed((float)jp.getDouble("spe"));
			l.setTime(jp.getLong("tim"));
			Position p = new Position(l);
			p.distance = (float)jp.getDouble("distance");
		}
		return session;
	}
}
