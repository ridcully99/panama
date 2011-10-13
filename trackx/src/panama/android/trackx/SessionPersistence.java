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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

/**
 * Save/Load session information internally using JSON
 * @author ridcully
 */
public class SessionPersistence {

	private final static String DB_NAME = "trackx_db";
	private final static int    DB_VERSION = 5;
	private final static String TBL_SESSION = "session";
	private final static String TBL_POSITION = "position";
	
	// Column names
	public static final String NAME = "name";
	public static final String NOTES = "notes";
	public static final String TIMESTAMP = "timestamp";
	public static final String TIME = "time";
	public static final String DISTANCE = "distance";

	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String ALTITUDE = "altitude";
	public static final String ACCURACY = "accuracy";
	public static final String SPEED = "speed";
	public static final String BEARING = "bearing";
	public static final String PROVIDER = "provider";

	
	private Context mContext;
	private MyOpenHelper mDBHelper;

	public SessionPersistence(Context context) {
		mContext = context;
		mDBHelper = new MyOpenHelper(context);
	}

	/** close opened database */
	public void close() {
		mDBHelper.close();
	}
	
	public boolean save(Session session) throws JSONException, IOException {
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			long id = db.insertOrThrow(TBL_SESSION, null, session.getContentValues());
			ContentValues cv = new ContentValues();
			cv.put("session_id", id);
			for (Position p : session.positions) {
				p.applyTo(cv);	
				db.insertOrThrow(TBL_POSITION, null, cv);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			mDBHelper.close();
		}
		return true;
	}
	
	public Session load(long sessionId) {
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		Cursor c = db.query(TBL_SESSION, new String[] {TIME, DISTANCE}, "_id = "+sessionId, null, null, null, null);
		if (c.getCount() == 0) {
			Log.e(MainActivity.TAG, "session "+sessionId+" not found.");
			c.close();
			mDBHelper.close();
			return null;
		}
		Session session = new Session();
		c.moveToFirst();
		session.time = c.getLong(0);
		session.distance = c.getFloat(1);
		c.close();

		c = db.query(TBL_POSITION, new String[] {TIME, LATITUDE, LONGITUDE, ALTITUDE, ACCURACY, SPEED, BEARING, PROVIDER, DISTANCE}, "session_id = "+sessionId, null, null, null, TIME+" asc");
		int n = c.getCount();
		List<Position> positions = new ArrayList<Position>(n);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			Location l = new Location(c.getString(7));	// provider
			l.setTime(c.getLong(0));
			l.setLatitude(c.getDouble(1));
			l.setLongitude(c.getDouble(2));
			l.setAltitude(c.getDouble(3));
			l.setAccuracy(c.getFloat(4));
			l.setSpeed(c.getFloat(5));
			l.setBearing(c.getFloat(6));
			Position p = new Position(l);
			p.distance = c.getFloat(8);
			positions.add(p);
			c.moveToNext();
		}
		c.close();
		mDBHelper.close();
		session.positions = positions;
		return session;
	}

	/** gets list of all sessions, ordered by timestamp descending */
	public Cursor getSessionList() {
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		Cursor c = db.query(TBL_SESSION, new String[] {"_id", TIMESTAMP, TIME, DISTANCE}, null, null, null, null, "timestamp desc");
		return c;
	}	
	
	// ---------------------------------------------------------------------------------- MyOpenHelper
	
	private static class MyOpenHelper extends SQLiteOpenHelper {

		public MyOpenHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
		    db.execSQL("CREATE TABLE " + TBL_SESSION +
		            " (_id integer primary key autoincrement, "+
		            NAME  +" text, "+
		            NOTES + " text, "+
		            TIMESTAMP + " integer, "+ // millis
		            TIME  + " integer, "+	// millis
		        	DISTANCE  + " real " +
		            " );");
		
		    db.execSQL("CREATE TABLE " + TBL_POSITION +
		        	" (_id integer primary key autoincrement, "+
		        	" session_id integer, "+
		            TIME 	  + " integer, "+	// millis
		        	LATITUDE  + " real, " +
		        	LONGITUDE + " real, " +
		        	ALTITUDE  + " real, " +
		        	ACCURACY  + " real, " +
		        	SPEED     + " real, " +
		        	BEARING   + " real, " +
		        	PROVIDER  + " text, " +
		        	DISTANCE  + " real " +
		        	");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO nur Beispiel -- bei späteren Updates sollten einfache ALTER TABLEs reichen um die Daten nicht zu verlieren.
	        db.execSQL("DROP TABLE IF EXISTS " + TBL_SESSION);
	        db.execSQL("DROP TABLE IF EXISTS " + TBL_POSITION);
	        onCreate(db);
		}
	}
}
