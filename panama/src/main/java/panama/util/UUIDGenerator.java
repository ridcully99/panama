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
package panama.util;

import java.util.UUID;

/**
 * Generates Base62 encoded RandomUUIDs for use as IDs in PersistentBean etc.
 * Using https://gist.github.com/jdcrensh/4670128
 * All UUIDs are 24 characters long.
 */
public class UUIDGenerator {

	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ012345678";
	private static final char SPECIAL = '9';

	public static final int BASE = ALPHABET.length();

	private UUIDGenerator() {}

	/**
	 * Provides UUID.
	 *
	 * @return
	 */
	public static String getUUID() {
		final UUID uuid = UUID.randomUUID();
		StringBuilder sb = new StringBuilder();
		appendBaseN(sb, uuid.getLeastSignificantBits(), 12);
		appendBaseN(sb, uuid.getMostSignificantBits(), 24);
		return sb.toString();
	}

	/**
	 * Base61-encodes given i. Negative values are prepended by special character not in encoding and encoded
	 * as positive value. Result is padded at the end with special character to given length.
	 *
	 * @param sb
	 * @param i
	 * @param length
	 */
	public static void appendBaseN(StringBuilder sb, long i, int length) {
		if (i < 0) {
			sb.append(SPECIAL);
			i = Math.abs(i);
		}
		while (i > 0) {
			final int rem = (int) (i % BASE);
			sb.append(ALPHABET.charAt(rem));
			i /= BASE;
		}
		while(sb.length() < length) sb.append(SPECIAL);
	}
}