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
package panama.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import panama.core.Dispatcher;
import panama.log.SimpleLogger;

/**
 * Provides access to configuration values in an optional panama.properties configuration file.
 *
 * In order to use a different configuration file, you can set a system-property providing a different file name.
 * The system-property must either be named <code>&lt;context&gt;.panama.configuration</code> to only apply to a single web-application in given context
 * or simply <code>panama.configuration</code> in which case it will be used by all panama web applications.
 *
 * @author robert.brandner
 */
public class Configuration {

	/** Simply! Logging */
	protected static SimpleLogger log = new SimpleLogger(Configuration.class);

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
		panamaProperties = readProperties(
				System.getProperty(appName + "." + Dispatcher.PREFIX + ".configuration"),
				System.getProperty(Dispatcher.PREFIX + ".configuration"),
				"/panama.properties");
	}

	/**
	 * returns value for specified property name from the properties file.
	 * @param key
	 * @return the value as string or null if key is null or no entry found
	 */
	public static String getString(String key) {
		return panamaProperties.getProperty(key);
	}

//	/**
//	 * returns Map of all properties where key matches one of the given prefixes
//	 * @param prefix list of prefixes
//	 * @return
//	 */
//	public static Map<String, String> getAll(String... prefix) {
//		Map<String, String> values = new HashMap<String, String>();
//		Enumeration<String> keys = bundle.getKeys();
//		while (keys.hasMoreElements()) {
//			String k = keys.nextElement();
//			for (String p : prefix) {
//				if (k.startsWith(p)) {
//					values.put(k, getString(k));
//					break;
//				}
//			}
//		}
//		return values;
//	}

	/**
	 * Tries to read properties from given fileNameOptions in given order from filesystem and as resource.
	 * As soon as properties could be read for a filename, those are returned.
	 * If there are no properties for any of the fileNameOptions, null is returned.
	 *
	 * @param classLoader
	 * @param fileNameOptions a list of filenames to be tried in given order
	 * @return Properties (will be empty if none of the filenames could be read)
	 */
	public static Properties readProperties(String... fileNameOptions) {
		Properties props = new Properties();
		for (String fileName : fileNameOptions) {
			if (fileName == null) continue;
			InputStream is = null;
			try {
				is = Configuration.class.getResourceAsStream(fileName);
				if (is == null) {	// no resource file, try as regular file
					log.info("could not read "+fileName+" as resource, going to try via file system now.");
					try {
						is = new FileInputStream(new File(fileName));
					} catch (Exception e) {
						log.warn("Error accessing "+fileName+" via file system: "+e.getMessage());
					}
				}
				if (is == null) continue;
				props.load(is);
				break;
			} catch (Exception e) {
				log.warn("Could not read properties from "+fileName+": "+e.getMessage());
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						// NOOP
					}
				}
			}
		}
		return props;
	}
}
