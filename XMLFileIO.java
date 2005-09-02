/*
 * XMLFileIO.java, Aug 26, 2005
 * mkelly
 * Part of UCSDMap.
 *
 *
 *
 * Aug 26, 2005
 */

import java.io.*;
import java.util.*;
import java.awt.Point;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

/**
 * Wrapper class for loading and saving paths and locations in XML format.
 * (It's all quite SAXy.)
 */
public class XMLFileIO {

    /** Print lots of info to trace execution of XML read/write methods? */
    static final boolean VERBOSE = false;
	
	/**
	 * Writes out the passed vector of locations to the passed filename
	 * @param outFile The filename to open an output stream to
	 * @param locVector The vector of locations to write out
	 * @return Error string if one occured, else null.
	 */
	public static String writeLocations(
			String outFile, Vector<Location> locVector)
	{
    	File xmlOut = new File(outFile);
    	OutputStreamWriter out = null;
    	
    	// Open ye stream
    	try{
    		out = new OutputStreamWriter(new BufferedOutputStream(
    				new FileOutputStream(xmlOut)));
    	}
    	catch(IOException e){
    		System.err.println("Error creating file " + xmlOut);
    		e.printStackTrace();
    		return("Error writing location to: " + xmlOut);
    	}
    	// Write out the locations
    	try{
    		writeXMLLocationData(locVector, out);
    		out.close();
    	}
    	catch(IOException e){
    		System.err.println("Error writing out to file " + xmlOut);
    		return("Error occured in writing process: " + xmlOut);
    	}
		return null;
	}
	
    /**
     * Writes out the passed location data to the passed OutputStreamWriter.
     * (Don't call this, call <code>writeData</code>. But it's private
     * anyway, so you can't.)
     * @param out The StreamWriter to use to write out with
     * @param locations The location data
     * @throws IOException an error the occurs in attempting to write
     */
	private static void writeXMLLocationData(Vector<Location> locations, 
			OutputStreamWriter out) throws IOException
	{
		final String TAB = "\t";
    	// Write out the prolog
    	out.write("<?xml version='1.0' encoding='UTF-8' ?>\n");
    	
    	// Write out the main Location wrapper
    	out.write("<locations version=\"1.0\">\n");
    	
    	for(Location l: locations)
    	{
    		String locTag = "\t<location ";
    		//<location x="100" y="100" id="1" passThrough="true" intersect="true" displayName="true">
    		// x cord, y cord
    		locTag += "x=\"" + l.cord.x + "\" y=\"" + l.cord.y + "\" ";
    		//ID
    		locTag += "id=\"" + l.ID + "\" ";

    		locTag += "passThrough=\"" + 
    			(l.isCanPassThrough() ? ("true"): ("false")) + "\" ";
    		
    		locTag += "intersect=\"" + 
			(l.isAllowIntersections() ? ("true"): ("false")) + "\" ";
    		
    		locTag += "displayName=\"" + 
			(l.isDisplayName() ? ("true"):("false")) + "\"";
    		
    		//Close the tag
    		locTag += ">\n";
    		
    		// Start the Location field
    		out.write(locTag);
    		
    		// Write out the name
    		//<name>York Hall</name>
    		out.write(TAB + TAB + terminal("name", l.getName()) + "\n"); 

    		// Write out the aliases, if any exist
    		if(l.getAliases() != null && l.getAliases().size() > 0)
    		{
    			out.write("\t\t<aliases>\n");
    			for(String alias : l.getAliases())
    			{
    				out.write(TAB + TAB + TAB + terminal("alias", alias) + "\n"); 
    			}
    			out.write("\t\t</aliases>\n");
    		}
    		
    		// write out the keyword field
    		if(l.getKeywords().length() > 0)
    			out.write(TAB + TAB + terminal("keywords", l.getKeywords()) + "\n"); 

    		// write out the building code
    		if(l.getBuildingCode().length() > 0)
    			out.write(TAB + TAB + terminal("code", l.getBuildingCode()) + "\n"); 
    		
    		// End the location tag
    		out.write("\t</location>\n");
    	}
    	// Close the locations
    	out.append("</locations>");
	}

