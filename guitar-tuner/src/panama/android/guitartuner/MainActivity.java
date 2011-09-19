package panama.android.guitartuner;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static int SAMPLE_RATE = 8000;
	private static int BUFFER_SIZE = SAMPLE_RATE / 2;
	
	private AudioRecord mRecord = null;
	private short[] mBuffer = null;
	private FrequenceView mFreqView = null;
	private TextView mHertzLabel = null;
	
	private AudioRecord.OnRecordPositionUpdateListener mListener = new AudioRecord.OnRecordPositionUpdateListener() {

		@Override
		public void onPeriodicNotification(AudioRecord record) {
			Log.i("Listener", "onNotification");
			int read = record.read(mBuffer, 0, mBuffer.length);
			if (read < 2) {
				return;
			}
			mFreqView.update(mBuffer, read);
			int distances[] = new int[read];
			int distcount = 0;
			int prev = -1;
			for (int i=0; i < read-1; i++) {
				if (mBuffer[i] >= 0 && mBuffer[i+1] < 0) {
					if (prev != -1) {
						int dist = i-prev;
						distances[distcount++] = dist;
					}
					prev = i;
				}
			}
			if (distcount > 0) {
				int dsum = 0;
				for (int i = 0; i<distcount;i++) {
					dsum += distances[i];
				}
				float avg = dsum/distcount;
				float hertz = SAMPLE_RATE/avg;
				mHertzLabel.setText(""+hertz);
				mHertzLabel.postInvalidate();
			}
		}
		
		@Override
		public void onMarkerReached(AudioRecord record) {
		}
	};
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mFreqView = (FrequenceView)findViewById(R.id.frequenceView);
        mHertzLabel = (TextView)findViewById(R.id.hertzLabel);
        mBuffer = new short[BUFFER_SIZE];
        try {
        	mRecord = new AudioRecord(android.media.MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, mBuffer.length/2); /* short == 2 bytes */
            mRecord.setRecordPositionUpdateListener(mListener);
            mRecord.setPositionNotificationPeriod(1000); 
        } catch (Exception e) {
        	Log.e("Recorder", e.getMessage());
        }
    }
    
    public void onButtonClick(View view) {
    	switch(view.getId()) {
    	case R.id.startButton: 
            mRecord.startRecording();
            mRecord.read(mBuffer, 0, mBuffer.length);	// lt. http://groups.google.com/group/android-developers/browse_thread/thread/1bf74961d3480bde einmal direkt aufrufen um Buffer zu übergeben.
    		break;
    	case R.id.stopButton:
        	mRecord.stop();
    		break;
    	}
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	mRecord.stop();
    	mRecord.release();
    	mRecord = null;
    }
}