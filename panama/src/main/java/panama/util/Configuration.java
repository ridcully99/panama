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
package panama.util;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import panama.core.Dispatcher;
import panama.log.SimpleLogger;

/**
 * Provides access to configuration values in an optional panama.properties configuration file.
 *
 * In order to use a different configuration file, you can set a system-property providing a different file name.
 * The system-property must either be named <code>&lt;context&gt;.panama.configuration</code> to only apply to a single web-application in given context
 * or simply <code>panama.configuration</code> in which case it will be used by all panama web applications.
 *
 * <b>Important:</b>
 * This class provides properties only after it's init() method has been invoked. This is done by Panama automatically during startup.
 * If you use this class in a different way (e.g. Unit Tests), make sure you invoke {@link #init(String)}, before trying to retrieve any configuration settings.
 *
 * @author robert.brandner
 */
public class Configuration {

	public final static String[] BOOLEAN_TRUE = new String[] {"true", "yes", "1"};
	public final static String[] BOOLEAN_FALSE = new String[] {"false", "no", "0"};

	/** Simply! Logging */
	private static SimpleLogger log = new SimpleLogger(Configuration.class);

	// prevent instantiation
	private Configuration() {}

	private static Properties panamaProperties = new Properties();

	/**
	 * This is invoked by the Dispatcher during initialization at start up.
	 * You will normally not use this method, Configuration is all set up after initial start up.
	 *
	 * @param appName
	 */
	public static synchronized void init(String appName) {
		panamaProperties = Dispatcher.readProperties(
				System.getProperty(appName + "." + Dispatcher.PREFIX + ".configuration"),
				System.getProperty(Dispatcher.PREFIX + ".configuration"),
				"/panama.properties",
				"/default-fallback-panama.properties");
	}

	/**
	 * Returns value for specified property name from the properties file.
	 * @param key
	 * @return the value as string or null if key is null or no entry found
	 */
	public static String getString(String key) {
		return panamaProperties.getProperty(key);
	}

	/**
	 * Returns value for specified property name from the properties file.
	 * @param key
	 * @param defaultValue
	 * @return the value as string or defaultValue if key is null or no entry found
	 */
	public static String getString(String key, String defaultValue) {
		String value = getString(key);
		return value == null ? defaultValue : value;
	}

	/**
	 * Returns int value for given property name from properties file.
	 * @param key
	 * @param defaultValue
	 * @return the value as int or defaultValue if property not found or value of property cannot be converted to an integer
	 */
	public static int getInt(String key, int defaultValue) {
		String value = getString(key);
		if (StringUtils.isEmpty(value)) return defaultValue;
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * Returns boolean value for given property name from properties file.
	 * Supported values for boolean properties are {@link #BOOLEAN_TRUE} and {@link #BOOLEAN_FALSE}, case insensitive.
	 *
	 * @param key
	 * @param defaultValue
	 * @return the value as boolean or defaultValue if property not found or value of property cannot be interpreted as boolean
	 */
	public static boolean getBoolean(String key, boolean defaultValue) {
		String value = getString(key);
		if (StringUtils.isEmpty(value)) return defaultValue;
		for (String s : BOOLEAN_TRUE) {
			if (s.equalsIgnoreCase(value)) return true;
		}
		for (String s : BOOLEAN_FALSE) {
			if (s.equalsIgnoreCase(value)) return false;
		}
		return defaultValue;
	}

	/**
	 * Returns complete configuration
	 * @return Properties object containing all configuration
	 */
	public static Properties getAll() {
		return panamaProperties;
	}
}
