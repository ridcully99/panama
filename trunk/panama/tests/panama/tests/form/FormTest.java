/*
 * Created on 10.07.2006
 *
 */
package panama.tests.form;

import panama.form.Form;
import panama.util.DynaBeanUtils;
import junit.framework.TestCase;

public class FormTest extends TestCase {

	public FormTest(String arg0) {
		super(arg0);
	}

	public void testAllProperties() {
		TestBean b = new TestBean();
		String[] list = DynaBeanUtils.getPropertyNames(b);
		assertEquals(10, list.length);
	}
	
	public void testAddFields() {
		Form f = new Form();
		f.addFields(TestBean.class);
		assertEquals(10, f.getFields().size());	
		
		Form f2 = new Form();
		f2.addFields(TestBean.class, "bln", "dt", "dbl");
		assertEquals(3, f2.getFields().size());

		Form f3 = new Form();
		f3.addFields(TestBean.class, Form.EXCLUDE_PROPERTIES, "bln", "dt", "dbl" );
		assertEquals(7, f3.getFields().size());
		assertNull(f3.getField("bln"));
		
		Form f4 = new Form();
		f4.addFields(TestBean.class, "unknown");	// should log an error msg at debug level.
		assertEquals(0, f4.getFields().size());		
	}
}
