/*
 *  Copyright 2004-2015 Robert Brandner (robert.brandner@gmail.com)
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
package panama.core;

import java.sql.Timestamp;
import java.util.Calendar;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.CalendarConverter;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.beanutils.converters.DateTimeConverter;
import org.apache.commons.beanutils.converters.SqlDateConverter;
import org.apache.commons.beanutils.converters.SqlTimeConverter;
import org.apache.commons.beanutils.converters.SqlTimestampConverter;

/**
 * Converts string values to all kinds of other types.
 * Used to map parameters to action method arguments.
 *
 * For date and time values, this class supports all kinds of ISO patterns defined by {@link #DATE_TIME_PATTERNS}.
 * Also see http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html about the patterns.
 *
 * This is only one-way, so cannot be used for Fields etc.
 *
 * @author ridcully
 *
 */
public class ParamConvertUtil extends ConvertUtilsBean {

	public final static String[] DATE_TIME_PATTERNS = new String[] {
		"yyyy-MM-dd",
		"yyyy-MM-dd HH:mm",
		"yyyy-MM-dd HH:mm:ss",
		"HH:mm",
		"HH:mm:ss",
		"yyyy-MM-dd'T'HH:mm:ss.SSSXXX" // ISO 8601 with Timezone
	};

	public ParamConvertUtil() {
		register(false, true, 0); // don't throw exceptions, use default values

		registerDateTimeConverter(java.util.Date.class, new DateConverter(null));
		registerDateTimeConverter(Calendar.class, new CalendarConverter(null));
		registerDateTimeConverter(java.sql.Date.class, new SqlDateConverter(null));
		registerDateTimeConverter(java.sql.Time.class, new SqlTimeConverter(null));
		registerDateTimeConverter(Timestamp.class, new SqlTimestampConverter(null));
	}

	/**
	 * Registers given converter for given clazz with support for all of DATE_TIME_PATTERNS.
	 * @param clazz
	 * @param converter
	 */
	private void registerDateTimeConverter(Class<?> clazz, DateTimeConverter converter) {
		converter.setPatterns(DATE_TIME_PATTERNS);
		register(converter, clazz);
	}
}
