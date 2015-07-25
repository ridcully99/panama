/**
 *
 */
package panama.util;

import java.util.UUID;

/**
 * Generates Base62 encoded RandomUUIDs for use as IDs in PersistentBean etc.
 * Using https://gist.github.com/jdcrensh/4670128
 * Negative values are encoded like positive with special character 0 which is not in the alphabet prepended.
 * All UUIDs are padded at the end with special character 0 which is not in the alphabet to 24 characters.
 */
public class UUIDGenerator {

	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";
	private static final char SPECIAL = '0';

	public static final int BASE = ALPHABET.length();

	private UUIDGenerator() {}

	public static String getUUID() {
		final UUID uuid = UUID.randomUUID();
		StringBuilder sb = new StringBuilder();
		append(sb, uuid.getLeastSignificantBits());
		append(sb, uuid.getMostSignificantBits());
		while(sb.length() < 24) sb.append(SPECIAL);
		return sb.toString();
	}

	public static void append(StringBuilder sb, long i) {
		if (i < 0) {
			sb.append(SPECIAL);
			i = Math.abs(i);
		}
		while (i > 0) {
			final int rem = (int) (i % BASE);
			sb.append(ALPHABET.charAt(rem));
			i /= BASE;
		}
	}
}