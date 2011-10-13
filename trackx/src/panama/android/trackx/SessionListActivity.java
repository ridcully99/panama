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

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * from http://developer.android.com/reference/android/app/ListActivity.html
 * @author ridcully
 *
 */
public class SessionListActivity extends ListActivity {

	private Cursor mCursor;
	private SessionPersistence mPersistence;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Put a managed wrapper around the retrieved cursor so we don't have to worry about
        // requerying or closing it as the activity changes state.
        mPersistence = new SessionPersistence(this);
        mCursor = mPersistence.getSessionList();
        startManagingCursor(mCursor);

        // Now create a new list adapter bound to the cursor.
        // SimpleListAdapter is designed for binding to a Cursor.
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this, // Context.
                R.layout.sessionlist_item, 
                mCursor,                                              												  // Pass in the cursor to bind to.
                new String[] {SessionPersistence.TIMESTAMP, SessionPersistence.TIME, SessionPersistence.DISTANCE},    // Array of cursor columns to bind to.
                new int[] {R.id.sessionListDate, R.id.sessionListData});  // formating is done via viewBinder
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (view.getId() == R.id.sessionListDate) {
					long timestampMillis = cursor.getLong(1);
					((TextView)view).setText(Util.formatDate(timestampMillis));
					return true;
				}
				if (view.getId() == R.id.sessionListData) {
					long timeMillis = cursor.getLong(2);
					long distance = cursor.getLong(3);
					((TextView)view).setText(Util.formatTime(timeMillis)+", "+Util.formatDistance(distance)+" km");
					return true;
				}
				return false;
			}
		});
        setListAdapter(adapter);	        // Bind to our new adapter.
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	stopManagingCursor(mCursor);
    	mPersistence.close();
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	Intent data = new Intent();
    	data.putExtra("id", id);
    	setResult(RESULT_OK, data);
    	finish();
    }
}
