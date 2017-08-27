/**
 * Copyright (c) 2008-2011, http://www.snakeyaml.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.yaml.snakeyaml.scanner;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.tokens.AliasToken;
import org.yaml.snakeyaml.tokens.AnchorToken;
import org.yaml.snakeyaml.tokens.BlockEndToken;
import org.yaml.snakeyaml.tokens.BlockEntryToken;
import org.yaml.snakeyaml.tokens.BlockMappingStartToken;
import org.yaml.snakeyaml.tokens.BlockSequenceStartToken;
import org.yaml.snakeyaml.tokens.DirectiveToken;
import org.yaml.snakeyaml.tokens.DocumentEndToken;
import org.yaml.snakeyaml.tokens.DocumentStartToken;
import org.yaml.snakeyaml.tokens.FlowEntryToken;
import org.yaml.snakeyaml.tokens.FlowMappingEndToken;
import org.yaml.snakeyaml.tokens.FlowMappingStartToken;
import org.yaml.snakeyaml.tokens.FlowSequenceEndToken;
import org.yaml.snakeyaml.tokens.FlowSequenceStartToken;
import org.yaml.snakeyaml.tokens.KeyToken;
import org.yaml.snakeyaml.tokens.ScalarToken;
import org.yaml.snakeyaml.tokens.StreamEndToken;
import org.yaml.snakeyaml.tokens.StreamStartToken;
import org.yaml.snakeyaml.tokens.TagToken;
import org.yaml.snakeyaml.tokens.TagTuple;
import org.yaml.snakeyaml.tokens.Token;
import org.yaml.snakeyaml.tokens.ValueToken;
import org.yaml.snakeyaml.util.ArrayStack;
import org.yaml.snakeyaml.util.UriEncoder;

/**
 * <pre>
 * Scanner produces tokens of the following types:
 * STREAM-START
 * STREAM-END
 * DIRECTIVE(name, value)
 * DOCUMENT-START
 * DOCUMENT-END
 * BLOCK-SEQUENCE-START
 * BLOCK-MAPPING-START
 * BLOCK-END
 * FLOW-SEQUENCE-START
 * FLOW-MAPPING-START
 * FLOW-SEQUENCE-END
 * FLOW-MAPPING-END
 * BLOCK-ENTRY
 * FLOW-ENTRY
 * KEY
 * VALUE
 * ALIAS(value)
 * ANCHOR(value)
 * TAG(value)
 * SCALAR(value, plain, style)
 * Read comments in the Scanner code for more details.
 * </pre>
 */
public final class ScannerImpl implements Scanner {
    private final static Pattern NOT_HEXA = Pattern.compile("[^0-9A-Fa-f]");
    public final static Map<Character, String> ESCAPE_REPLACEMENTS = new HashMap<Character, String>();
    public final static Map<Character, Integer> ESCAPE_CODES = new HashMap<Character, Integer>();

    static {
        ESCAPE_REPLACEMENTS.put(new Character('0'), "\0");
        ESCAPE_REPLACEMENTS.put(new Character('a'), "\u0007");
        ESCAPE_REPLACEMENTS.put(new Character('b'), "\u0008");
        ESCAPE_REPLACEMENTS.put(new Character('t'), "\u0009");
        ESCAPE_REPLACEMENTS.put(new Character('n'), "\n");
        ESCAPE_REPLACEMENTS.put(new Character('v'), "\u000B");
        ESCAPE_REPLACEMENTS.put(new Character('f'), "\u000C");
        ESCAPE_REPLACEMENTS.put(new Character('r'), "\r");
        ESCAPE_REPLACEMENTS.put(new Character('e'), "\u001B");
        ESCAPE_REPLACEMENTS.put(new Character(' '), "\u0020");
        ESCAPE_REPLACEMENTS.put(new Character('"'), "\"");
        ESCAPE_REPLACEMENTS.put(new Character('\\'), "\\");
        ESCAPE_REPLACEMENTS.put(new Character('N'), "\u0085");
        ESCAPE_REPLACEMENTS.put(new Character('_'), "\u00A0");
        ESCAPE_REPLACEMENTS.put(new Character('L'), "\u2028");
        ESCAPE_REPLACEMENTS.put(new Character('P'), "\u2029");

        ESCAPE_CODES.put(new Character('x'), 2);
        ESCAPE_CODES.put(new Character('u'), 4);
        ESCAPE_CODES.put(new Character('U'), 8);
    }
    private final StreamReader reader;
    // Had we reached the end of the stream?
    private boolean done = false;

    // The number of unclosed '{' and '['. `flow_level == 0` means block
    // context.
    private int flowLevel = 0;

    // List of processed tokens that are not yet emitted.
    private List<Token> tokens;

    // Number of tokens that were emitted through the `get_token` method.
    private int tokensTaken = 0;

    // The current indentation level.
    private int indent = -1;

    // Past indentation levels.
    private ArrayStack<Integer> indents;

    // Variables related to simple keys treatment. See PyYAML.

    /**
     * <pre>
     * A simple key is a key that is not denoted by the '?' indicator.
     * Example of simple keys:
     *   ---
     *   block simple key: value
     *   ? not a simple key:
     *   : { flow simple key: value }
     * We emit the KEY token before all keys, so when we find a potential
     * simple key, we try to locate the corresponding ':' indicator.
     * Simple keys should be limited to a single line and 1024 characters.
     * 
     * Can a simple key start at the current position? A simple key may
     * start:
     * - at the beginning of the line, not counting indentation spaces
     *       (in block context),
     * - after '{', '[', ',' (in the flow context),
     * - after '?', ':', '-' (in the block context).
     * In the block context, this flag also signifies if a block collection
     * may start at the current position.
     * </pre>
     */
    private boolean allowSimpleKey = true;

    /*
     * Keep track of possible simple keys. This is a dictionary. The key is
     * `flow_level`; there can be no more that one possible simple key for each
     * level. The value is a SimpleKey record: (token_number, required, index,
     * line, column, mark) A simple key may start with ALIAS, ANCHOR, TAG,
     * SCALAR(flow), '[', or '{' tokens.
     */
    private Map<Integer, SimpleKey> possibleSimpleKeys;

    public ScannerImpl(StreamReader reader) {
        this.reader = reader;
        this.tokens = new ArrayList<Token>(100);
        this.indents = new ArrayStack<Integer>(10);
        // the order in possibleSimpleKeys is kept for nextPossibleSimpleKey()
        this.possibleSimpleKeys = new LinkedHashMap<Integer, SimpleKey>();
        fetchStreamStart();// Add the STREAM-START token.
    }

