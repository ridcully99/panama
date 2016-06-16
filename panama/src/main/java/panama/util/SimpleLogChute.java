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
package panama.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;

import panama.log.DebugLevel;
import panama.log.SimpleLogger;

/**
 * A logger for Velocity that uses SimpleLog for logging.
 * @author ridcully
 *
 */
public class SimpleLogChute implements LogChute {

	private static SimpleLogger log = new SimpleLogger(Velocity.class);
	private static Map<Integer, DebugLevel> debugLevels = new HashMap<Integer, DebugLevel>() {
		private static final long serialVersionUID = 1L;
		{
		put(LogChute.ERROR_ID, DebugLevel.L2_ERROR);
		put(LogChute.WARN_ID, DebugLevel.L3_WARN);
		put(LogChute.INFO_ID, DebugLevel.L4_INFO);		
		put(LogChute.DEBUG_ID, DebugLevel.L5_DEBUG);
		put(LogChute.TRACE_ID, DebugLevel.L6_VERBOSE);		
	}};
	
	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.log.LogChute#init(org.apache.velocity.runtime.RuntimeServices)
	 */
	public void init(RuntimeServices rs) throws Exception {
	}

	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.log.LogChute#log(int, java.lang.String)
	 */
	public void log(int level, String message) {
		if (debugLevels.containsKey(level)) {
			log.db(debugLevels.get(level), message);
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.log.LogChute#log(int, java.lang.String, java.lang.Throwable)
	 */
	public void log(int level, String message, Throwable t) {
		if (debugLevels.containsKey(level)) {
			log.db(debugLevels.get(level), message);
			log.dbe(debugLevels.get(level), t);
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.log.LogChute#isLevelEnabled(int)
	 */
	public boolean isLevelEnabled(int level) {
		return log.wouldLog(debugLevels.get(level));
	}

}
