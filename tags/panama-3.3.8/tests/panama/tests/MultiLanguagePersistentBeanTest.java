/*
 * Created on 24.09.2005
 *
 */
package panama.tests;

import java.util.ArrayList;
import java.util.Locale;

import panama.persistence.PolyglotPersistentBean;

import junit.framework.TestCase;

public class MultiLanguagePersistentBeanTest extends TestCase {

	public MultiLanguagePersistentBeanTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testEqualsAndHashcode() throws Exception {
		PolyglotPersistentBean a = new PolyglotPersistentBean();
		PolyglotPersistentBean b = new PolyglotPersistentBean();
		
		assertFalse(a.equals(b));
		assertFalse(a.hashCode() == b.hashCode());
		
		b.setId(a.getId());
		b.setLanguage(a.getLanguage());
		
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}
	
	public void testTranslateTo() throws Exception {
		TestTestBean a = new TestTestBean();
		a.setI(new Integer(42));
		a.setS("Per Anhalter durch die Galaxis");
		a.setD(999.99d);
		a.setL(new ArrayList());
		a.setDd(new Double(Math.PI));
		TestTestBean b = (TestTestBean)a.translateTo(Locale.ENGLISH);
		assertEquals(a.getI(), b.getI());
		assertEquals(a.getS(), b.getS());
		assertFalse(a.getD() == b.getD()); /* cannot copy primitives */
		assertFalse(a.getL().equals(b.getL()));
		assertEquals(a.getDd(), b.getDd());
	}
}
