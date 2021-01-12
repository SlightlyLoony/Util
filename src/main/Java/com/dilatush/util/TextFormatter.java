package com.dilatush.util;

import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a very simple formatter for fixed-lineWidth text.  The motivation for creating this was the need to format messages that would appear on the
 * command line.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class TextFormatter {

    private final int width;           // the total lineWidth of the output text
    private final int leftIndent;      // the left indent
    private final int afterPeriod;     // the number of spaces after a period

    private final List<Line> buffer;


    public TextFormatter( final int _width, final int _leftIndent, final int _firstLineDelta, final int _afterPeriod ) {

        width          = _width;
        leftIndent     = _leftIndent;
        afterPeriod    = _afterPeriod;
        buffer         = new ArrayList<>();

        buffer.add( new Line( leftIndent + _firstLineDelta, width - (leftIndent + _firstLineDelta) ) );
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

    public static void main( String[] _args ) {

        String text = "//TAB4//A hamburger (also burger for short) is a sandwich consisting of one or more cooked patties of ground meat, usually beef, " +
                "placed inside a sliced bread roll or bun. The patty may be pan fried, grilled, smoked or flame broiled. Hamburgers are often " +
                "served with cheese, lettuce, tomato, onion, pickles, bacon, or chiles; condiments such as ketchup, mustard, mayonnaise, relish, " +
                "or a \"special sauce\", often a variation of Thousand Island dressing; and are frequently placed on sesame seed buns. A hamburger " +
                "topped with cheese is called a cheeseburger.[1]\n" +
                "//BR////BR//The term \"burger\" can also be applied to the meat patty on its own, especially in the United Kingdom, where the term \"patty\" " +
                "is rarely used, or the term can even refer simply to ground beef. Since the term hamburger usually implies beef, for clarity " +
                "\"burger\" may be prefixed with the type of meat or meat substitute used, as in beef burger, turkey burger, bison burger, or veggie " +
                "burger.\n" +
                "//TAB10//Hamburgers are sold at fast-food restaurants, diners, and specialty and high-end restaurants. There are many international and " +
                "regional variations of the hamburger. ";

        TextFormatter tf = new TextFormatter( 100, 4, 0, 2 );
        tf.add( text );
        String output = tf.getFormattedText();
        System.out.println( output );

        tf.hashCode();
    }
}
