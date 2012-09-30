/*
 * Created on 14.08.2005
 *
 */
package panama.tests;

import java.rmi.dgc.VMID;

import junit.framework.TestCase;

public class VMIDTest extends TestCase {

	public VMIDTest(String s) {
		super(s);
	}
	
	public void testVMID() throws Exception {
	    VMID guid = new VMID();
	    for (int i=0; i<1000; i++) {
	    	guid = new VMID();
	    	System.out.println(guid.toString());
	    }
	}
}
