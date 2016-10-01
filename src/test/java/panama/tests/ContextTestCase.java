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
import java.util.Locale;

import junit.framework.TestCase;
import panama.core.Context;
import panama.core.Dispatcher;

/**
 *
 */

/**
 * An extension to the normal TestCase class, that creates for you a fully initialized Context object (with Mock objects for Request, Response and Session)
 * This is very useful for writing unit tests for your controller classes.
 *
 * @author robert.brandner
 *
 */
public class ContextTestCase extends TestCase {

	protected Context context;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Dispatcher dispatcher = new Dispatcher();
		context = Context.createInstance(dispatcher, new MockHttpSession(), new MockRequest(), new MockResponse(), Locale.ENGLISH);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Context.destroyInstance();
	}

	public void testEmpty() {
		// prevents JUnit Test Runner from creating a warning
	}
}
