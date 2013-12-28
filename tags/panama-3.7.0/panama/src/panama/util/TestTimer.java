/*
 *  Copyright 2004-2012 Robert Brandner (robert.brandner@gmail.com) 
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
// TestTimer: $Source$
// TODO Class summary goes here
//
// Created : 31 oct. 2005 by jfsmart
// Last modified $Date$ by $Author$
// Revision: $Revision$
// Version : $ID$
// Copyright (c) 2005
////////////////////////////////////////////////////////////////////////////////

package panama.util;

import panama.log.SimpleLogger;

/**
 * Simple utilities class for benchmarking unit tests. Use is a follows:
 *     TestTimer t = new TestTimer("My task");
 * 	   // Do something
 *     t.done();

 * @author jfsmart
 * @author Ridcully
 */
public class TestTimer {

	private long startTime;
	private String message; 
	
	/* Logging */
	protected static SimpleLogger log = new SimpleLogger(TestTimer.class);	

	/**
	 * Initiate a timer
	 */
	public TestTimer(String message) {
		startTime = System.currentTimeMillis();
		this.message = message;
	}
	
	/**
	 * Reset the timer for another timing session.
	 *
	 */
	public void reset() {
		startTime = System.currentTimeMillis();		
	}
	
	/**
	 * End the timing session and log the results.
	 * @return The logged result as string.
	 */
	public String done() {
		long endTime = System.currentTimeMillis();
		String result = message + " : "
   								+ (endTime - startTime) 
   								+ " ms.";
		log.info(result);
		return result;
	}
}
