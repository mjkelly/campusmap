/*
 * xmlTest.java, Aug 26, 2005
 * mkelly
 * Part of UCSDMap.
 *
 *
 *
 * Aug 26, 2005
 */

import java.io.*;
import java.util.*;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

public class xmlHandler {

    /**
     * Load a series of locations from an XML file.
     */
    public static Vector<Location> loadLocations() {
        SAXParserFactory sax = SAXParserFactory.newInstance();
        Vector<Location> locs = new Vector<Location>();

        try {
            SAXParser parser = sax.newSAXParser();
            parser.parse("locations.xml", new LocationHandler(locs));
        } catch (ParserConfigurationException e) {
            System.err.println("ParserConfigurationException: "
                    + e.getMessage());
        } catch (SAXException e) {
            System.err.println("SAXException: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }

        System.err.println("Locations parsed:");
        for (Location l : locs) {
            System.err.println(l);
        }

        return locs;
    }

}

/**
 * This class handles all the SAX callbacks for a location XML file.
 */
class LocationHandler extends DefaultHandler {
    // for storing inner text of elements
    StringBuffer innerText;

    // the list of locations to which we add
    Vector<Location> locations;

    // the following variables store the various attributes of a Location
    // before it is created

    String name;

    int x;

    int y;

    int id;

    String keywords;

    String code;

    Vector<String> aliases;

    boolean intersect, passThrough, displayName;

    /**
     * Constructor.
     * 
     * @param locs
     *            a vector of locations to add to
     */
    public LocationHandler(Vector<Location> locs) {
        locations = locs;
    }

    /**
     * Called when the document starts.
     */
    public void startDocument() {
        System.err.println("Start document");
    }

    /**
     * Called when the document ends.
     */
    public void endDocument() {
        System.err.println("End document");
    }

    /**
     * Called when an element begins. I.e., &lt;p&gt;. In a simple XML document,
     * qName would be "p" in that case. (That's what we use here.)
     */
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) {
        System.err.println("\t<" + qName + ">");
        // System.err.println("\tattributes = " + attributes);
        writeAttrs(attributes);

        // reset the buffer storing inner characters of this element
        innerText = new StringBuffer(64);

        // this is the list of locations
        // we just check for one attribute
        if (qName.equals("locations")) {
            if (attributes.getValue("maxid") != null)
                Location.IDcount = parseInt(attributes.getValue("maxid"));
        }

        // this is an individual location
        // we initialize variables representing the location's members to
        // default values, and get a few basic attributes (id, location, boolean
        // flags).
        else if (qName.equals("location")) {
            // when a new Location comes around, reset all temporary variables
            // to default values
            x = y = 0;
            name = null;
            code = null;
            keywords = null;
            aliases = null;
            intersect = passThrough = false;
            displayName = true;
            id = 0;

            // get the integer attributes of the location
            if (attributes.getValue("id") != null)
                id = parseInt(attributes.getValue("id"));
            if (attributes.getValue("x") != null)
                x = parseInt(attributes.getValue("x"));
            if (attributes.getValue("y") != null)
                y = parseInt(attributes.getValue("y"));

            // get the boolean attributes of the location
            if (attributes.getValue("passthrough") != null)
                passThrough = parseBoolean(attributes.getValue("passthrough"));
            if (attributes.getValue("intersect") != null)
                intersect = parseBoolean(attributes.getValue("intersect"));
            if (attributes.getValue("displayname") != null)
                displayName = parseBoolean(attributes.getValue("displayname"));

        }

        // we've got a list of aliases coming up... general Veers, prepare the
        // alias vector!
        if (qName.equals("aliases")) {
            aliases = new Vector<String>();
        }

    }

    /**
     * This is called when an element ends. I.e., &lt;/p&gt;. We have the same
     * args as startElement, except no attribute list (obviously). Most of the
     * object creation goes here.
     */
    public void endElement(String uri, String localName, String qName) {
        if (innerText != null)
            System.err.println("\t" + new String(innerText));
        System.err.println("\t</" + qName + ">");

        // anything containing just text, such as <name>foo</name>,
        // can be initialized here, because innerText will contain "foo"

        // string Location attributes
        if (qName.equals("name")) {
            if (innerText != null)
                name = new String(innerText);
        } else if (qName.equals("code")) {
            if (innerText != null)
                code = new String(innerText);
        } else if (qName.equals("keywords")) {
            if (innerText != null)
                keywords = new String(innerText);
        }

        // the end of an alias tag means we add this alias to the list of
        // aliases
        else if (qName.equals("alias")) {
            if (aliases != null && innerText != null)
                aliases.add(new String(innerText));
        }

        // The Location object itself: create a new Location
        // While we were still inside the tag, we should have set lots of
        // temporary variables (x, y, name, etc). This is where we apply them to
        // the Location object.
        else if (qName.equals("location")) {
            Location newLoc = new Location(x, y, name);

            // if we had an alias block, add the aliases that were defined there
            if (aliases != null)
                for (String a : aliases)
                    newLoc.addAlias(a);

            // set boolean values
            newLoc.setCanPassThrough(passThrough);
            newLoc.setAllowIntersections(intersect);
            newLoc.setDisplayName(displayName);

            // set string fields
            if (code != null)
                newLoc.setBuildingCode(code);
            if (keywords != null)
                newLoc.setKeywords(keywords);

            // force the location's ID to be the value defined in the file
            if (id > 0)
                newLoc.ID = id;

            locations.add(newLoc);
        }

        // we're done with this text -- don't let it bleed into the next element
        innerText = null;
    }

    /**
     * This is called periodically on inner text of elements. We keep a
     * StringBuffer around to catch all the text inside each element. E.g.,
     * &lt;p&gt;foo bar baz&lt;/p&gt; would call this function on "foo bar baz"
     * (but the string could be split up, and the function called multiple times
     * on substrings)
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (innerText != null)
            innerText.append(ch, start, length);
    }

    /* ANYTHING BELOW HERE IS NOT PART OF THE INTERFACE */

    /**
     * Print an Attributes object. (Looks like a glorified array...)
     * 
     * @param a
     *            the Attributes object to print
     */
    public void writeAttrs(Attributes a) {
        for (int i = 0; i < a.getLength(); i++)
            System.err.println("\t\t[" + a.getQName(i) + " = " + a.getValue(i)
                    + "]");
    }

    /**
     * A wrapper for Integer.parseInt() -- if a NumberFormatException is thrown,
     * this method smothers it and returns 0 instead.
     * 
     * @param s the string to parse
     * @return the integer value of <code>s</code>, or 0 on an exception
     */
    public int parseInt(String s) {
        // try to return the string as an int
        try {
            return Integer.parseInt(s);
        }
        // but if we can't parse it, return 0
        catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Convert a string to a boolean value. The string "true" (in any
     * capitalization) is true, and everything else is false.
     * 
     * @param s
     *            the string to parse
     * @return true if the lowercase version of <code>s</code> is "true", else
     *         false
     */
    public boolean parseBoolean(String s) {
        // "true" is true, everything else is false
        return (s.toLowerCase().equals("true"));
    }
}
