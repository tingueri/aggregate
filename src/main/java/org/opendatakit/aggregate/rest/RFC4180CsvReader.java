/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.aggregate.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Unfortunately, while the CSVWriter of opencsv works, the reader is broken
 * w.r.t. quoted strings and RFC4180.  To further complicate things, on a
 * Mac, Excel fails to emit line feeds, but only emits carriage returns, which
 * is non-conformant behavior. Here is a working reader...
 * The key difference is that when a quotechar appears within a quoted
 * string, it is replaced with two quotechars.  So if we see two quotechars
 * in a row, then they are replaced with a single quotechar inside the
 * current string. quotechar, cr, lf are not permitted unless they are within
 * a quoted field if either cr, cr lf, or lf appear in a quoted field, a
 * cr lf combination is produced in the field string (i.e., we ensure that a
 * cr is always followed by a lf in any field values).
 *
 * @author mitchellsundt@gmail.com
 */
public class RFC4180CsvReader {

  private final BufferedReader br;

  private static final char CR_CHAR_INDEX = 13;
  private static final char LF_CHAR_INDEX = 10;
  private static final char SEPARATOR_CHAR = ',';
  private static final char QUOTE_CHAR = '"';


  /**
   * Constructs CSVReader with supplied separator and quote char.
   *
   * @param reader the reader to an underlying CSV source.
   */
  public RFC4180CsvReader(Reader reader) {
    this.br = new BufferedReader(reader);
    if (!br.markSupported()) {
      throw new IllegalStateException("Impossible de supporter la marque!");
    }
  }