    /**
     * Return a terminal tag of the given name with the given contents.
     * The contents are escaped.
     * @param name the name of the tag to return
     * @param content the content of the tag, unescaped
     * @return a terminal tag named <code>name</code>, containing
     * <code>content</code>
     */
	public static String terminal(String name, String content)
	{
		return("<" + name + ">" + toXMLStr(content) + "</" + name + ">");
	}
    
	/**
	 * Converts the passed in string to a valid XML content string
     * TODO: Check XML specs... specifically what characters must be escaped?
	 * @param s The string to convert
	 * @return valid XML string
	 */
	public static String toXMLStr(String s)
	{
		s = s.replaceAll("&", "&amp;");
		s = s.replaceAll("<", "&lt");
		s = s.replaceAll(">", "&gt");
		s = s.replaceAll("'", "&apos;");
		s = s.replaceAll("\"", "&quot;");
		return s;
	}
	/**
	 * Write out the passed in path data to the passed in filename
	 * @param outFile The name of the file to write to
	 * @param paths The data to write
	 * @return Error if one occured, else null.
	 */
	public static String writePaths(
			String outFile, Vector<Vector<Point>> paths)
	{
    	File xmlOut = new File(outFile);
    	OutputStreamWriter out = null;
    	
    	// Open ye stream
    	try{
    		out = new OutputStreamWriter(new BufferedOutputStream(
    				new FileOutputStream(xmlOut)));
    	}
    	catch(IOException e){
    		System.err.println("Error creating file " + xmlOut);
    		e.printStackTrace();
    		return("Error opening file: " + xmlOut);
    	}
    	// Write out the locations
    	try{
        	final String TAB = "\t";
        	out.write("<?xml version='1.0' encoding='UTF-8' ?>\n\n");
        	
        	// Open the container for all the paths
    		out.write("<paths>\n");
    		
    		// for each path in the set of paths
    		for(Vector<Point> path : paths)
    		{
    			// if path isn't empty, write it out
    			if(path.size() > 0)
    			{
    				// Open path
    				out.write(TAB + "<path>\n");
    				// write the points in the path
    				for(Point p : path)
    				{
    					out.write(TAB+TAB + "<point x=\"" + p.x + 
    							"\" y=\"" + p.y + "\"/>\n");
    				}
    				// close path
    				out.write(TAB + "</path>\n");
    			}
    		}
    		// close container for all paths
    		out.write("</paths>\n");
    		out.close();
    	}
    	catch(IOException e){
    		System.err.println("Error writing out to file " + xmlOut);
    		return ("Error in writing out to file " + xmlOut);
    	}
    	return null;
	}

	
    /**
     * Load a series of locations from an XML file.
     * @param locationFile The file to load
     * @return the locations contained by the XML file.
     */
    public static Vector<Location> loadLocations(String locationFile) {
        SAXParserFactory sax = SAXParserFactory.newInstance();
        Vector<Location> locs = new Vector<Location>();
        
        try {
            SAXParser parser = sax.newSAXParser();
            parser.parse(locationFile, new LocationHandler(locs));
        } catch (ParserConfigurationException e) {
            System.err.println("ParserConfigurationException: "
                    + e.getMessage());
            return locs;  // return locations obtained so far
        } catch (SAXException e) {
            System.err.println("SAXException: " + e.getMessage());
            return locs;
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            return locs;
        }
        if(VERBOSE)
        {
        	System.err.println("Locations parsed:");
        	for (Location l : locs) {
        		System.err.println(l);
        	}
        }
        return locs;
    }
    
