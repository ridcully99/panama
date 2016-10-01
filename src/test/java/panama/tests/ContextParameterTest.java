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

import java.util.GregorianCalendar;

/**
 * @author ridcully
 *
 */
public class ContextParameterTest extends ContextTestCase {

    public void testStringParams() {
        context.setParameter("null", (String)null);
        context.setParameter("hello", "hello");
        assertNull(context.getParameter("null"));
        assertEquals("default", context.getParameter("null", "default"));
        assertEquals("hello", context.getParameter("hello"));
    }

    public void testIntegerParams() {
        context.setParameter("null", (String)null);
        context.setParameter("one", "1");
        assertNull(context.getParameter("null", Integer.class, null));
        assertEquals(new Integer(1), context.getParameter("one", Integer.class, null));
        assertTrue(1 == context.getIntParameter("one"));
        assertTrue(1 == context.getIntParameter("null", 1));
        assertTrue(0 == context.getParameter("null", int.class, 0));
        assertNull(context.getParameter("unknown", int[].class, null));
        assertTrue(1 == context.getParameter("null", int[].class, null).length);
        assertTrue(99 == context.getParameter("unknown", int[].class, new int[] {99})[0]);
    }

    public void testBooleanParams() {
    	context.setParameter("bool", "true");
    	assertTrue(context.getBooleanParameter("bool"));
    	context.setParameter("bool", "TRUE");
    	assertTrue(context.getBooleanParameter("bool"));
    	assertNull(context.getBooleanParameter("unknown"));
    	assertTrue(context.getBooleanParameter("unknown", true));
    	assertTrue(context.getBooleanParameter("unknown", Boolean.TRUE));
    	assertFalse(context.getParameter("unknown", boolean.class, false));
    	assertNull(context.getParameter("unknown", boolean.class, null));
    	assertTrue(context.getParameter("unknown", boolean.class, true));
    }

    public void testDateParams() {
    	context.setParameter("date", "2015-08-01");
    	assertEquals(new GregorianCalendar(2015, 7, 1).getTime(), context.getDateParameter("date"));
    	context.setParameter("date", "2015-08-01 15:00:00");
    	assertEquals(new GregorianCalendar(2015, 7, 1, 15, 0, 0).getTime(), context.getDateParameter("date"));
    }
}
