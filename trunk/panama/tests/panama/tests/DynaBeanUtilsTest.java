package panama.tests;

import panama.util.DynaBeanUtils;
import junit.framework.TestCase;

public class DynaBeanUtilsTest extends TestCase {

	private BeanSupportTestBean b;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		b = new BeanSupportTestBean();
	}
	
	public void testSetProperty1() {
		DynaBeanUtils.setProperty(b, "string", "foobar");
		DynaBeanUtils.setProperty(b, "number", new Long(42));
		assertEquals("foobar", b.getString());
		assertEquals(new Long(42), b.getNumber());
	}
	
	public void testSetProperty2() {
		DynaBeanUtils.setProperty(b, "strings", new String[] {"foo", "bar"});
		assertEquals(2, b.getStrings().length);
	}
	

	public class BeanSupportTestBean {
		private String string;
		private String[] strings;
		private Long number;
		private Long[] numbers;
		
		public String getString() {
			return string;
		}
		public void setString(String string) {
			this.string = string;
		}
		public String[] getStrings() {
			return strings;
		}
		public void setStrings(String[] strings) {
			this.strings = strings;
		}
		public Long getNumber() {
			return number;
		}
		public void setNumber(Long number) {
			this.number = number;
		}
		public Long[] getNumbers() {
			return numbers;
		}
		public void setNumbers(Long[] numbers) {
			this.numbers = numbers;
		}
	}
}
