package me.tonsky.toml_clj;

import clojure.lang.*;
import clojure.lang.Compiler;
import java.io.Reader;
import java.util.*;

/**
 * A configurable parser of TOML configurations. It is not thread-safe.
 *
 * @author TheElectronWill
 * @see <a href="https://github.com/toml-lang/toml">TOML specification</a>
 */
public final class TomlParser {
	// --- Parser's settings ---
	private int initialStringBuilderCapacity = 16, initialListCapacity = 10;
	private boolean lenientBareKeys = false;
	private boolean lenientSeparators = false;
	public IFn keyFn = (IFn) Compiler.maybeResolveIn(Namespace.find(Symbol.intern("clojure.core")), Symbol.intern("identity"));

	// --- Parser's state for TOML compliance ---
	private final Set<IPersistentMap> inlineTables = Collections.newSetFromMap(new IdentityHashMap<>());

	void registerInlineTable(IPersistentMap table) {
		inlineTables.add(table);
	}

	boolean isInlineTable(IPersistentMap table) {
		return inlineTables.contains(table);
	}

	private void clearParsingState() {
		inlineTables.clear();
	}

	// --- Parser's methods ---
	public IPersistentMap parse(Reader reader) {
		CharacterInput input = new ReaderInput(reader);
		// this.parsingMode = parsingMode;
		// parsingMode.prepareParsing(destination);
		IPersistentMap rootTable = TableParser.parseNormal(input, this);
		int next;
		while ((next = input.peek()) != -1) {
			final boolean isArray = (next == '[');
			if (isArray) {
				input.skipPeeks();
			}
			final List path = TableParser.parseTableName(input, this, isArray);
			final int lastIndex = path.size() - 1;
			final Object lastKey = path.get(lastIndex);
			final List parentPath = path.subList(0, lastIndex);
			final IPersistentMap parentConfig = getSubtable(rootTable, parentPath);
			// final Map<String, Object> parentMap = (parentConfig != null) ? parentConfig.valueMap()
			// 															 : null;
			if (hasPendingComment()) {// Handles comments that are before the table declaration
				String comment = consumeComment();
				// if (parentConfig instanceof IPersistentMap) {
				// 	List<String> lastPath = Collections.singletonList(lastKey);
				// 	((IPersistentMap)parentConfig).setComment(lastPath, comment);
				// }
			}
			if (isArray) {// It's an element of an array of tables
				if (parentConfig == null) {
					throw new ParsingException("Cannot create entry "
											   + path
											   + " because of an invalid "
											   + "parent that isn't a table.");
				}
				IPersistentMap table = TableParser.parseNormal(input, this);
				IPersistentVector arrayOfTables = (IPersistentVector) parentConfig.valAt(lastKey);
				if (arrayOfTables == null) {
					arrayOfTables = PersistentVector.EMPTY;
					// parentMap.put(lastKey, arrayOfTables);
				}
				// arrayOfTables.add(table);
				arrayOfTables = arrayOfTables.cons(table);
				rootTable = updateSubtable(rootTable, parentPath, parentConfig.assoc(lastKey, arrayOfTables));
				
			} else {// It's a table
				if (parentConfig == null) {
					throw new ParsingException("Cannot create entry "
											   + path
											   + " because of an invalid "
											   + "parent that isn't a table.");
				}
				Object alreadyDeclared = parentConfig.valAt(lastKey);
				if (alreadyDeclared == null) {
					IPersistentMap table = TableParser.parseNormal(input, this);
					rootTable = updateSubtable(rootTable, parentPath, parentConfig.assoc(lastKey, table));
				} else {
					if (alreadyDeclared instanceof IPersistentMap) {
						IPersistentMap table = (IPersistentMap) alreadyDeclared;
						checkContainsOnlySubtables(table, path);
						// IPersistentMap commentedTable = IPersistentMap.fake(table);
						table = TableParser.parseNormal(input, this, table);
						rootTable = updateSubtable(rootTable, parentPath, parentConfig.assoc(lastKey, table));
					} else {
						throw new ParsingException("Entry " + path + " has been defined twice.");
					}
				}
			}
		}
		clearParsingState();
		return rootTable;
	}

