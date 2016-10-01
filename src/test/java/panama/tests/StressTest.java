/*
 *  Copyright 2004-2016 Robert Brandner (robert.brandner@gmail.com) 
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
package panama.tests;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import junit.framework.TestCase;

/**
 * Test for occasional HTTP-500 error
 * @author ridcully
 *
 */
public class StressTest extends TestCase {

	//private final static String TEST_URL = "http://zrw.evolaris.net/zrw/services/";
	
	private final static String TEST_URL = "http://localhost:8080/panama-examples/guestbook/";
	
	private final static int NUM_THREADS = 50;
	private final static int NUM_RUNS = 5;
	private final static int MIN_RANDOM_SLEEP = 100;
	private final static int MAX_RANDOM_SLEEP = 500;
	private final static String OK_STRING = "HTTP/1.1 200 OK";

	public void testSingleAccess() {
		try {
		    // Create a URLConnection object for a URL
		    URL url = new URL(TEST_URL);
		    URLConnection conn = url.openConnection();
		    System.out.println(conn.getHeaderField(null));
		    // List all the response headers from the server.
		    // Note: The first call to getHeaderFieldKey() will implicit send
		    // the HTTP request to the server.
//		    for (int i=0; ; i++) {
//		        String headerName = conn.getHeaderFieldKey(i);
//		        String headerValue = conn.getHeaderField(i);
//
//		        if (headerName == null && headerValue == null) {
//		            // No more headers
//		            break;
//		        }
//		        if (headerName == null) {
//		            // The header value contains the server's HTTP version
//		        }
//		        System.out.println(headerName+":"+headerValue);
//		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testParallelAccess() {
		
		TestRunner threads[] = new TestRunner[NUM_THREADS];
		
		for (int i=0; i<NUM_THREADS; i++) {
			TestRunner t = new TestRunner("Thread-"+(i+1));
			t.start();
			threads[i] = t;
		}
		
		// wait for all Threads to finish.
		for (int i=0; i<threads.length; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
 		
		for (int i=0; i<threads.length; i++) {
			System.out.println(threads[i].name+": "+threads[i].errorCount);
		}
		
	}
	
	class TestRunner extends Thread {

		public int errorCount = 0;
		public String name = "";
		
		public TestRunner(String name) {
			this.name = name;
		}
		
		@Override
		public void run() {
			try {
			    // Create a URLConnection object for a URL
			    URL url = new URL(TEST_URL);
				for (int r=0; r<NUM_RUNS; r++) {			    
				    URLConnection conn = url.openConnection();
				    String status = conn.getHeaderField(null);
			    	System.out.println(name+":"+status);
				    if (!OK_STRING.equals(status)) {
				    	errorCount++;
				    }
				    long sleepTime = (long)(Math.random()*(MAX_RANDOM_SLEEP-MIN_RANDOM_SLEEP));
				    Thread.sleep(sleepTime);
			    }
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}
	}
}
