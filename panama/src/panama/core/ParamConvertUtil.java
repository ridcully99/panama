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

import java.io.File;
import java.sql.Timestamp;
import java.util.Calendar;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.CalendarConverter;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.beanutils.converters.FileConverter;
import org.apache.commons.beanutils.converters.SqlDateConverter;
import org.apache.commons.beanutils.converters.SqlTimeConverter;
import org.apache.commons.beanutils.converters.SqlTimestampConverter;

/**
 * Converts string values to all kinds of other types.
 * Used to map parameters to action method arguments.
 * This is only one-way, so cannot be used for Fields etc.
 *
 * @author ridcully
 *
 */
public class ParamConvertUtil extends ConvertUtilsBean {

	public ParamConvertUtil() {
		register(false, true, 0); // don't throw exceptions, use default values

//		// re-register converters for all date related types and allow only millisecond values as valid pattern
//        register(java.util.Date.class, throwException ? new DateConverter()        : new DateConverter(null));
//        register(Calendar.class,      throwException ? new CalendarConverter()     : new CalendarConverter(null));
//        register(File.class,          throwException ? new FileConverter()         : new FileConverter(null));
//        register(java.sql.Date.class, throwException ? new SqlDateConverter()      : new SqlDateConverter(null));
//        register(java.sql.Time.class, throwException ? new SqlTimeConverter()      : new SqlTimeConverter(null));
//        register(Timestamp.class,     throwException ? new SqlTimestampConverter() : new SqlTimestampConverter(null));
	}

}
