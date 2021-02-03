package com.dilatush.util;

import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a very simple formatter for fixed-lineWidth text.  The motivation for creating this was the need to format messages that would appear on
 * the command line.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TextFormatter {

    private final int width;           // the total lineWidth of the output text
    private final int leftIndent;      // the left indent
    private final int afterPeriod;     // the number of spaces after a period
    private final int firstLineDelta;  // the delta for the first line's indent

    private final List<Line> buffer;


    public TextFormatter( final int _width, final int _leftIndent, final int _firstLineDelta, final int _afterPeriod ) {

        width          = _width;
        leftIndent     = _leftIndent;
        afterPeriod    = _afterPeriod;
        firstLineDelta = _firstLineDelta;
        buffer         = new ArrayList<>();

        buffer.add( new Line( leftIndent + firstLineDelta, width - (leftIndent + firstLineDelta) ) );
    }


    /**
     * Clears all data out of this text formatter, exactly as if it was re-instantiated.
     */
    public void clear() {
        buffer.clear();
        buffer.add( new Line( leftIndent + firstLineDelta, width - (leftIndent + firstLineDelta) ) );
    }


    /**
     * Adds the given text to this formatter as a paragraph.  Within the text, words are any strings bounded by whitespace.  Newline characters
     * within the text are treated as paragraph separators, exactly as if each such defined paragraph were separately added through this method.
     *
     * @param _text The text to add to this formatter.
     */
    public void add( final String _text ) {

        // get the current index, initializing it if necessary...
        int index = buffer.size() - 1;

        // get the paragraphs in our input...
        String[] paragraphs = _text.split( "\n" );

        // get the line we're adding to, indented as required...
        Line line = buffer.get( index );

        // iterate over our paragraphs...
        for( String paragraph : paragraphs ) {

            // if the preceding line had any content, end it and get a new one...
            if( line.contents.length() > 0 ) {
                line = new Line( leftIndent, width - leftIndent );
                buffer.add( line );
                index++;
            }

            // look for breaks at the start of a paragraph...
            while( paragraph.startsWith( "//BR//" ) ) {

                // add a blank line...
                line = new Line( leftIndent, width - leftIndent );
                buffer.add( line );

                // lose the break we just processed...
                paragraph = paragraph.substring( 6 );
            }

            // look for a tab near the start of a paragraph...
            int tabStart = paragraph.substring( 0, Math.min( width, paragraph.length() ) ).indexOf( "//TAB" );
            if( tabStart >= 0) {

                // see if we also have the rest of the tab directive...
                ParsePosition pp = new ParsePosition( tabStart + 5 );
                DecimalFormat df = new DecimalFormat( "##0" );
                Number n = df.parse( paragraph, pp );
                if( n != null ) {
                    if( paragraph.startsWith( "//", pp.getIndex() ) ) {

                        int tabCol = n.intValue();
                        String hardSpaces = Strings.getStringOfChar( (char) 0xA0, tabCol - tabStart );
                        paragraph = paragraph.substring( 0, tabStart ) + hardSpaces + paragraph.substring( pp.getIndex() + 2 );
                    }
                }
            }

            // split our paragraph into words...
            String[] words = paragraph.split( " +" );

            // iterate over all our words...
            for( String word : words ) {

                // if there's room for our word at the end of the line, add it...
                if( line.hasRoom( word ) )
                    line.add( word );

                // otherwise, start a new line and add the word...
                else {
                    line = new Line( leftIndent, width - leftIndent );
                    line.add( word );
                    buffer.add( line );
                    index++;
                }
            }
        }
    }


    private class Line {

        private final int           lineLeftIndent;
        private final int           lineWidth;
        private final StringBuilder contents;


        private Line( final int _leftIndent, final int _width ) {
            lineLeftIndent = _leftIndent;
            lineWidth = _width;
            contents = new StringBuilder( lineWidth );
        }


        private void add( final String _word ) {
            contents.append( Strings.getStringOfChar( ' ', leadingSpaces() ) );
            contents.append( _word );
        }


        private boolean hasRoom( final String _word ) {
            return leadingSpaces() + _word.length() + contents.length() < lineWidth;
        }


        private int leadingSpaces() {
            char lc = getLastChar();
            return (lc != ' ') ? ((lc == '.') ? afterPeriod : 1) : 0;
        }


        /**
         * If there's no content, returns a space; otherwise, returns the last character on the line.
         *
         * @return the last character on the line
         */
        private char getLastChar() {
            return (contents.length() != 0) ? contents.charAt( contents.length() - 1 ) : ' ';
        }
    }


    public String getFormattedText() {
        StringBuilder result = new StringBuilder( buffer.size() * width );
        for( Line line : buffer ) {
            result.append( Strings.getStringOfChar( ' ', line.lineLeftIndent ) );
            result.append( line.contents );
            result.append( System.lineSeparator() );
        }
        return result.toString();
    }
}
