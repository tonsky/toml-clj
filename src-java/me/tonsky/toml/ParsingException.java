package me.tonsky.toml_clj;

/**
 * Thrown when a parsing operation fails.
 *
 * @author TheElectronWill
 */
public class ParsingException extends RuntimeException {
	public ParsingException(String message) {
		super(message);
	}

	public ParsingException(String message, Throwable cause) {
		super(message, cause);
	}

	public static ParsingException readFailed(Throwable cause) {
		return new ParsingException("Failed to parse data from Reader", cause);
	}

	public static ParsingException notEnoughData() {
		return new ParsingException("Not enough data available");
	}
}