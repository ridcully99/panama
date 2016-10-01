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

import panama.util.Configuration;
import junit.framework.TestCase;

/**
 * Dieser Test verwendet die default-fallback-panama.properties die nur für diesen Zweck da sind.
 * @author ridcully
 *
 */
public class ConfigurationTest extends TestCase {

	protected void setUp() throws Exception {
		Configuration.init("test");
	}

	public void testAll() {
		assertEquals("Hello World", Configuration.getString("panama_test_string"));
		assertEquals("Default", Configuration.getString("panama_test_string_x", "Default"));
		assertEquals(42, Configuration.getInt("panama_test_integer", 0));
		assertEquals(0, Configuration.getInt("panama_test_integer_x", 0));
		assertTrue(Configuration.getBoolean("panama_test_boolean_1", false));
		assertFalse(Configuration.getBoolean("panama_test_boolean_2", true));
		assertTrue(Configuration.getBoolean("panama_test_boolean_3", true));
	}
}
