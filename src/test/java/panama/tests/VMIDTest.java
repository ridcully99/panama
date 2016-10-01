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
