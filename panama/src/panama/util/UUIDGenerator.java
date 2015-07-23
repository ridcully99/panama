/**
 *
 */
package panama.util;

import java.util.UUID;

/**
 * Generates Base62 encoded RandomUUIDs for use as IDs in PersistentBean etc.
 * Using https://gist.github.com/jdcrensh/4670128
 */
public class UUIDGenerator {

	public static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	public static final int BASE = ALPHABET.length();

	private UUIDGenerator() {}
	
	public static String getUUID() {
		final UUID uuid = UUID.randomUUID();
		StringBuilder sb = new StringBuilder();
		append(sb, uuid.getLeastSignificantBits());
		append(sb, uuid.getMostSignificantBits());
		return sb.toString();
	}

	public static void append(StringBuilder sb, long i) {
		while (i != 0) {
			final int rem = (int) (i % BASE);
			sb.append(ALPHABET.charAt(Math.abs(rem)));
			i /= BASE;
		}
	}
}