  /**
   * Reads the next line from the buffer and converts to a string array.
   * Lines are expected to end with either CR-LF or just LF (newline).
   * Throws an IllegalStateException if this is not the case.
   *
   * @return <p>a String array with each comma-separated element as a separate
   *     entry in that array.</p>
   *     <p>Zero-length elements are returned as null values in the
   *     String[] array.</p>
   *     <p>Double-quotes should wrap elements containing new lines
   *     and commas. Embedded double-quotes are represented as two
   *     consecutive double-quote characters.</p>
   *     <p>Whether quoted or not, spaces are significant.</p>
   *     <p>Returns null instead of a String[] array if the stream is at EOF.</p>
   * @throws IllegalStateException if the file is badly formed.
   * @throws IOException           if bad things happen during the read.
   */
  public String[] readNext() throws IOException {

    List<String> results = new ArrayList<String>();
    StringBuilder b = new StringBuilder();

    boolean fetchNextChar = true;
    ParseState state = ParseState.atStartOfLine;
    int ch = -1;
    while (true) {
      if (fetchNextChar) {
        ch = br.read();
      }
      fetchNextChar = true;

      // Handle the start of the line differently
      // - if we hit end-of-file, return null.
      // - if we hit CR LF, return []
      //
      if (state == ParseState.atStartOfLine) {
        if (ch == -1) {
          // no more lines in file
          return null;
        }
        if (ch == CR_CHAR_INDEX) {
          // special case -- if we have an immediate CR LF, return an empty array
          br.mark(1);
          ch = br.read();
          if (ch != LF_CHAR_INDEX && ch != -1) {
            // Excel for Mac silliness
            br.reset();
          }
          return results.toArray(new String[results.size()]);
        } else if (ch == LF_CHAR_INDEX) {
          // alternate line terminator (no cr, just lf)
          return results.toArray(new String[results.size()]);
        }
        state = ParseState.atStartOfField;
      }

      // NOTE: we must be in a ParseState other than atStartOfLine
      if (state == ParseState.atStartOfField) {
        // If we are expecting the start of a field and
        // encounter a CR LF, then we emit a null value
        // and return the results array.
        if (ch == CR_CHAR_INDEX) {
          br.mark(1);
          ch = br.read();
          if (ch != LF_CHAR_INDEX && ch != -1) {
            // Excel for Mac silliness
            br.reset();
          }
          results.add(null);
          return results.toArray(new String[results.size()]);
        } else if (ch == LF_CHAR_INDEX || ch == -1) {
          // alternate line terminator
          results.add(null);
          return results.toArray(new String[results.size()]);
        } else if (ch == SEPARATOR_CHAR) {
          // no value in cell
          results.add(null);
          // the separator advances us to the next field
          state = ParseState.atStartOfField;
        } else if (ch == QUOTE_CHAR) {
          // start of a quoted string
          state = ParseState.quoted;
          b.setLength(0);
        } else {
          // start of an unquoted string
          state = ParseState.naked;
          b.setLength(0);
          b.append((char) ch);
        }
      } else if (state == ParseState.expectingComma) {
        if (ch == -1) {
          // allow the last line to not have a terminator
          // this is legal w.r.t. RFC4180, and is what Excel does on a Mac.
          return results.toArray(new String[results.size()]);
        } else if (ch == CR_CHAR_INDEX) {
          // We are expecting a comma but hit a CR LF
          // return the current results array.
          br.mark(1);
          ch = br.read();
          if (ch != LF_CHAR_INDEX && ch != -1) {
            // Excel for Mac silliness
            br.reset();
          }
          return results.toArray(new String[results.size()]);
        } else if (ch == LF_CHAR_INDEX) {
          // alternate line terminator
          return results.toArray(new String[results.size()]);
        } else if (ch == SEPARATOR_CHAR) {
          // found the comma -- transition to look for the start of the next field
          state = ParseState.atStartOfField;
        } else {
          throw new IllegalStateException("Attendu une virgule ou CR LF, mais trouvé: " + String.valueOf(ch));
        }
      } else if (state == ParseState.naked) {
        if (ch == -1) {
          // allow the last line to not have a terminator
          // this is legal w.r.t. RFC4180, and is what Excel does on a Mac.
          results.add(b.toString());
          b.setLength(0);
          return results.toArray(new String[results.size()]);
        } else if (ch == CR_CHAR_INDEX) {
          // marks the end of this naked field (and the end of the line)
          br.mark(1);
          ch = br.read();
          if (ch != LF_CHAR_INDEX && ch != -1) {
            // Excel for Mac silliness
            br.reset();
          }
          results.add(b.toString());
          b.setLength(0);
          return results.toArray(new String[results.size()]);
        } else if (ch == LF_CHAR_INDEX) {
          results.add(b.toString());
          b.setLength(0);
          return results.toArray(new String[results.size()]);
        } else if (ch == SEPARATOR_CHAR) {
          // marks the end of this naked field
          String field = b.toString();
          if (field.length() == 0) {
            // empty strings are nulls
            field = null;
          }
          results.add(field);
          b.setLength(0);
          // look for the start of the next field
          state = ParseState.atStartOfField;
        } else if (ch == QUOTE_CHAR) {
          throw new IllegalStateException("Double guillemet inattendu dans une valeur de champ non entre guillemets");
        } else {
          // anything else is just added to the field
          b.append((char) ch);
        }
      } else if (state == ParseState.quoted) {
        if (ch == -1) {
          throw new IllegalStateException("Fin de fichier inattendue dans la valeur de champ citée");
        } else if (ch == QUOTE_CHAR) {
          // read the next character to see of it is an escaped quote
          ch = br.read();
          if (ch != QUOTE_CHAR) {
            // nope -- we are done with this quoted field
            // and expect a comma (or CR LF).
            String field = b.toString();
            if (field.length() == 0) {
              // empty strings are nulls
              field = null;
            }
            results.add(field);
            b.setLength(0);
            state = ParseState.expectingComma;
            // process the ch we read in but didn't use
            fetchNextChar = false;
          } else {
            // it was a escaped quote.
            // append a quote char to the string
            b.append((char) ch);
          }
        } else {
          // anything else is just added to the field
          //
          // If we have a cr, cr/lf, lf or nl, always emit
          // a cr/lf combination.
          if (ch == CR_CHAR_INDEX) {
            // marks the end of this naked field (and the end of the line)
            br.mark(1);
            ch = br.read();
            if (ch != LF_CHAR_INDEX && ch != -1) {
              // Excel for Mac silliness
              br.reset();
            }
            b.append((char) CR_CHAR_INDEX);
            b.append((char) LF_CHAR_INDEX);
          } else if (ch == LF_CHAR_INDEX) {
            b.append((char) CR_CHAR_INDEX);
            b.append((char) LF_CHAR_INDEX);
          } else {
            b.append((char) ch);
          }
        }
      }
    }
  }

  /**
   * Closes the underlying reader.
   *
   * @throws IOException if the close fails
   */
  public void close() throws IOException {
    br.close();
  }

  enum ParseState {
    atStartOfLine,
    atStartOfField,
    naked,
    quoted,
    expectingComma
  }


}