    /**
     * Check if the next token is one of the given types.
     */
    public boolean checkToken(Token.ID... choices) {
        while (needMoreTokens()) {
            fetchMoreTokens();
        }
        if (!this.tokens.isEmpty()) {
            if (choices.length == 0) {
                return true;
            }
            // since profiler puts this method on top we should not use
            // 'foreach' here
            Token.ID first = this.tokens.get(0).getTokenId();
            for (int i = 0; i < choices.length; i++) {
                if (first == choices[i]) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return the next token, but do not delete if from the queue.
     */
    public Token peekToken() {
        while (needMoreTokens()) {
            fetchMoreTokens();
        }
        return this.tokens.get(0);
    }

    /**
     * Return the next token.
     */
    public Token getToken() {
        if (!this.tokens.isEmpty()) {
            this.tokensTaken++;
            return this.tokens.remove(0);
        }
        return null;
    }

    // Private methods.

    private boolean needMoreTokens() {
        if (this.done) {
            return false;
        }
        if (this.tokens.isEmpty()) {
            return true;
        }
        // The current token may be a potential simple key, so we
        // need to look further.
        stalePossibleSimpleKeys();
        return nextPossibleSimpleKey() == this.tokensTaken;
    }

    private void fetchMoreTokens() {
        // Eat whitespaces and comments until we reach the next token.
        scanToNextToken();
        // Remove obsolete possible simple keys.
        stalePossibleSimpleKeys();
        // Compare the current indentation and column. It may add some tokens
        // and decrease the current indentation level.
        unwindIndent(reader.getColumn());
        // Peek the next character.
        char ch = reader.peek();
        switch (ch) {
        case '\0':
            // Is it the end of stream?
            fetchStreamEnd();
            return;
        case '%':
            // Is it a directive?
            if (checkDirective()) {
                fetchDirective();
                return;
            }
            break;
        case '-':
            // Is it the document start?
            if (checkDocumentStart()) {
                fetchDocumentStart();
                return;
                // Is it the block entry indicator?
            } else if (checkBlockEntry()) {
                fetchBlockEntry();
                return;
            }
            break;
        case '.':
            // Is it the document end?
            if (checkDocumentEnd()) {
                fetchDocumentEnd();
                return;
            }
            break;
        // TODO support for BOM within a stream. (not implemented in PyYAML)
        case '[':
            // Is it the flow sequence start indicator?
            fetchFlowSequenceStart();
            return;
        case '{':
            // Is it the flow mapping start indicator?
            fetchFlowMappingStart();
            return;
        case ']':
            // Is it the flow sequence end indicator?
            fetchFlowSequenceEnd();
            return;
        case '}':
            // Is it the flow mapping end indicator?
            fetchFlowMappingEnd();
            return;
        case ',':
            // Is it the flow entry indicator?
            fetchFlowEntry();
            return;
            // see block entry indicator above
        case '?':
            // Is it the key indicator?
            if (checkKey()) {
                fetchKey();
                return;
            }
            break;
        case ':':
            // Is it the value indicator?
            if (checkValue()) {
                fetchValue();
                return;
            }
            break;
        case '*':
            // Is it an alias?
            fetchAlias();
            return;
        case '&':
            // Is it an anchor?
            fetchAnchor();
            return;
        case '!':
            // Is it a tag?
            fetchTag();
            return;
        case '|':
            // Is it a literal scalar?
            if (this.flowLevel == 0) {
                fetchLiteral();
                return;
            }
            break;
        case '>':
            // Is it a folded scalar?
            if (this.flowLevel == 0) {
                fetchFolded();
                return;
            }
            break;
        case '\'':
            // Is it a single quoted scalar?
            fetchSingle();
            return;
        case '"':
            // Is it a double quoted scalar?
            fetchDouble();
            return;
        }
        // It must be a plain scalar then.
        if (checkPlain()) {
            fetchPlain();
            return;
        }
        // No? It's an error. Let's produce a nice error message.
        String chRepresentation = String.valueOf(ch);
        for (Character s : ESCAPE_REPLACEMENTS.keySet()) {
            String v = ESCAPE_REPLACEMENTS.get(s);
            if (v.equals(chRepresentation)) {
                chRepresentation = "\\" + s;// ' ' -> '\t'
                break;
            }
        }
        throw new ScannerException("while scanning for the next token", null, "found character "
                + ch + "'" + chRepresentation + "' that cannot start any token", reader.getMark());
    }

    // Simple keys treatment.

    /**
     * Return the number of the nearest possible simple key. Actually we don't
     * need to loop through the whole dictionary.
     */
    private int nextPossibleSimpleKey() {
        /*
         * the implementation is not as in PyYAML. Because
         * this.possibleSimpleKeys is ordered we can simply take the first key
         */
        if (!this.possibleSimpleKeys.isEmpty()) {
            return this.possibleSimpleKeys.values().iterator().next().getTokenNumber();
        }
        return -1;
    }

    /**
     * <pre>
     * Remove entries that are no longer possible simple keys. According to
     * the YAML specification, simple keys
     * - should be limited to a single line,
     * - should be no longer than 1024 characters.
     * Disabling this procedure will allow simple keys of any length and
     * height (may cause problems if indentation is broken though).
     * </pre>
     */
    private void stalePossibleSimpleKeys() {
        // use toRemove to avoid java.util.ConcurrentModificationException
        if (!this.possibleSimpleKeys.isEmpty()) {
            for (Iterator<SimpleKey> iterator = this.possibleSimpleKeys.values().iterator(); iterator
                    .hasNext();) {
                SimpleKey key = iterator.next();
                if ((key.getLine() != reader.getLine())
                        || (reader.getIndex() - key.getIndex() > 1024)) {
                    if (key.isRequired()) {
                        throw new ScannerException("while scanning a simple key", key.getMark(),
                                "could not found expected ':'", reader.getMark());
                    }
                    iterator.remove();
                }
            }
        }
    }

    /**
     * The next token may start a simple key. We check if it's possible and save
     * its position. This function is called for ALIAS, ANCHOR, TAG,
     * SCALAR(flow), '[', and '{'.
     */
    private void savePossibleSimpleKey() {
        // The next token may start a simple key. We check if it's possible
        // and save its position. This function is called for
        // ALIAS, ANCHOR, TAG, SCALAR(flow), '[', and '{'.

        // Check if a simple key is required at the current position.
        boolean required = ((this.flowLevel == 0) && (this.indent == this.reader.getColumn()));

        if (allowSimpleKey || !required) {
            // A simple key is required only if it is the first token in the
            // current
            // line. Therefore it is always allowed.
        } else {
            throw new YAMLException(
                    "A simple key is required only if it is the first token in the current line");
        }

        // The next token might be a simple key. Let's save it's number and
        // position.
        if (this.allowSimpleKey) {
            removePossibleSimpleKey();
            int tokenNumber = this.tokensTaken + this.tokens.size();
            SimpleKey key = new SimpleKey(tokenNumber, required, reader.getIndex(),
                    reader.getLine(), this.reader.getColumn(), this.reader.getMark());
            this.possibleSimpleKeys.put(this.flowLevel, key);
        }
    }

    /**
     * Remove the saved possible key position at the current flow level.
     */
    private void removePossibleSimpleKey() {
        SimpleKey key = possibleSimpleKeys.remove(flowLevel);
        if (key != null && key.isRequired()) {
            throw new ScannerException("while scanning a simple key", key.getMark(),
                    "could not found expected ':'", reader.getMark());
        }
    }

    // Indentation functions.

    /**
     * <pre>
     * In flow context, tokens should respect indentation.
     * Actually the condition should be `self.indent &gt;= column` according to
     * the spec. But this condition will prohibit intuitively correct
     * constructions such as
     * key : {
     * }
     * </pre>
     */
    private void unwindIndent(int col) {
        // In the flow context, indentation is ignored. We make the scanner less
        // restrictive then specification requires.
        if (this.flowLevel != 0) {
            return;
        }

        // In block context, we may need to issue the BLOCK-END tokens.
        while (this.indent > col) {
            Mark mark = reader.getMark();
            this.indent = this.indents.pop();
            this.tokens.add(new BlockEndToken(mark, mark));
        }
    }

    /**
     * Check if we need to increase indentation.
     */
    private boolean addIndent(int column) {
        if (this.indent < column) {
            this.indents.push(this.indent);
            this.indent = column;
            return true;
        }
        return false;
    }

    // Fetchers.

    /**
     * We always add STREAM-START as the first token and STREAM-END as the last
     * token.
     */
    private void fetchStreamStart() {
        // Read the token.
        Mark mark = reader.getMark();

        // Add STREAM-START.
        Token token = new StreamStartToken(mark, mark);
        this.tokens.add(token);
    }

    private void fetchStreamEnd() {
        // Set the current intendation to -1.
        unwindIndent(-1);

        // Reset simple keys.
        removePossibleSimpleKey();
        this.allowSimpleKey = false;
        this.possibleSimpleKeys.clear();

        // Read the token.
        Mark mark = reader.getMark();

        // Add STREAM-END.
        Token token = new StreamEndToken(mark, mark);
        this.tokens.add(token);

        // The stream is finished.
        this.done = true;
    }

    private void fetchDirective() {
        // Set the current intendation to -1.
        unwindIndent(-1);

        // Reset simple keys.
        removePossibleSimpleKey();
        this.allowSimpleKey = false;

        // Scan and add DIRECTIVE.
        Token tok = scanDirective();
        this.tokens.add(tok);
    }

    private void fetchDocumentStart() {
        fetchDocumentIndicator(true);
    }

    private void fetchDocumentEnd() {
        fetchDocumentIndicator(false);
    }

    private void fetchDocumentIndicator(boolean isDocumentStart) {
        // Set the current intendation to -1.
        unwindIndent(-1);

        // Reset simple keys. Note that there could not be a block collection
        // after '---'.
        removePossibleSimpleKey();
        this.allowSimpleKey = false;

        // Add DOCUMENT-START or DOCUMENT-END.
        Mark startMark = reader.getMark();
        reader.forward(3);
        Mark endMark = reader.getMark();
        Token token;
        if (isDocumentStart) {
            token = new DocumentStartToken(startMark, endMark);
        } else {
            token = new DocumentEndToken(startMark, endMark);
        }
        this.tokens.add(token);
    }

    private void fetchFlowSequenceStart() {
        fetchFlowCollectionStart(false);
    }

    private void fetchFlowMappingStart() {
        fetchFlowCollectionStart(true);
    }

    private void fetchFlowCollectionStart(boolean isMappingStart) {
        // '[' and '{' may start a simple key.
        savePossibleSimpleKey();

        // Increase the flow level.
        this.flowLevel++;

        // Simple keys are allowed after '[' and '{'.
        this.allowSimpleKey = true;

        // Add FLOW-SEQUENCE-START or FLOW-MAPPING-START.
        Mark startMark = reader.getMark();
        reader.forward(1);
        Mark endMark = reader.getMark();
        Token token;
        if (isMappingStart) {
            token = new FlowMappingStartToken(startMark, endMark);
        } else {
            token = new FlowSequenceStartToken(startMark, endMark);
        }
        this.tokens.add(token);
    }

    private void fetchFlowSequenceEnd() {
        fetchFlowCollectionEnd(false);
    }

    private void fetchFlowMappingEnd() {
        fetchFlowCollectionEnd(true);
    }

    private void fetchFlowCollectionEnd(boolean isMappingEnd) {
        // Reset possible simple key on the current level.
        removePossibleSimpleKey();

        // Decrease the flow level.
        this.flowLevel--;

        // No simple keys after ']' or '}'.
        this.allowSimpleKey = false;

        // Add FLOW-SEQUENCE-END or FLOW-MAPPING-END.
        Mark startMark = reader.getMark();
        reader.forward();
        Mark endMark = reader.getMark();
        Token token;
        if (isMappingEnd) {
            token = new FlowMappingEndToken(startMark, endMark);
        } else {
            token = new FlowSequenceEndToken(startMark, endMark);
        }
        this.tokens.add(token);
    }

    private void fetchFlowEntry() {
        // Simple keys are allowed after ','.
        this.allowSimpleKey = true;

        // Reset possible simple key on the current level.
        removePossibleSimpleKey();

        // Add FLOW-ENTRY.
        Mark startMark = reader.getMark();
        reader.forward();
        Mark endMark = reader.getMark();
        Token token = new FlowEntryToken(startMark, endMark);
        this.tokens.add(token);
    }

    private void fetchBlockEntry() {
        // Block context needs additional checks.
        if (this.flowLevel == 0) {
            // Are we allowed to start a new entry?
            if (!this.allowSimpleKey) {
                throw new ScannerException(null, null, "sequence entries are not allowed here",
                        reader.getMark());
            }

            // We may need to add BLOCK-SEQUENCE-START.
            if (addIndent(this.reader.getColumn())) {
                Mark mark = reader.getMark();
                this.tokens.add(new BlockSequenceStartToken(mark, mark));
            }
        } else {
            // It's an error for the block entry to occur in the flow
            // context,but we let the parser detect this.
        }
        // Simple keys are allowed after '-'.
        this.allowSimpleKey = true;

        // Reset possible simple key on the current level.
        removePossibleSimpleKey();

        // Add BLOCK-ENTRY.
        Mark startMark = reader.getMark();
        reader.forward();
        Mark endMark = reader.getMark();
        Token token = new BlockEntryToken(startMark, endMark);
        this.tokens.add(token);
    }

    private void fetchKey() {
        // Block context needs additional checks.
        if (this.flowLevel == 0) {
            // Are we allowed to start a key (not necessary a simple)?
            if (!this.allowSimpleKey) {
                throw new ScannerException(null, null, "mapping keys are not allowed here",
                        reader.getMark());
            }
            // We may need to add BLOCK-MAPPING-START.
            if (addIndent(this.reader.getColumn())) {
                Mark mark = reader.getMark();
                this.tokens.add(new BlockMappingStartToken(mark, mark));
            }
        }
        // Simple keys are allowed after '?' in the block context.
        this.allowSimpleKey = this.flowLevel == 0;

        // Reset possible simple key on the current level.
        removePossibleSimpleKey();

        // Add KEY.
        Mark startMark = reader.getMark();
        reader.forward();
        Mark endMark = reader.getMark();
        Token token = new KeyToken(startMark, endMark);
        this.tokens.add(token);
    }

    private void fetchValue() {
        // Do we determine a simple key?
        SimpleKey key = this.possibleSimpleKeys.remove(this.flowLevel);
        if (key != null) {
            // Add KEY.
            this.tokens.add(key.getTokenNumber() - this.tokensTaken, new KeyToken(key.getMark(),
                    key.getMark()));

            // If this key starts a new block mapping, we need to add
            // BLOCK-MAPPING-START.
            if (this.flowLevel == 0) {
                if (addIndent(key.getColumn())) {
                    this.tokens.add(key.getTokenNumber() - this.tokensTaken,
                            new BlockMappingStartToken(key.getMark(), key.getMark()));
                }
            }
            // There cannot be two simple keys one after another.
            this.allowSimpleKey = false;

        } else {// It must be a part of a complex key.
            // Block context needs additional checks.Do we really need them?
            // They
            // will be catched by the parser anyway.)
            if (this.flowLevel == 0) {

                // We are allowed to start a complex value if and only if we can
                // start a simple key.
                if (!this.allowSimpleKey) {
                    throw new ScannerException(null, null, "mapping values are not allowed here",
                            reader.getMark());
                }
            }

            // If this value starts a new block mapping, we need to add
            // BLOCK-MAPPING-START. It will be detected as an error later by
            // the parser.
            if (flowLevel == 0) {
                if (addIndent(reader.getColumn())) {
                    Mark mark = reader.getMark();
                    this.tokens.add(new BlockMappingStartToken(mark, mark));
                }
            }

            // Simple keys are allowed after ':' in the block context.
            allowSimpleKey = (flowLevel == 0);

            // Reset possible simple key on the current level.
            removePossibleSimpleKey();
        }
        // Add VALUE.
        Mark startMark = reader.getMark();
        reader.forward();
        Mark endMark = reader.getMark();
        Token token = new ValueToken(startMark, endMark);
        this.tokens.add(token);
    }

    private void fetchAlias() {
        // ALIAS could be a simple key.
        savePossibleSimpleKey();

        // No simple keys after ALIAS.
        this.allowSimpleKey = false;

        // Scan and add ALIAS.
        Token tok = scanAnchor(false);
        this.tokens.add(tok);
    }

    private void fetchAnchor() {
        // ANCHOR could start a simple key.
        savePossibleSimpleKey();

        // No simple keys after ANCHOR.
        this.allowSimpleKey = false;

        // Scan and add ANCHOR.
        Token tok = scanAnchor(true);
        this.tokens.add(tok);
    }

    private void fetchTag() {
        // TAG could start a simple key.
        savePossibleSimpleKey();

        // No simple keys after TAG.
        this.allowSimpleKey = false;

        // Scan and add TAG.
        Token tok = scanTag();
        this.tokens.add(tok);
    }

    private void fetchLiteral() {
        fetchBlockScalar('|');
    }

    private void fetchFolded() {
        fetchBlockScalar('>');
    }

    private void fetchBlockScalar(char style) {
        // A simple key may follow a block scalar.
        this.allowSimpleKey = true;

        // Reset possible simple key on the current level.
        removePossibleSimpleKey();

        // Scan and add SCALAR.
        Token tok = scanBlockScalar(style);
        this.tokens.add(tok);
    }

    private void fetchSingle() {
        fetchFlowScalar('\'');
    }

    private void fetchDouble() {
        fetchFlowScalar('"');
    }

    private void fetchFlowScalar(char style) {
        // A flow scalar could be a simple key.
        savePossibleSimpleKey();

        // No simple keys after flow scalars.
        this.allowSimpleKey = false;

        // Scan and add SCALAR.
        Token tok = scanFlowScalar(style);
        this.tokens.add(tok);
    }

    private void fetchPlain() {
        // A plain scalar could be a simple key.
        savePossibleSimpleKey();

        // No simple keys after plain scalars. But note that `scan_plain` will
        // change this flag if the scan is finished at the beginning of the
        // line.
        this.allowSimpleKey = false;

        // Scan and add SCALAR. May change `allow_simple_key`.
        Token tok = scanPlain();
        this.tokens.add(tok);
    }

    // Checkers.

    private boolean checkDirective() {
        // DIRECTIVE: ^ '%' ...
        // The '%' indicator is already checked.
        return reader.getColumn() == 0;
    }

    private boolean checkDocumentStart() {
        // DOCUMENT-START: ^ '---' (' '|'\n')
        if (reader.getColumn() == 0) {
            if ("---".equals(reader.prefix(3)) && Constant.NULL_BL_T_LINEBR.has(reader.peek(3))) {
                return true;
            }
        }
        return false;
    }

    private boolean checkDocumentEnd() {
        // DOCUMENT-END: ^ '...' (' '|'\n')
        if (reader.getColumn() == 0) {
            if ("...".equals(reader.prefix(3)) && Constant.NULL_BL_T_LINEBR.has(reader.peek(3))) {
                return true;
            }
        }
        return false;
    }

    private boolean checkBlockEntry() {
        // BLOCK-ENTRY: '-' (' '|'\n')
        return Constant.NULL_BL_T_LINEBR.has(reader.peek(1));
    }

    private boolean checkKey() {
        // KEY(flow context): '?'
        if (this.flowLevel != 0) {
            return true;
        } else {
            // KEY(block context): '?' (' '|'\n')
            return Constant.NULL_BL_T_LINEBR.has(reader.peek(1));
        }
    }

    private boolean checkValue() {
        // VALUE(flow context): ':'
        if (flowLevel != 0) {
            return true;
        } else {
            // VALUE(block context): ':' (' '|'\n')
            return Constant.NULL_BL_T_LINEBR.has(reader.peek(1));
        }
    }

    private boolean checkPlain() {
        /**
         * <pre>
         * A plain scalar may start with any non-space character except:
         *   '-', '?', ':', ',', '[', ']', '{', '}',
         *   '#', '&amp;', '*', '!', '|', '&gt;', '\'', '\&quot;',
         *   '%', '@', '`'.
         * 
         * It may also start with
         *   '-', '?', ':'
         * if it is followed by a non-space character.
         * 
         * Note that we limit the last rule to the block context (except the
         * '-' character) because we want the flow context to be space
         * independent.
         * </pre>
         */
        char ch = reader.peek();
        return Constant.NULL_BL_T_LINEBR.hasNo(ch, "-?:,[]{}#&*!|>\'\"%@`")
                || (Constant.NULL_BL_T_LINEBR.hasNo(reader.peek(1)) && (ch == '-' || (this.flowLevel == 0 && "?:"
                        .indexOf(ch) != -1)));
    }

    // Scanners.

    /**
     * <pre>
     * We ignore spaces, line breaks and comments.
     * If we find a line break in the block context, we set the flag
     * `allow_simple_key` on.
     * The byte order mark is stripped if it's the first character in the
     * stream. We do not yet support BOM inside the stream as the
     * specification requires. Any such mark will be considered as a part
     * of the document.
     * TODO: We need to make tab handling rules more sane. A good rule is
     *   Tabs cannot precede tokens
     *   BLOCK-SEQUENCE-START, BLOCK-MAPPING-START, BLOCK-END,
     *   KEY(block), VALUE(block), BLOCK-ENTRY
     * So the checking code is
     *   if &lt;TAB&gt;:
     *       self.allow_simple_keys = False
     * We also need to add the check for `allow_simple_keys == True` to
     * `unwind_indent` before issuing BLOCK-END.
     * Scanners for block, flow, and plain scalars need to be modified.
     * </pre>
     */
    private void scanToNextToken() {
        if (reader.getIndex() == 0 && reader.peek() == '\uFEFF') {
            reader.forward();
        }
        boolean found = false;
        while (!found) {
            int ff = 0;
            while (reader.peek(ff) == ' ') {
                ff++;
            }
            if (ff > 0) {
                reader.forward(ff);
            }

            if (reader.peek() == '#') {
                ff = 0;
                while (Constant.NULL_OR_LINEBR.hasNo(reader.peek(ff))) {
                    ff++;
                }
                if (ff > 0) {
                    reader.forward(ff);
                }
            }
            if (scanLineBreak().length() != 0) {
                if (this.flowLevel == 0) {
                    this.allowSimpleKey = true;
                }
            } else {
                found = true;
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Token scanDirective() {
        // See the specification for details.
        Mark startMark = reader.getMark();
        Mark endMark;
        reader.forward();
        String name = scanDirectiveName(startMark);
        List<?> value = null;
        if ("YAML".equals(name)) {
            value = scanYamlDirectiveValue(startMark);
            endMark = reader.getMark();
        } else if ("TAG".equals(name)) {
            value = scanTagDirectiveValue(startMark);
            endMark = reader.getMark();
        } else {
            endMark = reader.getMark();
            int ff = 0;
            while (Constant.NULL_OR_LINEBR.hasNo(reader.peek(ff))) {
                ff++;
            }
            if (ff > 0) {
                reader.forward(ff);
            }
        }
        scanDirectiveIgnoredLine(startMark);
        return new DirectiveToken(name, value, startMark, endMark);
    }

    private String scanDirectiveName(Mark startMark) {
        // See the specification for details.
        int length = 0;
        char ch = reader.peek(length);
        while (Constant.ALPHA.has(ch)) {
            length++;
            ch = reader.peek(length);
        }
        if (length == 0) {
            throw new ScannerException("while scanning a directive", startMark,
                    "expected alphabetic or numeric character, but found " + ch + "(" + ((int) ch)
                            + ")", reader.getMark());
        }
        String value = reader.prefixForward(length);
        ch = reader.peek();
        if (Constant.NULL_BL_LINEBR.hasNo(ch)) {
            throw new ScannerException("while scanning a directive", startMark,
                    "expected alphabetic or numeric character, but found " + ch + "(" + ((int) ch)
                            + ")", reader.getMark());
        }
        return value;
    }

    private List<Integer> scanYamlDirectiveValue(Mark startMark) {
        // See the specification for details.
        while (reader.peek() == ' ') {
            reader.forward();
        }
        Integer major = scanYamlDirectiveNumber(startMark);
        if (reader.peek() != '.') {
            throw new ScannerException("while scanning a directive", startMark,
                    "expected a digit or '.', but found " + reader.peek() + "("
                            + ((int) reader.peek()) + ")", reader.getMark());
        }
        reader.forward();
        Integer minor = scanYamlDirectiveNumber(startMark);
        if (Constant.NULL_BL_LINEBR.hasNo(reader.peek())) {
            throw new ScannerException("while scanning a directive", startMark,
                    "expected a digit or ' ', but found " + reader.peek() + "("
                            + ((int) reader.peek()) + ")", reader.getMark());
        }
        List<Integer> result = new ArrayList<Integer>(2);
        result.add(major);
        result.add(minor);
        return result;
    }

    private Integer scanYamlDirectiveNumber(Mark startMark) {
        // See the specification for details.
        char ch = reader.peek();
        if (!Character.isDigit(ch)) {
            throw new ScannerException("while scanning a directive", startMark,
                    "expected a digit, but found " + ch + "(" + ((int) ch) + ")", reader.getMark());
        }
        int length = 0;
        while (Character.isDigit(reader.peek(length))) {
            length++;
        }
        Integer value = new Integer(reader.prefixForward(length));
        return value;
    }

    private List<String> scanTagDirectiveValue(Mark startMark) {
        // See the specification for details.
        while (reader.peek() == ' ') {
            reader.forward();
        }
        String handle = scanTagDirectiveHandle(startMark);
        while (reader.peek() == ' ') {
            reader.forward();
        }
        String prefix = scanTagDirectivePrefix(startMark);
        List<String> result = new ArrayList<String>(2);
        result.add(handle);
        result.add(prefix);
        return result;
    }

    private String scanTagDirectiveHandle(Mark startMark) {
        // See the specification for details.
        String value = scanTagHandle("directive", startMark);
        char ch = reader.peek();
        if (ch != ' ') {
            throw new ScannerException("while scanning a directive", startMark,
                    "expected ' ', but found " + reader.peek() + "(" + ch + ")", reader.getMark());
        }
        return value;
    }

    private String scanTagDirectivePrefix(Mark startMark) {
        // See the specification for details.
        String value = scanTagUri("directive", startMark);
        if (Constant.NULL_BL_LINEBR.hasNo(reader.peek())) {
            throw new ScannerException("while scanning a directive", startMark,
                    "expected ' ', but found " + reader.peek() + "(" + ((int) reader.peek()) + ")",
                    reader.getMark());
        }
        return value;
    }

    private String scanDirectiveIgnoredLine(Mark startMark) {
        // See the specification for details.
        int ff = 0;
        while (reader.peek(ff) == ' ') {
            ff++;
        }
        if (ff > 0) {
            reader.forward(ff);
        }
        if (reader.peek() == '#') {
            ff = 0;
            while (Constant.NULL_OR_LINEBR.hasNo(reader.peek(ff))) {
                ff++;
            }
            reader.forward(ff);
        }
        char ch = reader.peek();
        String lineBreak = scanLineBreak();
        if (lineBreak.length() == 0 && ch != '\0') {
            throw new ScannerException("while scanning a directive", startMark,
                    "expected a comment or a line break, but found " + ch + "(" + ((int) ch) + ")",
                    reader.getMark());
        }
        return lineBreak;
    }

    /**
     * <pre>
     * The specification does not restrict characters for anchors and
     * aliases. This may lead to problems, for instance, the document:
     *   [ *alias, value ]
     * can be interpreted in two ways, as
     *   [ &quot;value&quot; ]
     * and
     *   [ *alias , &quot;value&quot; ]
     * Therefore we restrict aliases to numbers and ASCII letters.
     * </pre>
     */
    private Token scanAnchor(boolean isAnchor) {
        Mark startMark = reader.getMark();
        char indicator = reader.peek();
        String name = indicator == '*' ? "alias" : "anchor";
        reader.forward();
        int length = 0;
        char ch = reader.peek(length);
        while (Constant.ALPHA.has(ch)) {
            length++;
            ch = reader.peek(length);
        }
        if (length == 0) {
            throw new ScannerException("while scanning an " + name, startMark,
                    "expected alphabetic or numeric character, but found but found " + ch,
                    reader.getMark());
        }
        String value = reader.prefixForward(length);
        ch = reader.peek();
        if (Constant.NULL_BL_T_LINEBR.hasNo(ch, "?:,]}%@`")) {
            throw new ScannerException("while scanning an " + name, startMark,
                    "expected alphabetic or numeric character, but found " + ch + "("
                            + ((int) reader.peek()) + ")", reader.getMark());
        }
        Mark endMark = reader.getMark();
        Token tok;
        if (isAnchor) {
            tok = new AnchorToken(value, startMark, endMark);
        } else {
            tok = new AliasToken(value, startMark, endMark);
        }
        return tok;
    }

    private Token scanTag() {
        // See the specification for details.
        Mark startMark = reader.getMark();
        char ch = reader.peek(1);
        String handle = null;
        String suffix = null;
        if (ch == '<') {
            reader.forward(2);
            suffix = scanTagUri("tag", startMark);
            if (reader.peek() != '>') {
                throw new ScannerException("while scanning a tag", startMark,
                        "expected '>', but found '" + reader.peek() + "' (" + ((int) reader.peek())
                                + ")", reader.getMark());
            }
            reader.forward();
        } else if (Constant.NULL_BL_T_LINEBR.has(ch)) {
            suffix = "!";
            reader.forward();
        } else {
            int length = 1;
            boolean useHandle = false;
            while (Constant.NULL_BL_LINEBR.hasNo(ch)) {
                if (ch == '!') {
                    useHandle = true;
                    break;
                }
                length++;
                ch = reader.peek(length);
            }
            handle = "!";
            if (useHandle) {
                handle = scanTagHandle("tag", startMark);
            } else {
                handle = "!";
                reader.forward();
            }
            suffix = scanTagUri("tag", startMark);
        }
        ch = reader.peek();
        if (Constant.NULL_BL_LINEBR.hasNo(ch)) {
            throw new ScannerException("while scanning a tag", startMark,
                    "expected ' ', but found '" + ch + "' (" + ((int) ch) + ")", reader.getMark());
        }
        TagTuple value = new TagTuple(handle, suffix);
        Mark endMark = reader.getMark();
        return new TagToken(value, startMark, endMark);
    }

    private Token scanBlockScalar(char style) {
        // See the specification for details.
        boolean folded;
        if (style == '>') {
            folded = true;
        } else {
            folded = false;
        }
        StringBuilder chunks = new StringBuilder();
        Mark startMark = reader.getMark();
        // Scan the header.
        reader.forward();
        Chomping chompi = scanBlockScalarIndicators(startMark);
        int increment = chompi.getIncrement();
        scanBlockScalarIgnoredLine(startMark);

        // Determine the indentation level and go to the first non-empty line.
        int minIndent = this.indent + 1;
        if (minIndent < 1) {
            minIndent = 1;
        }
        String breaks = null;
        int maxIndent = 0;
        int indent = 0;
        Mark endMark;
        if (increment == -1) {
            Object[] brme = scanBlockScalarIndentation();
            breaks = (String) brme[0];
            maxIndent = ((Integer) brme[1]).intValue();
            endMark = (Mark) brme[2];
            indent = Math.max(minIndent, maxIndent);
        } else {
            indent = minIndent + increment - 1;
            Object[] brme = scanBlockScalarBreaks(indent);
            breaks = (String) brme[0];
            endMark = (Mark) brme[1];
        }

        String lineBreak = "";

        // Scan the inner part of the block scalar.
        while (this.reader.getColumn() == indent && reader.peek() != '\0') {
            chunks.append(breaks);
            boolean leadingNonSpace = " \t".indexOf(reader.peek()) == -1;
            int length = 0;
            while (Constant.NULL_OR_LINEBR.hasNo(reader.peek(length))) {
                length++;
            }
            chunks.append(reader.prefixForward(length));
            lineBreak = scanLineBreak();
            Object[] brme = scanBlockScalarBreaks(indent);
            breaks = (String) brme[0];
            endMark = (Mark) brme[1];
            if (this.reader.getColumn() == indent && reader.peek() != '\0') {

                // Unfortunately, folding rules are ambiguous.
                //
                // This is the folding according to the specification:
                if (folded && "\n".equals(lineBreak) && leadingNonSpace
                        && " \t".indexOf(reader.peek()) == -1) {
                    if (breaks.length() == 0) {
                        chunks.append(" ");
                    }
                } else {
                    chunks.append(lineBreak);
                }
                // Clark Evans's interpretation (also in the spec examples) not
                // imported from PyYAML
            } else {
                break;
            }
        }
        // Chomp the tail.
        if (chompi.chompTailIsNotFalse()) {
            chunks.append(lineBreak);
        }
        if (chompi.chompTailIsTrue()) {
            chunks.append(breaks);
        }
        // We are done.
        return new ScalarToken(chunks.toString(), false, startMark, endMark, style);
    }

    private Chomping scanBlockScalarIndicators(Mark startMark) {
        // See the specification for details.
        Boolean chomping = null;
        int increment = -1;
        char ch = reader.peek();
        if (ch == '-' || ch == '+') {
            if (ch == '+') {
                chomping = Boolean.TRUE;
            } else {
                chomping = Boolean.FALSE;
            }
            reader.forward();
            ch = reader.peek();
            if (Character.isDigit(ch)) {
                increment = Integer.parseInt(String.valueOf(ch));
                if (increment == 0) {
                    throw new ScannerException("while scanning a block scalar", startMark,
                            "expected indentation indicator in the range 1-9, but found 0",
                            reader.getMark());
                }
                reader.forward();
            }
        } else if (Character.isDigit(ch)) {
            increment = Integer.parseInt(String.valueOf(ch));
            if (increment == 0) {
                throw new ScannerException("while scanning a block scalar", startMark,
                        "expected indentation indicator in the range 1-9, but found 0",
                        reader.getMark());
            }
            reader.forward();
            ch = reader.peek();
            if (ch == '-' || ch == '+') {
                if (ch == '+') {
                    chomping = Boolean.TRUE;
                } else {
                    chomping = Boolean.FALSE;
                }
                reader.forward();
            }
        }
        ch = reader.peek();
        if (Constant.NULL_BL_LINEBR.hasNo(ch)) {
            throw new ScannerException("while scanning a block scalar", startMark,
                    "expected chomping or indentation indicators, but found " + ch,
                    reader.getMark());
        }
        return new Chomping(chomping, increment);
    }

    private String scanBlockScalarIgnoredLine(Mark startMark) {
        // See the specification for details.
        int ff = 0;
        while (reader.peek(ff) == ' ') {
            ff++;
        }
        if (ff > 0) {
            reader.forward(ff);
        }

        if (reader.peek() == '#') {
            ff = 0;
            while (Constant.NULL_OR_LINEBR.hasNo(reader.peek(ff))) {
                ff++;
            }
            if (ff > 0) {
                reader.forward(ff);
            }
        }
        char ch = reader.peek();
        String lineBreak = scanLineBreak();
        if (lineBreak.length() == 0 && ch != '\0') {
            throw new ScannerException("while scanning a block scalar", startMark,
                    "expected a comment or a line break, but found " + ch, reader.getMark());
        }
        return lineBreak;
    }

    private Object[] scanBlockScalarIndentation() {
        // See the specification for details.
        StringBuilder chunks = new StringBuilder();
        int maxIndent = 0;
        Mark endMark = reader.getMark();
        while (Constant.LINEBR.has(reader.peek(), " \r")) {
            if (reader.peek() != ' ') {
                chunks.append(scanLineBreak());
                endMark = reader.getMark();
            } else {
                reader.forward();
                if (this.reader.getColumn() > maxIndent) {
                    maxIndent = reader.getColumn();
                }
            }
        }
        return new Object[] { chunks.toString(), maxIndent, endMark };
    }

    private Object[] scanBlockScalarBreaks(int indent) {
        // See the specification for details.
        StringBuilder chunks = new StringBuilder();
        Mark endMark = reader.getMark();
        int ff = 0;
        int col = this.reader.getColumn();
        while (col < indent && reader.peek(ff) == ' ') {
            ff++;
            col++;
        }
        if (ff > 0) {
            reader.forward(ff);
        }

        String lineBreak = null;
        while ((lineBreak = scanLineBreak()).length() != 0) {
            chunks.append(lineBreak);
            endMark = reader.getMark();
            ff = 0;
            col = this.reader.getColumn();
            while (col < indent && reader.peek(ff) == ' ') {
                ff++;
                col++;
            }
            if (ff > 0) {
                reader.forward(ff);
            }
        }
        return new Object[] { chunks.toString(), endMark };
    }

    /**
     * <pre>
     * See the specification for details.
     * Note that we loose indentation rules for quoted scalars. Quoted
     * scalars don't need to adhere indentation because &quot; and ' clearly
     * mark the beginning and the end of them. Therefore we are less
     * restrictive then the specification requires. We only need to check
     * that document separators are not included in scalars.
     * </pre>
     */
    private Token scanFlowScalar(char style) {
        boolean _double;
        if (style == '"') {
            _double = true;
        } else {
            _double = false;
        }
        StringBuilder chunks = new StringBuilder();
        Mark startMark = reader.getMark();
        char quote = reader.peek();
        reader.forward();
        chunks.append(scanFlowScalarNonSpaces(_double, startMark));
        while (reader.peek() != quote) {
            chunks.append(scanFlowScalarSpaces(startMark));
            chunks.append(scanFlowScalarNonSpaces(_double, startMark));
        }
        reader.forward();
        Mark endMark = reader.getMark();
        return new ScalarToken(chunks.toString(), false, startMark, endMark, style);
    }

    private String scanFlowScalarNonSpaces(boolean _double, Mark startMark) {
        // See the specification for details.
        StringBuilder chunks = new StringBuilder();
        while (true) {
            int length = 0;
            while (Constant.NULL_BL_T_LINEBR.hasNo(reader.peek(length), "\'\"\\")) {
                length++;
            }
            if (length != 0) {
                chunks.append(reader.prefixForward(length));
            }
            char ch = reader.peek();
            if (!_double && ch == '\'' && reader.peek(1) == '\'') {
                chunks.append("'");
                reader.forward(2);
            } else if ((_double && ch == '\'') || (!_double && "\"\\".indexOf(ch) != -1)) {
                chunks.append(ch);
                reader.forward();
            } else if (_double && ch == '\\') {
                reader.forward();
                ch = reader.peek();
                if (ESCAPE_REPLACEMENTS.containsKey(new Character(ch))) {
                    chunks.append(ESCAPE_REPLACEMENTS.get(new Character(ch)));
                    reader.forward();
                } else if (ESCAPE_CODES.containsKey(new Character(ch))) {
                    length = (ESCAPE_CODES.get(new Character(ch))).intValue();
                    reader.forward();
                    String hex = reader.prefix(length);
                    if (NOT_HEXA.matcher(hex).find()) {
                        throw new ScannerException("while scanning a double-quoted scalar",
                                startMark, "expected escape sequence of " + length
                                        + " hexadecimal numbers, but found: " + hex,
                                reader.getMark());
                    }
                    char unicode = (char) Integer.parseInt(hex, 16);
                    chunks.append(unicode);
                    reader.forward(length);
                } else if (scanLineBreak().length() != 0) {
                    chunks.append(scanFlowScalarBreaks(startMark));
                } else {
                    throw new ScannerException("while scanning a double-quoted scalar", startMark,
                            "found unknown escape character " + ch + "(" + ((int) ch) + ")",
                            reader.getMark());
                }
            } else {
                return chunks.toString();
            }
        }
    }

    private String scanFlowScalarSpaces(Mark startMark) {
        // See the specification for details.
        StringBuilder chunks = new StringBuilder();
        int length = 0;
        while (" \t".indexOf(reader.peek(length)) != -1) {
            length++;
        }
        String whitespaces = reader.prefixForward(length);
        char ch = reader.peek();
        if (ch == '\0') {
            throw new ScannerException("while scanning a quoted scalar", startMark,
                    "found unexpected end of stream", reader.getMark());
        }
        String lineBreak = scanLineBreak();
        if (lineBreak.length() != 0) {
            String breaks = scanFlowScalarBreaks(startMark);
            if (!"\n".equals(lineBreak)) {
                chunks.append(lineBreak);
            } else if (breaks.length() == 0) {
                chunks.append(" ");
            }
            chunks.append(breaks);
        } else {
            chunks.append(whitespaces);
        }
        return chunks.toString();
    }

    private String scanFlowScalarBreaks(Mark startMark) {
        // See the specification for details.
        StringBuilder chunks = new StringBuilder();
        while (true) {
            // Instead of checking indentation, we check for document
            // separators.
            String prefix = reader.prefix(3);
            if (("---".equals(prefix) || "...".equals(prefix))
                    && Constant.NULL_BL_T_LINEBR.has(reader.peek(3))) {
                throw new ScannerException("while scanning a quoted scalar", startMark,
                        "found unexpected document separator", reader.getMark());
            }
            while (" \t".indexOf(reader.peek()) != -1) {
                reader.forward();
            }
            String lineBreak = scanLineBreak();
            if (lineBreak.length() != 0) {
                chunks.append(lineBreak);
            } else {
                return chunks.toString();
            }
        }
    }

    /**
     * <pre>
     * See the specification for details.
     * We add an additional restriction for the flow context:
     *   plain scalars in the flow context cannot contain ',', ':' and '?'.
     * We also keep track of the `allow_simple_key` flag here.
     * Indentation rules are loosed for the flow context.
     * </pre>
     */
    private Token scanPlain() {
        StringBuilder chunks = new StringBuilder();
        Mark startMark = reader.getMark();
        Mark endMark = startMark;
        int indent = this.indent + 1;
        String spaces = "";
        while (true) {
            char ch;
            int length = 0;
            if (reader.peek() == '#') {
                break;
            }
            while (true) {
                ch = reader.peek(length);
                if (Constant.NULL_BL_T_LINEBR.has(ch)
                        || (this.flowLevel == 0 && ch == ':' && Constant.NULL_BL_T_LINEBR
                                .has(reader.peek(length + 1)))
                        || (this.flowLevel != 0 && ",:?[]{}".indexOf(ch) != -1)) {
                    break;
                }
                length++;
            }
            // It's not clear what we should do with ':' in the flow context.
            if (this.flowLevel != 0 && ch == ':'
                    && Constant.NULL_BL_T_LINEBR.hasNo(reader.peek(length + 1), ",[]{}")) {
                reader.forward(length);
                throw new ScannerException("while scanning a plain scalar", startMark,
                        "found unexpected ':'", reader.getMark(),
                        "Please check http://pyyaml.org/wiki/YAMLColonInFlowContext for details.");
            }
            if (length == 0) {
                break;
            }
            this.allowSimpleKey = false;
            chunks.append(spaces);
            chunks.append(reader.prefixForward(length));
            endMark = reader.getMark();
            spaces = scanPlainSpaces();
            // System.out.printf("spaces[%s]\n", spaces);
            if (spaces.length() == 0 || reader.peek() == '#'
                    || (this.flowLevel == 0 && this.reader.getColumn() < indent)) {
                break;
            }
        }
        return new ScalarToken(chunks.toString(), startMark, endMark, true);
    }

    /**
     * <pre>
     * See the specification for details.
     * The specification is really confusing about tabs in plain scalars.
     * We just forbid them completely. Do not use tabs in YAML!
     * </pre>
     */
    private String scanPlainSpaces() {
        int length = 0;
        while (reader.peek(length) == ' ') {
            length++;
        }
        String whitespaces = reader.prefixForward(length);
        String lineBreak = scanLineBreak();
        if (lineBreak.length() != 0) {
            this.allowSimpleKey = true;
            String prefix = reader.prefix(3);
            if ("---".equals(prefix) || "...".equals(prefix)
                    && Constant.NULL_BL_T_LINEBR.has(reader.peek(3))) {
                return "";
            }
            StringBuilder breaks = new StringBuilder();
            while (true) {
                if (reader.peek() == ' ') {
                    reader.forward();
                } else {
                    String lb = scanLineBreak();
                    if (lb.length() != 0) {
                        breaks.append(lb);
                        prefix = reader.prefix(3);
                        if ("---".equals(prefix) || "...".equals(prefix)
                                && Constant.NULL_BL_T_LINEBR.has(reader.peek(3))) {
                            return "";
                        }
                    } else {
                        break;
                    }
                }
            }
            if (!"\n".equals(lineBreak)) {
                return lineBreak + breaks;
            } else if (breaks.length() == 0) {
                return " ";
            }
            return breaks.toString();
        }
        return whitespaces;
    }

    /**
     * <pre>
     * See the specification for details.
     * For some strange reasons, the specification does not allow '_' in
     * tag handles. I have allowed it anyway.
     * </pre>
     */
    private String scanTagHandle(String name, Mark startMark) {
        char ch = reader.peek();
        if (ch != '!') {
            throw new ScannerException("while scanning a " + name, startMark,
                    "expected '!', but found " + ch + "(" + ((int) ch) + ")", reader.getMark());
        }
        int length = 1;
        ch = reader.peek(length);
        if (ch != ' ') {
            while (Constant.ALPHA.has(ch)) {
                length++;
                ch = reader.peek(length);
            }
            if (ch != '!') {
                reader.forward(length);
                throw new ScannerException("while scanning a " + name, startMark,
                        "expected '!', but found " + ch + "(" + ((int) ch) + ")", reader.getMark());
            }
            length++;
        }
        String value = reader.prefixForward(length);
        return value;
    }

    private String scanTagUri(String name, Mark startMark) {
        // See the specification for details.
        // Note: we do not check if URI is well-formed.
        StringBuilder chunks = new StringBuilder();
        int length = 0;
        char ch = reader.peek(length);
        while (Constant.URI_CHARS.has(ch)) {
            if (ch == '%') {
                chunks.append(reader.prefixForward(length));
                length = 0;
                chunks.append(scanUriEscapes(name, startMark));
            } else {
                length++;
            }
            ch = reader.peek(length);
        }
        if (length != 0) {
            chunks.append(reader.prefixForward(length));
            length = 0;
        }
        if (chunks.length() == 0) {
            throw new ScannerException("while scanning a " + name, startMark,
                    "expected URI, but found " + ch + "(" + ((int) ch) + ")", reader.getMark());
        }
        return chunks.toString();
    }

    private String scanUriEscapes(String name, Mark startMark) {
        // First, look ahead to see how many URI-escaped characters we should
        // expect, so we can use the correct buffer size.
        int length = 1;
        while (reader.peek(length * 3) == '%') {
            length++;
        }
        // See the specification for details.
        // URIs containing 16 and 32 bit Unicode characters are
        // encoded in UTF-8, and then each octet is written as a
        // separate character.
        Mark beginningMark = reader.getMark();
        ByteBuffer buff = ByteBuffer.allocate(length);
        while (reader.peek() == '%') {
            reader.forward();
            try {
                byte code = (byte) Integer.parseInt(reader.prefix(2), 16);
                buff.put(code);
            } catch (NumberFormatException nfe) {
                throw new ScannerException("while scanning a " + name, startMark,
                        "expected URI escape sequence of 2 hexadecimal numbers, but found "
                                + reader.peek() + "(" + ((int) reader.peek()) + ") and "
                                + reader.peek(1) + "(" + ((int) reader.peek(1)) + ")",
                        reader.getMark());
            }
            reader.forward(2);
        }
        buff.flip();
        try {
            return UriEncoder.decode(buff);
        } catch (CharacterCodingException e) {
            throw new ScannerException("while scanning a " + name, startMark,
                    "expected URI in UTF-8: " + e.getMessage(), beginningMark);
        }
    }

    private String scanLineBreak() {
        // Transforms:
        // '\r\n' : '\n'
        // '\r' : '\n'
        // '\n' : '\n'
        // '\x85' : '\n'
        // default : ''
        char ch = reader.peek();
        if (ch == '\r' || ch == '\n' || ch == '\u0085') {
            if (ch == '\r' && '\n' == reader.peek(1)) {
                reader.forward(2);
            } else {
                reader.forward();
            }
            return "\n";
        } else if (ch == '\u2028' || ch == '\u2029') {
            reader.forward();
            return String.valueOf(ch);
        }
        return "";
    }

    /**
     * Chomping the tail may have 3 values - yes, no, not defined.
     */
    private class Chomping {
        private final Boolean value;
        private final int increment;

        public Chomping(Boolean value, int increment) {
            this.value = value;
            this.increment = increment;
        }

        public boolean chompTailIsNotFalse() {
            return value == null || value;
        }

        public boolean chompTailIsTrue() {
            return value != null && value;
        }

        public int getIncrement() {
            return increment;
        }
    }
}
