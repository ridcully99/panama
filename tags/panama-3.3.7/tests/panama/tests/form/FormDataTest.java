/*
 * Created on 10.07.2006
 *
 */
package panama.tests.form;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import junit.framework.TestCase;
import panama.core.Context;
import panama.form.Form;
import panama.form.FormData;
import panama.form.LongField;
import panama.persistence.PersistentBean;

public class FormDataTest extends TestCase {

	public FormDataTest(String arg0) {
		super(arg0);
	}

	@Override
	protected void setUp() throws Exception {
		Context.createInstance(null, null, null, null, Locale.getDefault());
	}
	
	public void testApplyTo() {
		Form f = new Form();
		f.addFields(TestBean.class);
		FormData fd = new FormData(f);
		TestBean src = makeTestBean();
		fd.setInput(src);
		TestBean target = new TestBean();
		fd.applyTo(target, Form.EXCLUDE_PROPERTIES, "prst");
		assertEquals(src.getBln(), target.getBln());
	}
	
	public void testApplyToWithArrays() {
		Form f = new Form();
		f.addFields(TestBean.class);
		FormData fd = new FormData(f);
		TestBean src = makeTestBean();
		src.setStrngs(new String[] {"foo", "bar"});
		src.setLngs(new Long[] {42L, 4711L});
		fd.setInput(src);
		TestBean target = new TestBean();
		fd.applyTo(target, Form.EXCLUDE_PROPERTIES, "prst");
		assertEquals(2, target.getStrngs().length);
		assertEquals(2, target.getLngs().length);
	}	
	
	public void testApplyEmptyTo() {
		Form f = new Form();
		f.addFields(TestBean.class);
		FormData fd = new FormData(f);
		TestBean src = new TestBean();
		fd.setInput(src);
		TestBean target = new TestBean();
		fd.applyTo(target, Form.EXCLUDE_PROPERTIES, "prst");
		assertEquals(src.getBln(), target.getBln());
	}
	
	public void testValuesAsList() throws Exception {
		Form f = new Form();
		f.addFields(TestBean.class);
		FormData fd = new FormData(f);
		fd.setInput("strng", null);
		List l = fd.getValuesAsList("strng");
		assertTrue(l.isEmpty());
		try {
			l = fd.getValuesAsList("foobar");
			assertFalse(true);
		} catch (Exception e) {
			assertTrue(true);
		}
		fd.setInput("strng", "foo");
		l = fd.getValuesAsList("strng");
		assertTrue(l.get(0).equals("foo"));		
	}
	
	public void testValidateNotEmptyForNotPassedInput() {
		Form f = new Form();
		f.addField(new LongField("id", true));
		FormData fd = new FormData(f);
		Long id = fd.getLong("id");
		assertTrue("fd has errors", fd.hasErrors());
	}
	
	// ------------------------------------------------------------- helpers
	
	private TestBean makeTestBean() {
		TestBean b = new TestBean();
		b.setBln(Boolean.TRUE);
		b.setDbl(Double.valueOf(42d));
		b.setDt(new Date());
		b.setFlt(Float.valueOf(42f));
		b.setLng(Long.valueOf(42));
		b.setNtgr(Integer.valueOf(42));
		b.setPrst(new PersistentBean());
		b.setStrng("pandora");
		return b;
	}
}
