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
	
}
