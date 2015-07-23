/*
 *  Copyright 2004-2015 Robert Brandner (robert.brandner@gmail.com) 
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
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import panama.util.UUIDGenerator;

/**
 * @author robert.brandner
 *
 */
public class UUIDTest extends TestCase {

	private final static int TIMES = 10000;
	
	public void testPerformance() {
		long vmidMillis = doVMID();
		long uuidGeneratorMillis = doUUIDGenerator();
		System.out.println(vmidMillis);
		System.out.println(uuidGeneratorMillis);
	}
	
	public void testUUIDGenerator() {
		for (int i = 0; i < 10; i++) {
			System.out.println(i + " : " + UUIDGenerator.getUUID());
		}
	}
	
	public void testUUIDGenerator_Uniqueness() {
		Set<String> values = new HashSet<String>();
		for (int i = 0; i < TIMES * 1000; i++) {
			String uuid = UUIDGenerator.getUUID();
			assertFalse(values.contains(uuid));
			values.add(uuid);
		}
	}

	private long doVMID() {
		long start = System.currentTimeMillis();
		for (int i=0; i < TIMES; i++) {
			String id = new VMID().toString().replace(':', 'x');
		}
		return System.currentTimeMillis() - start;
	}

	private long doUUIDGenerator() {
		long start = System.currentTimeMillis();
		for (int i=0; i < TIMES; i++) {
			String s = UUIDGenerator.getUUID();
		}
		return System.currentTimeMillis() - start;
	}
}
