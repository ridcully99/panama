/*
 * Created on 03.09.2005
 *
 */
package panama.tests;

import panama.collections.DefaultTree;
import junit.framework.TestCase;

public class TreeTest extends TestCase {

	public TreeTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testToggle() throws Exception {
		DefaultTree t = new DefaultTree("key");
		String nodeId1 = "nodeid1";
		assertTrue(t.isClosed(nodeId1));
		t.toggleNode(nodeId1);
		assertTrue(t.isOpen(nodeId1));
		t.toggleNode(nodeId1);
		assertTrue(t.isClosed(nodeId1));		
	}
}
