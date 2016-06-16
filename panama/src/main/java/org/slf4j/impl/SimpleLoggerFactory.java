/*
 *  Copyright 2004-2013 Robert Brandner (robert.brandner@gmail.com) 
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
package org.slf4j.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import panama.log.SimpleLogger;

/**
 * @author ridcully
 * 
 */
public class SimpleLoggerFactory implements ILoggerFactory {

	// key: name (String), value: a Log4jLoggerAdapter;
	ConcurrentMap<String, Logger> loggerMap;

	public SimpleLoggerFactory() {
		loggerMap = new ConcurrentHashMap<String, Logger>();
	}

	/**
	 * SimpleLogger instances are always created for classes. So we suppose name
	 * is a FQCN and try to get a class for it. If that fails, we fall back to
	 * or empty Root class.
	 * 
	 * @see org.slf4j.ILoggerFactory#getLogger(java.lang.String)
	 */
	public Logger getLogger(String name) {

		// the root logger is called "" in JUL
		if (name.equalsIgnoreCase(Logger.ROOT_LOGGER_NAME)) {
			name = "";
		}

		Logger slf4jLogger = loggerMap.get(name);
		if (slf4jLogger != null)
			return slf4jLogger;
		else {
			Class<?> clazz;
			try {
				clazz = Class.forName(name);
			} catch (Exception e) {
				clazz = Root.class;
			}
			SimpleLogger simpleLogger = new SimpleLogger(clazz);
			Logger newInstance = new SimpleLoggerAdapter(simpleLogger);
			Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
			return oldInstance == null ? newInstance : oldInstance;
		}
	}
}