    /**
     * Load paths from an XML file.
     * @param pathFile The XML file to load from
     * @return a vector of vectors containg all the loaded points
     */
    public static Vector<Vector<Point>> loadPaths(String pathFile) {
    	boolean printLoadedPaths = false;
        SAXParserFactory sax = SAXParserFactory.newInstance();
        Vector<Vector<Point>> points = new Vector<Vector<Point>>();

        try {
            SAXParser parser = sax.newSAXParser();
            parser.parse(pathFile, new PathHandler(points));
        } catch (ParserConfigurationException e) {
            System.err.println("ParserConfigurationException: "
                    + e.getMessage());
            return points; // return the points parsed out so far
        } catch (SAXException e) {
            System.err.println("SAXException: " + e.getMessage());
            return points;
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            return points;
        }

        if(printLoadedPaths)
        {
        	System.err.println("Points parsed:");
        	for (Vector<Point> v : points) {
        		System.err.println("Path:");
        		for(Point p : v){
        			System.err.println("\t" + p);
        		}
        	}
        }

        return points;
    }

    
    /**
     * Print an Attributes object. (Looks like a glorified array...)
     * 
     * @param a
     *            the Attributes object to print
     */
    public static void writeAttrs(Attributes a) {
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
    public static int parseInt(String s) {
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
    public static boolean parseBoolean(String s) {
        // "true" is true, everything else is false
        return (s.toLowerCase().equals("true"));
    }
    
}

/**
 * This class handles all the SAX callbacks for a path XML file.
 */
class PathHandler extends DefaultHandler {
    Vector<Vector<Point>> points;
    Vector<Point> curPath;
    
    /**
     * Constructor for PathHandler, sets the points vector.
     * @param points The collection of points to write.
     */
    public PathHandler(Vector<Vector<Point>> points){
        this.points = points;
    }
    
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) {
        
        // when we encounter a <path> tag, add another vector
        if (qName.equals("path")) {
            curPath = new Vector<Point>();
            points.add(curPath);
        }
        // a <point> tag is just another Point() in the current path...
        else if(qName.equals("point")){
            int x = XMLFileIO.parseInt(attributes.getValue("x"));
            int y = XMLFileIO.parseInt(attributes.getValue("y"));
            if(curPath != null)
                curPath.add(new Point(x, y));
        }
    }
}

/**
 * This class handles all the SAX callbacks for a location XML file.
 */
class LocationHandler extends DefaultHandler {
    
    /** Whether or not we alter location IDs to be minimum and contiguous. */
    public static boolean compressIDs = false;
    
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

    int maxLocID = 0;
    
    /**
     * Constructor, stores the locations to write
     * @param locations Set of locations to write out
     */
    public LocationHandler(Vector<Location> locations) {
        this.locations = locations;
    }

    /**
     * Called when an element begins. I.e., &lt;p&gt;. In a simple XML document,
     * qName would be "p" in that case. (That's what we use here.)
     */
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) {

        if(XMLFileIO.VERBOSE){
        	System.err.println("\t<" + qName + ">");
    		XMLFileIO.writeAttrs(attributes);
        }

        // reset the buffer storing inner characters of this element
        innerText = new StringBuffer(64);

        // this is an individual location
        // we initialize variables representing the location's members to
        // default values, and get a few basic attributes (id, location, boolean
        // flags).
        if (qName.equals("location")) {
            // when a new Location comes around, reset all temporary variables
            // to default values
            x = y = 0;
            name = null;
            code = null;
            keywords = null;
            aliases = null;
            passThrough = false;
	    intersect = true;
            displayName = true;
            id = 0;

            // get the integer attributes of the location
            if (attributes.getValue("id") != null) {
                if(LocationHandler.compressIDs){
                    id = ++maxLocID;
                    if(XMLFileIO.VERBOSE)
                        System.err.println("[[Compressing to ID = " + id + "]]");
                }
                else{
                    id = XMLFileIO.parseInt(attributes.getValue("id"));
                }
                
                // keep track of the largest ID we've seen so far
                if(id > maxLocID){
                    maxLocID = id;
                }
            }
            if (attributes.getValue("x") != null)
                x = XMLFileIO.parseInt(attributes.getValue("x"));
            if (attributes.getValue("y") != null)
                y = XMLFileIO.parseInt(attributes.getValue("y"));

            // get the boolean attributes of the location
            if (attributes.getValue("passThrough") != null)
                passThrough = XMLFileIO.parseBoolean(attributes.getValue("passThrough"));
            if (attributes.getValue("intersect") != null)
                intersect = XMLFileIO.parseBoolean(attributes.getValue("intersect"));
            if (attributes.getValue("displayName") != null)
                displayName = XMLFileIO.parseBoolean(attributes.getValue("displayName"));

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
    	if(XMLFileIO.VERBOSE)
        {
    		if (innerText != null)
    			System.err.println("\t" + new String(innerText));
    		System.err.println("\t</" + qName + ">");
        }

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
     * When the document ends, we do a little housekeeping.
     */
    public void endDocument(){
        Location.IDcount = maxLocID + 1;
        if(XMLFileIO.VERBOSE){
            System.err.println("At end of document. Location.IDcount = "
                    + Location.IDcount);
        }
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

 }