	private IPersistentMap getSubtable(IPersistentMap parentTable, List<String> path) {
		if (path.isEmpty()) {
			return parentTable;
		}
		IPersistentMap currentConfig = parentTable;
		for (Object key : path) {
			Object value = currentConfig.valAt(key);
			if (value == null) {
				return PersistentArrayMap.EMPTY;
			} else if (value instanceof IPersistentMap) {
				currentConfig = (IPersistentMap) value;
			} else if (value instanceof List) {
				List<?> list = (List<?>) value;
				if (!list.isEmpty() && list.stream().allMatch(IPersistentMap.class::isInstance)) {// Arrays of tables
					int lastIndex = list.size() - 1;
					currentConfig = (IPersistentMap) list.get(lastIndex);
				} else {
					return null;
				}
			} else {
				return null;
			}
			if (this.isInlineTable(currentConfig)) {
				// reject modification of inline tables
				throw new ParsingException("Cannot modify an inline table after its creation. Key path: " + path);
			}
		}
		return currentConfig;
	}

	private IPersistentMap updateSubtable(IPersistentMap table, List<String> path, IPersistentMap value) {
		// System.out.println("updateSubtable " + table + " " + path + " " + value);
		IPersistentMap res = (IPersistentMap) updateSubtableImpl(table, path, value);
		// System.out.println("  => " + res);
		return res;
	}

	private Object updateSubtableImpl(Object table, List<String> path, IPersistentMap value) {
		if (path.size() > 0) {
			if (table instanceof IPersistentVector) {
				IPersistentVector v = (IPersistentVector) table;
				return v.assoc(v.count() - 1, updateSubtableImpl(v.peek(), path, value));
			}
			if (table == null) {
				table = PersistentArrayMap.EMPTY;
			}
			IPersistentMap m = (IPersistentMap) table;
			Object key = path.get(0);
			Object subtable = m.valAt(key);
			return m.assoc(key, updateSubtableImpl(subtable, path.subList(1, path.size()), value));
		} else if (table instanceof IPersistentVector) {
			IPersistentVector v = (IPersistentVector) table;
			return v.assoc(v.count() - 1, value);
		} else {
			return value;
		}
	}

	private void checkContainsOnlySubtables(IPersistentMap table, List<String> path) {
		for (Object value : (List) RT.vals(table)) {
			if (!(value instanceof IPersistentMap)) {
				throw new ParsingException("Table with path " + path + " has been declared twice.");
			}
		}
	}

	// --- Getters/setters for the settings ---
	public boolean isLenientWithSeparators() {
		return lenientSeparators;
	}

	/**
	 * Makes this parser lenient (if true) or strict (if false - this is the default) with
	 * key/values separators. In lenient mode, the parser accepts both '=' and ':' between
	 * keys and values. In strict mode, only the standard '=' is accepted.
	 *
	 * @param lenientSeparators true for lenient, false for strict
	 * @return this parser
	 */
	public TomlParser setLenientWithSeparators(boolean lenientSeparators) {
		this.lenientSeparators = lenientSeparators;
		return this;
	}

	public boolean isLenientWithBareKeys() {
		return lenientBareKeys;
	}

	/**
	 * Makes this parser lenient (if true) or strict (if false - this is the default) with bar keys.
	 * In lenient mode, almost all characters are allowed in bare keys. In struct mode, only the
	 * standard A-Za-z0-9_- range is allowed.
	 *
	 * @param lenientBareKeys true for lenient, false for strict
	 * @return this parser
	 */
	public TomlParser setLenientWithBareKeys(boolean lenientBareKeys) {
		this.lenientBareKeys = lenientBareKeys;
		return this;
	}

	public TomlParser setInitialStringBuilderCapacity(int initialStringBuilderCapacity) {
		this.initialStringBuilderCapacity = initialStringBuilderCapacity;
		return this;
	}

	public TomlParser setInitialListCapacity(int initialListCapacity) {
		this.initialListCapacity = initialListCapacity;
		return this;
	}

	// --- Configured objects creation ---
	<T> List<T> createList() {
		return new ArrayList<>(initialListCapacity);
	}

	CharsWrapper.Builder createBuilder() {
		return new CharsWrapper.Builder(initialStringBuilderCapacity);
	}

	// --- Comment management ---
	private String currentComment;

	boolean hasPendingComment() {
		return currentComment != null;
	}

	String consumeComment() {
		String comment = currentComment;
		currentComment = null;
		return comment;
	}

	void setComment(CharsWrapper comment) {
		if (comment != null) {
			if (currentComment == null) {
				currentComment = comment.toString();
			} else {
				currentComment = currentComment + '\n' + comment.toString();
			}
		}
	}

	void setComment(List<CharsWrapper> commentsList) {
		CharsWrapper.Builder builder = new CharsWrapper.Builder(32);
		if (!commentsList.isEmpty()) {
			Iterator<CharsWrapper> it = commentsList.iterator();
			builder.append(it.next());
			while (it.hasNext()) {
				builder.append('\n');
				builder.append(it.next());
			}
			setComment(builder.build());// Appends the builder to the current comment if any
		}
	}
}