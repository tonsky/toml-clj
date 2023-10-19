package me.tonsky.toml_clj;

/**
 * Interface for outputs of characters.
 *
 * @author TheElectronWill
 */
public interface CharacterOutput {
	/**
	 * Writes a character.
	 *
	 * @param c the character to write
	 */
	void write(char c);

	/**
	 * Writes an array of characters.
	 *
	 * @param chars the characters to write
	 */
	default void write(char... chars) {
		write(chars, 0, chars.length);
	}

	/**
	 * Writes a portion of an array of characters.
	 *
	 * @param chars  the characters to write
	 * @param offset the index to start at
	 * @param length the number of characters to write
	 */
	void write(char[] chars, int offset, int length);

	/**
	 * Writes all the characters in the given String.
	 *
	 * @param s the string to write
	 */
	default void write(String s) {
		write(s, 0, s.length());
	}

	/**
	 * Writes a portion of a String.
	 *
	 * @param s      the string to write
	 * @param offset the index to start at
	 * @param length the number of characters to write
	 */
	void write(String s, int offset, int length);

	/**
	 * Writes all the characters in the given CharsWrapper.
	 *
	 * @param cw the CharsWrapper to write
	 */
	default void write(CharsWrapper cw) {
		write(cw.chars, cw.offset, cw.limit - cw.offset);
	}
}