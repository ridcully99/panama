/**
 *
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

	public static String getUUID() {
		final UUID uuid = UUID.randomUUID();
		StringBuilder sb = new StringBuilder();
		append(sb, uuid.getLeastSignificantBits(), 12);
		append(sb, uuid.getMostSignificantBits(), 24);
		return sb.toString();
	}

	public static void append(StringBuilder sb, long i, int length) {
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