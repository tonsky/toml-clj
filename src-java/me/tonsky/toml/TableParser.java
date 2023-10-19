package me.tonsky.toml_clj;

import clojure.lang.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author TheElectronWill
 * @see <a href="https://github.com/toml-lang/toml#user-content-table">TOML specification -
 * Tables</a>
 */
final class TableParser {

	private static final char[] KEY_END = {'\t', ' ', '=', '.', '\n', '\r', ']', ':'};

	static IPersistentMap parseInline(CharacterInput input, TomlParser parser) {
		IPersistentMap table = PersistentArrayMap.EMPTY;
		parser.registerInlineTable(table);
		while (true) {
			char keyFirst = Toml.readNonSpaceChar(input, false);
			if (keyFirst == '}') {
				return table;// handles {} and {k1=v1,... ,}
			}
			Object key = parseKey(input, keyFirst, parser);
			char sep = Toml.readNonSpaceChar(input, false);
			checkInvalidSeparator(sep, key, parser);

			Object value = ValueParser.parse(input, parser);
			if (table.containsKey(key)) {
				throw new ParsingException("Invalid TOML data: entry \"" + key + "\" defined twice" + " in its table.");
			}
			table = table.assoc(key, value);

			char after = Toml.readNonSpaceChar(input, false);
			if (after == '}') {
				return table;
			}
			if (after != ',') {
				throw new ParsingException(
						"Invalid entry separator '" + after + "' in inline table.");
			}
		}
	}

	static IPersistentMap parseNormal(CharacterInput input, TomlParser parser, IPersistentMap table) {
		while (true) {
			List<CharsWrapper> commentsList = new ArrayList<>(2);
			int keyFirst = Toml.readUseful(input, commentsList);
			if (keyFirst == -1 || keyFirst == '[') {
				parser.setComment(commentsList);// Saves the comments that are above the next table
				return table;// No more data, or beginning of an other table
			}
			List<String> key = parseDottedKey(input, (char)keyFirst, parser);

			Object value = ValueParser.parse(input, parser);
			Object notFound = new Object();
			if (Utils.getIn(table, key, notFound) != notFound) {
				throw new ParsingException("Invalid TOML data: entry \"" + String.join(".", key) + "\" defined twice" + " in its table.");
			}
			table = Utils.assocIn(table, key, value);

			int after = Toml.readNonSpace(input, false);
			if (after == -1) {// End of the stream
				return table;
			}
			if (after == '#') {
				CharsWrapper comment = Toml.readLine(input);
				commentsList.add(comment);
			} else if (after != '\n' && after != '\r') {
				throw new ParsingException("Invalid character '"
										   + (char)after
										   + "' after table entry \""
										   + key
										   + "\" = "
										   + value);
			}
			parser.setComment(commentsList);
			parser.consumeComment();
		}
	}

	private static void checkInvalidSeparator(char sep, Object key, TomlParser parser) {
		if (!Toml.isKeyValueSeparator(sep, parser.isLenientWithSeparators())) {
			throw new ParsingException(
					"Invalid separator '" + sep + "'after key \"" + key + "\" in some table.");
		}
	}

	static IPersistentMap parseNormal(CharacterInput input, TomlParser parser) {
		return parseNormal(input, parser, PersistentArrayMap.EMPTY);
	}

	static List parseTableName(CharacterInput input, TomlParser parser, boolean array) {
		List list = parser.createList();
		while (true) {
			char firstChar = Toml.readNonSpaceChar(input, false);
			if (firstChar == ']') {
				throw new ParsingException("Tables names must not be empty.");
			}
			Object key = parseKey(input, firstChar, parser);
			list.add(key);

			char separator = Toml.readNonSpaceChar(input, false);
			if (separator == ']') {// End of the declaration
				if (array) {
					char after = input.readChar();
					if (after != ']') {
						throw new ParsingException("Invalid declaration of an element of an array"
												   + " of tables: it ends by ]"
												   + after
												   + " but should end by ]]");

					}
				}
				
				int after = input.readAndSkip(Toml.WHITESPACE);
				if (after == (int) '#') {// Comment
					CharsWrapper comment = Toml.readLine(input);
					parser.setComment(comment);
				} else if (after != -1 && after != (int) '\n' && after != (int) '\r') {
					throw new ParsingException(
							"Invalid character '" + after + "' after a table " + "declaration.");
				}
				return list;
			} else if (separator != '.') {
				throw new ParsingException("Invalid separator '" + separator + "' in table name.");
			}
		}
	}

	static List parseDottedKey(CharacterInput input, char firstChar, TomlParser parser) {
		List list = parser.createList();
		char first = firstChar;
		while (true) {
			Object part = parseKey(input, first, parser);
			list.add(part);

			char sep = Toml.readNonSpaceChar(input, false);
			if (Toml.isKeyValueSeparator(sep, parser.isLenientWithSeparators())) {
				return list;
			} else if (sep != '.') {
				throw new ParsingException("Invalid character '" + sep + "' after key " + list);
			}
			first = Toml.readNonSpaceChar(input, false);
		}
	}

	static Object parseKey(CharacterInput input, char firstChar, TomlParser parser) {
		// Note that a key can't be multiline
		// Empty keys are allowed if and only if they are quoted (with double or single quotes)
		if (firstChar == '\"') {
			return parser.keyFn.invoke(StringParser.parseBasic(input, parser));
		} else if (firstChar == '\'') {
			return parser.keyFn.invoke(StringParser.parseLiteral(input, parser));
		} else {
			CharsWrapper restOfKey = input.readCharsUntil(KEY_END);
			String bareKey = new CharsWrapper.Builder(restOfKey.length() + 1).append(firstChar)
																			 .append(restOfKey)
																			 .toString();
			// Checks that the bare key is conform to the specification
			if (bareKey.isEmpty()) {
				throw new ParsingException("Empty bare keys aren't allowed.");
			}
			if (!Toml.isValidBareKey(bareKey, parser.isLenientWithBareKeys())) {
				throw new ParsingException("Invalid bare key: " + bareKey);
			}
			return parser.keyFn.invoke(bareKey);
		}
	}

	private TableParser() {}
}