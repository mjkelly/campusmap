// vim: tabstop=4 shiftwidth=4 textwidth=79 expandtab
/*-----------------------------------------------------------------
 * $Id$
 * Editor for map path data.
 *
 * Uses examples from java.sun.com:
 * <http://java.sun.com/docs/books/tutorial/uiswing/components/scrollpane.html>
 * Original sections of ScrollablePicture class copyright 1994-2004 Sun
 * Microsystems, Inc. All Rights Reserved.
 * The rest copyright 2005 David Lindquist and Michael Kelly
 *
 * This program is released under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 -----------------------------------------------------------------*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;


public class ShowImage extends JFrame{
    
    // CONSTANTS
    private final int LOCATION_FIELD_WIDTH = 30;
    
    private ImageIcon img;
    public JTextField locationName;
    public JLabel statusBar;
    public ScrollablePicture ipanel;
    // For accessing the locationName text field


    /* Driver */
    public static void main(String[] args) {
        // If no arguments were supplied, output error message
        if(args.length == 0){
            System.err.println("usage: java ShowImage FILENAME");
            System.exit(2);
        }
      
        // set up and display the window
        ShowImage s = new ShowImage(args[0]);
        s.pack();
        s.setVisible(true);
        s.ipanel.readData();
    }

    /**
     * Create a new window holding the specified image.
     * @param filename filename of image to open
     */
    public ShowImage(String filename){

        super(filename);  // JFrame
        
        // Set background color
        this.setBackground(Color.LIGHT_GRAY);

        Container pane = this.getContentPane();

        // don't immediately exit on close, but rather call this handler
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter(){
           public void windowClosing(WindowEvent e){
               // show a dialog asking the user to confirm
               int userSays = confirmDialog("Write data?");
               
               // if the user says yes, save, then continue...
               if(userSays == JOptionPane.YES_OPTION)
                   ipanel.writeData();
               // if the user cancels, leave immediately
               else if(userSays == JOptionPane.CANCEL_OPTION)
                   return;
               
               // if we're here, the user said yes (and data has already
               // been saved), or they said no (and we didn't cancel)
               setVisible(false);
               dispose();
           }
        });
        
        // use a SpringLayout
        SpringLayout spring = new SpringLayout();
        pane.setLayout( spring );
        
        // img: private ImageIcon
        img = createImageIcon(filename);

        // create the other objects in the main window
        locationName = new JTextField("", LOCATION_FIELD_WIDTH);
        // this text is overridden in the ScrollablePicture constructor
        statusBar = new JLabel("Status Text");
        
        // See class listing below
        // Pass in ImageIcon, and maxUnitIncrement (10).
        ipanel = new ScrollablePicture(img, 10, this);

        // Create the main window: a scroll pane with the new image panel
        JScrollPane scroll = new JScrollPane(ipanel);
        scroll.setPreferredSize(new Dimension(600, 500));
        scroll.setWheelScrollingEnabled(false);
        
        // a button to clear the text field and remove its focus
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                locationName.setText("");
                ipanel.requestFocus();
            }
        });
        
        // Add all the objects to the main window's content pane
        pane.add(scroll);
        pane.add(locationName);
        pane.add(statusBar);
        pane.add(clearButton);
        
        // Everything that follows is layout/alignment stuff, 
        // with illustrations
        
        //  (main window)
        // +----------
        // |     ^^^
        // | <<< scroll
        spring.putConstraint(SpringLayout.WEST, scroll, 0, 
                SpringLayout.WEST, pane);
        spring.putConstraint(SpringLayout.NORTH, scroll, 0, 
                SpringLayout.NORTH, pane);
        
        // | (scroll)
        // +-------------
        // |     ^^^
        // | <<< locationName
        spring.putConstraint(SpringLayout.WEST, locationName, 5, 
                SpringLayout.WEST, pane);
        spring.putConstraint(SpringLayout.NORTH, locationName, 5, 
                SpringLayout.SOUTH, scroll);
        
        // |                 (scroll)
        // +---------------------
        //                |      ^^^
        // (locationName) | <<< clearButton
        spring.putConstraint(SpringLayout.WEST, clearButton, 5, 
                SpringLayout.EAST, locationName);
        spring.putConstraint(SpringLayout.NORTH, clearButton, 5, 
                SpringLayout.SOUTH, scroll);
        
        // | (locationName) | (clearButton)
        // +--------------------------
        // |                   ^^^
        // | <<< statusBar
        spring.putConstraint(SpringLayout.WEST, statusBar, 5, 
                SpringLayout.WEST, pane);
        spring.putConstraint(SpringLayout.NORTH, statusBar, 5, 
                SpringLayout.SOUTH, clearButton);
        
        //           scroll | <<<
        // statusBar        |
        // -----------------+
        // ^^^
        spring.putConstraint(SpringLayout.EAST, pane, 0, 
                SpringLayout.EAST, scroll);
        spring.putConstraint(SpringLayout.SOUTH, pane, 5, 
                SpringLayout.SOUTH, statusBar);

    }
    
    /**
     * Shows a confirmation dialog box with the description being
     * based on the message passed in.  
     */
    public int confirmDialog (String message)
    {
    	// Call showConfirmDialog on JOptionPane!
		return(JOptionPane.showConfirmDialog(this, message));
    }
    
    /**
     * Returns an ImageIcon, or null if the path was invalid.
     * @param path file path to image to load
     * */
    protected static ImageIcon createImageIcon(String path){
        java.net.URL imgURL = ShowImage.class.getResource(path);
        if (imgURL != null)
            return ( new ImageIcon(imgURL) );
        else{
            System.err.println("Couldn't find file: " + path);
            return (null);
        }
    }
}


/**
 * A picture suitable to placing in a JScrollPane, and handles mouse and key
 * events to allow modification of a set of path points. Points and connecting
 * lines are drawn over the image.
 */
class ScrollablePicture extends JLabel implements Scrollable, 
                                                  MouseMotionListener{
    private int maxUnitIncrement = 5;
    private boolean missingPicture = false;
    private Vector lines; // keep track of lines we've drawn
    private Vector paths; // Vector of paths (super of lines).
    private Vector locations;  // Vector of locations (see class Locations)
    private int pathNumIndex = 0; //Index of where we are  in the paths array.
    private int pointNumIndex = 0;
    private ShowImage parent;
    
    // Constructor
    public ScrollablePicture(ImageIcon i, int maxUnitPassed,
                                          ShowImage newParent) {
        super(i);
        
        parent = newParent;
        if (i == null) {
            missingPicture = true;
            setText("No picture found.");
            setHorizontalAlignment(CENTER);
            setOpaque(true);
            setBackground(Color.white);
        }
        maxUnitIncrement = maxUnitPassed;

        //Let the user scroll by dragging to outside the window.
        setAutoscrolls(true);         // enable synthetic drag events
        addMouseMotionListener(this); // handle mouse drags

        // define a short anonymous inner class to feed our own
        // method (handleKey) the key events
        addKeyListener(new KeyAdapter(){
            public void keyPressed(KeyEvent e){ handleKey(e); }
        });
        
        // ...but also need to make sure we can receive (key?) focus
        setFocusable(true);
        
        paths = new Vector(128);
        locations = new Vector(128);

        paths.add( new Vector() );
        lines = (Vector)paths.get(pathNumIndex);
        parent.statusBar.setText( statusBarText() );
        
        // save the curent 'this' for the inner class
        final ScrollablePicture thisParent = this;
        
        /* add the mouse-click handler */
        addMouseListener(new MouseAdapter(){
        	
            public void mouseClicked(MouseEvent e){
                int x = e.getX();
                int y = e.getY();

                
                if(SwingUtilities.isLeftMouseButton(e)) {
                    // store the click location in a Point object
                    Point p = new Point(x, y);

                    // add this point to the list...
                    if(lines == null)
                    {
                    	lines = new Vector(128);
                    }
                    lines.add(p);
                
                    setPointNumIndex(true);
                    
                    // and redraw immediately to see the changes
                    repaint( getVisibleRect() );
                    
                    // if the location field was filled, add a location
                    if(!parent.locationName.getText().equals(""))
                    {
                        System.err.println(parent.locationName.getText());
                        locations.add(new Location(x, y,
                                parent.locationName.getText()));
                    }
                    
                    parent.statusBar.setText( statusBarText() );
                    parent.locationName.setText("");
                    
                    thisParent.requestFocus();
                }
                
                // Right click.
                else if(SwingUtilities.isRightMouseButton(e)) {

                    // Don't  create a new point in a path.
                    if( pathNumIndex < lines.size() )
                    {
                    	// check if there's an existing point
                    	int locIndex = findLocationAtPoint(
                    	        (Point)lines.get(pointNumIndex));
                    	
                    	// if there is one...
                    	System.err.println("Found location at index: " 
                    		+ locIndex);
                    	if(locIndex >= 0){
                    	    // if the text field is empty, delete the point
                    	    if(parent.locationName.getText().equals("")){
                    	        locations.remove(locIndex);
                    	    }
                    	    // otherwise, we replace the location at this point
                    	    // with another that has the new x/y location and 
                    	    // the name the user entered
                    	    else{
	                	        locations.set(locIndex, new Location(x, y,
	                	                parent.locationName.getText()));
                            	
	                	        System.err.println("Moving location...");
                            	parent.statusBar.setText( statusBarText() );
                                parent.locationName.setText("");
                                
                                thisParent.requestFocus();
                    	    }
                    	   
                    	}
                    	// if we DIDN'T find a matching point, and the text
                    	// field isn't empty, create a new point
                    	// this looks very similar to the code above, but
                    	// it's not quite the same.
                    	else if(!parent.locationName.getText().equals("")){
                    	    locations.add(new Location(x, y,
                                    parent.locationName.getText()));
                            
                            parent.statusBar.setText( statusBarText() );
                            parent.locationName.setText("");
                            
                            thisParent.requestFocus();
                	    }
                    	
                    	// move the point
                    	((Point)lines.get(pointNumIndex)).setLocation(x,y);
                    	
                    	repaint();
                    }
                    
                }
                
            }
        });

    }

    // this actually handles the key events; it's called by the anonymous
    // KeyAdapter subclass defined in the constructor
    private void handleKey(KeyEvent k){
        
        int c = k.getKeyCode();
        // F1: Erase current point (and location, if applicable)
        if(c ==  KeyEvent.VK_F1)
        {
        	// Only go back if there is a point to go to
            if(lines.size() >= 1)
            {
                // if there's a location here, delete it too
                int locIndex = findLocationAtPoint(
                        (Point)lines.get(pointNumIndex));
                if(locIndex >= 0)
                    locations.remove(locIndex);
                
                // remove the current node, decrement pointNumIndex
                lines.remove(pointNumIndex--);
                
                // Do checking of bounds on pointNumIndex
                setPointNumIndex(false);


                parent.statusBar.setText( statusBarText() );
                repaint();
            }
        }
        // F2: Go to previous path option
        else if(c == KeyEvent.VK_F2)
        {
        	// Only go back if a paths exists
            if(pathNumIndex >= 1){
            	// Set focus
                lines = (Vector)paths.get(--pathNumIndex);
                // Automatically focus on the last element
                setPointNumIndex(true);
                // Set statusbar
                parent.statusBar.setText( statusBarText() );
                repaint();
            }
        }
        // F3: Go to the next path (if exists)
        else if(c == KeyEvent.VK_F3)
        {
        	// Only advance to the next path if it exists
            if(pathNumIndex < paths.size() - 1)
            {
            	// Advance
                lines = (Vector)paths.get(++pathNumIndex);
                // Automatically focus on the last element
                setPointNumIndex(true);
                // Set statusBar
                parent.statusBar.setText( statusBarText() );
                repaint();
            }
        }
        // F4: Move backwards along elements in a path
        else if(c == KeyEvent.VK_F4){
        	if(pointNumIndex > 0)
        	{
	        	// Decrement index
	            pointNumIndex--;
	            repaint();
	            // Show status bar
	            parent.statusBar.setText( statusBarText() );
        	}
        }
        // F5: Move forwards along elements in a path
        else if(c == KeyEvent.VK_F5){
        	if(pointNumIndex < lines.size() - 1)
        	{
	        	// Increment
	        	pointNumIndex++;
	        	repaint();
	        	// Show status bar
	            parent.statusBar.setText( statusBarText() );
	        }
        }
        // F7: Save data into files
        else if(c ==  KeyEvent.VK_F7)
        {
            if(parent.confirmDialog("Write data?") == JOptionPane.YES_OPTION)
                writeData();
            else
    			parent.statusBar.setText("Writing of data canceled");
        }
        // F8: load/read from files.
        else if(c ==  KeyEvent.VK_F8)
        {
            if(parent.confirmDialog("Load/read?") == JOptionPane.YES_OPTION)
            	readData();
            else
        		parent.statusBar.setText("Reading of data canceled");
        }
        // F9: Print locations to file
        else if(c == KeyEvent.VK_F9){
        	FileOutputStream textOutput;
        	PrintStream outStream;
        	
        	try{
        		textOutput =  new FileOutputStream("Locations.txt");
        		outStream = new PrintStream( textOutput );
        		
        		outStream.println("Locations:");
        		
        		// loop through each location and print its name and
        		// (x,y) coordinates
        		for(int locIndex = 0; locIndex < locations.size(); locIndex++)
        		{
        			outStream.println("Location " + (locIndex + 1) + " of " +
        					locations.size() + ": " + 
        					getLocation(locIndex).toString());
        		}
        		parent.statusBar.setText("Locations listing created");
        		outStream.close();
        	}
        	catch(Exception e){
        		System.err.println("Error writing to file!");
			}
        }
        // F10: Manually place dialog
        else if(c == KeyEvent.VK_F10)
        {
        	manualPlaceDialog();
        }
        // F12: Create new path
        else if(c == KeyEvent.VK_F12)
        {
        	// The current pathNumIndex is the sizeof the vector before
        	// The new element is created.
            pathNumIndex = paths.size();
            // Create the space for the new path.
            paths.add( new Vector() );
            // Set focus
            lines = (Vector)paths.get(pathNumIndex);
            pointNumIndex = 0;
            // Status bar
            parent.statusBar.setText( statusBarText() );
            repaint();
        }
        // Take a wild gusss :)
        else{
            System.err.println("I'm sorry, this key does not" +
            		" have a defined option");
        }
    }
    
    /**
     * Write data to disk.
     */
    public void writeData(){
        boolean pathWriteSuccess = false;
    	boolean locationWriteSuccess = false;
		// Load files
		final String pathFileName = "rawPathData.dat";
		final String locFileName = "rawLocationData.dat";
		final String pathNotFound = 
			"File \"" + pathFileName + "\" not found!     ";
		final String locNotFound = "File \"" + locFileName + "\" not found!";
    	// Define output files for paths and locations.
        File pathOutputFile = new File(pathFileName);
        File locationOutputFile = new File(locFileName);
        try{
        	// Write out the paths vector
            ObjectOutputStream pathout = new ObjectOutputStream(
                    new FileOutputStream(pathOutputFile));
            pathout.writeObject(paths);
            pathWriteSuccess = true;
            pathout.close();
        }
	    catch(FileNotFoundException e){
	    	System.err.println(pathNotFound);
	    	parent.statusBar.setText(pathNotFound);
	    }
	    catch(IOException e){
	    	System.err.println("Error in writing \"" + pathFileName + "\"!");
	    }
	    
        try{
            // Write out the locations vector
            ObjectOutputStream locout = new ObjectOutputStream(
                    new FileOutputStream(locationOutputFile));
            locout.writeObject(locations);
            locationWriteSuccess = true;
            locout.close();
        }
	    catch(FileNotFoundException e){
	    	System.err.println(locNotFound);
	    	parent.statusBar.setText(parent.statusBar.getText() + locNotFound);
	    }
	    catch(IOException e){
	    	System.err.println("Error in writing \"" + locFileName + "\"!");
	    }
	    if(locationWriteSuccess && pathWriteSuccess){
	        // Set status
	        parent.statusBar.setText("Paths and locations written to file");
        }
    }
    
    /**
     * Read data from disk.
     *
     */
    public void readData(){
    	boolean pathLoadSuccess = false;
    	boolean locationLoadSuccess = false;
		// Load files
		final String pathFileName = "rawPathData.dat";
		final String locFileName = "rawLocationData.dat";
		final String pathNotFound = 
			"File \"" + pathFileName + "\" not found!     ";
		final String locNotFound = "File \"" + locFileName + "\" not found!";
		// Define input files for paths and locations.
	    File pathsInputFile = new File(pathFileName);
	    File locationsInputFile = new File(locFileName);
	    
	    // Get the paths vector
	    try{
	    	// Open stream
	        ObjectInputStream pathin = new ObjectInputStream(
	                new FileInputStream(pathsInputFile));
	        // Get Vector
	        paths = (Vector)pathin.readObject();
	        pathLoadSuccess = true;
	    }
	    catch(FileNotFoundException e){
	    	System.err.println(pathNotFound);
	    	parent.statusBar.setText(pathNotFound);
	    }
	    catch(Exception e){
	    	System.err.println("Error in opening \"" + pathFileName + "\"!");
	    }
	    
	    // Get the locations vector
	    try{
	        // Open stream
	        ObjectInputStream locin = new ObjectInputStream(
	                new FileInputStream(locationsInputFile));
	        // Get Vector
	        locations = (Vector)locin.readObject();
	        locationLoadSuccess = true;
	    }
	    catch(FileNotFoundException e){
	    	System.err.println(locNotFound);
	    	parent.statusBar.setText(parent.statusBar.getText() + locNotFound);
	    }
	    catch(Exception e){
	    	System.err.println("Error in opening \"" + locFileName + "\"!");
	    }
	    if( locationLoadSuccess && pathLoadSuccess ){
		    
	        // Set status bar
	        parent.statusBar.setText("Input read from file");
	
	        // Set the active path (also termed path in "focus")...
	        
	        // If the current pathNumIndex (path focus) is greater 
	        // than the size of the paths array that was just read in...
	        // Then attach focus to the last path in the paths array.
	        if(pathNumIndex > paths.size() - 1)
	            pathNumIndex = paths.size();
	        
	        //Set lines
	        lines = (Vector)paths.get(pathNumIndex);
	        
	        // Do the repaint dance...woooo!
	        repaint();
        }
	}


    /**
     * Set the pointNumber index
     * 
     * For a true value passed in, set the pointNumIndex to the last element
     * in the vector of elements.  
     * 
     * For a false value passed in, check the bounds of pointNumIndex.
     */
    public void setPointNumIndex (boolean setAtEndPoint)
    {
    	if(setAtEndPoint == true){
    		//If empty, set to 0
	    	if(lines.size() == 0)
	    		pointNumIndex = 0;
	        //Otherwise set to it's size.  
	    	else
	    		pointNumIndex = lines.size() - 1;
    	}
    	else{
            // make sure pointNumIndex is in-bounds
            if(pointNumIndex < 0)
                pointNumIndex = 0;
            if(pointNumIndex >= lines.size())
                pointNumIndex = lines.size() - 1;
    	}
    		
    }
    
    
    /**
     * Display a dialog that allows the user to manually place (if the 
     * last node is selected), or move (if a previous node is selected) a node.
     */
    public void manualPlaceDialog ()
    {
    	final JDialog dialog = new JDialog(parent, "Manually place", true);
    	dialog.getContentPane().setLayout( new FlowLayout() );
    	JButton done = new JButton("Done");
    	JButton cancel = new JButton("Cancel");
    	final JTextField xinput = new JTextField(5);
    	final JTextField yinput = new JTextField(5);
    	final JLabel message = new JLabel("Enter a coordinate:  ");

    	// add all the interface elements the dialog box
    	dialog.getContentPane().add(message);
    	dialog.getContentPane().add(xinput);
    	dialog.getContentPane().add(yinput);
    	dialog.getContentPane().add(done);
    	dialog.getContentPane().add(cancel);

    	dialog.pack();
    	
    	// add a handler for the cancel button, which hides and
    	// disposes the dialog
    	cancel.addActionListener(new ActionListener(){
    		public void actionPerformed(ActionEvent e){
    			dialog.setVisible(false);
    			dialog.dispose();
    		}
    		
    	});

    	// the "done" button's handler handles most of the actual work:
    	// it gets the data the user entered and moves/adds the point.
    	done.addActionListener(new ActionListener(){
    		public void actionPerformed(ActionEvent e){
    			dialog.setVisible(false);
    			dialog.dispose();
    			Point newPoint;
    			try{
        			newPoint = 
        				new Point(Integer.parseInt(xinput.getText()), 
        						Integer.parseInt(yinput.getText()));				
    			}
    			catch(NumberFormatException numException){
    				parent.statusBar.setText("No input entered! (Bad format)");
    				return;
    			}
    			if (pointNumIndex < lines.size() - 1)
    			{
    				lines.set(pointNumIndex,newPoint);
    			}
    			else
    			{
    				lines.add(newPoint);
    				pointNumIndex = lines.size() - 1;
    			}
    			
    			repaint();
    		}
    	});
    	
    	dialog.setVisible(true);
    }
    
    
    /**
     * Return the status bar text
     * Write: Current path number in focus, number of elements in current
     *        path, number of paths, and any location string associated
     *        with the current in focus point.  
     **/
    public String statusBarText (){
    	int elementNumber = pointNumIndex + 1;
    	// Check bounds on the elementNumber
        if(elementNumber < 0)
            elementNumber = 0;
        if(elementNumber >= lines.size())
            elementNumber = lines.size();
    	
    	return ( "Path Number: " + (pathNumIndex + 1) + " of " + paths.size()+
			",  Element: " + (elementNumber) + " of " + lines.size() +
			printCurrentPoint() + printCurrentLocation() );
    }
    
    /**
     * Print the name of the location if there is a location that intersects
     * with the current point.
     * @return name of location if exist, else null string.
     */
    public String printCurrentLocation () {
    	if( locations.size() > 0 && lines.size() > 0)
    	{
    		int locIndex=findLocationAtPoint((Point)lines.get(pointNumIndex));
    		// For all locations
			if (locIndex >= 0)
	    		return(", Locations: " + getLocation(locIndex).name);    				
    	}
    	return("");
    }
    
    /**
     * Find the index of any location that exists at a given point.
     * @param pointToCompare Point to search at
     * @return the index of the matching location, or -1 if none is found.
     */
    public int findLocationAtPoint(Point pointToCompare){
    	for(int locIndex = 0; locIndex < locations.size(); locIndex++){
			if ((getLocation(locIndex).cord).equals(pointToCompare))
			{
				return(locIndex);
			}
    	}
    	return(-1);
    }
    
    /**
     * Print the (x,y) coordinates of the current point.
     * @return string as described, else empty string.
     */
    public String printCurrentPoint (){
    	// Print out point only if the size is greater than 0 &
    	// The pointNumIndex is at least zero
    	if( lines.size() > 0)
    	{
    		return(",  @ (" + ((Point)lines.get( pointNumIndex )).x + 
    				", " + ((Point)lines.get( pointNumIndex )).y + ")");
    	}
    	else
    		return("");
    }
    //Methods required by the MouseMotionListener interface:
    public void mouseMoved(MouseEvent e) { }

    public void mouseDragged(MouseEvent e) {
        //The user is dragging us, so scroll!
        Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
        scrollRectToVisible(r);
    }

    public Dimension getPreferredSize() {
        if (missingPicture) {
            return ( new Dimension(320, 480) );
        } 
        else {
            return ( super.getPreferredSize() );
        }
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect,
                                          int orientation,
                                          int direction) {
        return maxUnitIncrement;
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect,
                                           int orientation,
                                           int direction) {
        return maxUnitIncrement;
    }

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    // Set the maxUnitIncrement using passed in value.  
    public void setMaxUnitIncrement(int pixels) {
        maxUnitIncrement = pixels;
    }

    /**
     * Override the paintComponent method so we can draw lines
     * Draws all of the paths in memory according to the 
     * paths field (Vector).  Color codes the paths according to
     * their current focus.  
     **/
    public void paintComponent(Graphics g){
    	// Constants for colors dependant on dot/line status
    	final Color LASTPLACED_DOT = Color.BLUE;
    	final Color IN_FOCUS_PATH = Color.GREEN;
    	final Color OUT_OF_FOCUS_PATH = Color.RED;
    	final Color LOCATION_COLOR = Color.MAGENTA;
    	
        // first we paint ourselves according to our parent;
        // this should take care of drawing the actual image
        super.paintComponent(g);

        // Get the visible rectangle to decide if we can point the point
        Rectangle visible = getVisibleRect();
        
        // Set the default color to red (OUT_OF_FOCUS)
        g.setColor(OUT_OF_FOCUS_PATH);
        
        // Declare a current and previous point.  
        Point cur = null;
		Point prev = null;
        // draw all the dots that are VISIBLE
		
		// For every path in the paths array...
        for(int pathsIndex = 0; pathsIndex<paths.size(); pathsIndex++)
        {
        	// For every point along a path...
		    for(int pointInPath = 0; 
		        pointInPath < ( (Vector)paths.get( pathsIndex) ).size(); 
		        pointInPath++)
		    {
		    	// This is a bit messy...
		    	// Get the current point by first getting the apppropriate
		    	// path from paths Vector, cast to a Vector (as it should be)
		    	// and then get the point in the vector & cast it to a Point.
		        cur = (Point) ((Vector)paths.get(pathsIndex)).get(pointInPath);
		        
		        
	        	// If the path that we're drawing is the current one in
	        	if(lines == (Vector)paths.get(pathsIndex))
	        	{
	        		if(pointInPath == pointNumIndex)
	        		{
	        			g.setColor(LASTPLACED_DOT);
	        		}
	        		else
	        		    g.setColor(IN_FOCUS_PATH);
	        		
			        // if we have a "previous" dot to connect to, and at least
			        // ONE of the dots is visible, connect the two with a line
			        if(prev != null && 
			        		(visible.contains(cur) || visible.contains(prev))){
			            connectTheDots(g, prev, cur);
			        }
	        	}
		        // Else, it's at's a previously placed dot
	        	else
	        	{
	        		g.setColor(OUT_OF_FOCUS_PATH);
			        // if we have a "previous" dot to connect to, and at least
			        // ONE of the dots is visible, connect the two with a line
			        if(prev != null && 
			        		(visible.contains(cur) || visible.contains(prev))){
			            connectTheDots(g, prev, cur);
			        }
			        
			        if(pointInPath == ((Vector)paths.get(pathsIndex)).size()-1)
			        {
			        	g.setColor(LASTPLACED_DOT);
			        }
			        	
	        	}

		        // now draw the actual dot
		        if(visible.contains(cur))
		            drawDot(g, cur);
		        
		        // Assign the current value as the previous
		        prev = cur;
		    }
		    // Previous point is null, because we're going to go into a new
		    // path
		    prev = null;
        }
        g.setColor(LOCATION_COLOR);
        
        // Draw the names of all the locations
        for(int locIndex = 0; locIndex < locations.size(); locIndex++)
        {
        	int x = getLocation(locIndex).cord.x;
        	int y = getLocation(locIndex).cord.y;
        	g.drawString( getLocation(locIndex).toString(), x, y);
        }
        
    }


    public Location getLocation(int locIndex){
    	return((Location)locations.get(locIndex));
    }
    
    /**
     * Draw a dot on the specified Graphics object at the specified Point
     * Fills a rectangle around the point, paints according to the Graphic
     * component.
     **/
    public void drawDot(Graphics g, Point p){
            g.fillRect( (int)p.getX()-2, (int)p.getY()-2, 5, 5 );
    }

    /**
     * Connect the two passed in points. 
     * This is really just a call to drawLine()
     * Paints according to the passed in Graphics object.  
     **/
    public void connectTheDots(Graphics g, Point start, Point end){
        g.drawLine( (int)start.getX(), (int)start.getY(),
                    (int)end.getX(), (int)end.getY() );
    }
}

/**
 * Location class.  Simply an object contatining a point (Point cord) and 
 * a description of the point (String name)
 **/
class Location implements Serializable
{
	public Point cord;   // Coordinate of the point
	public String name;  // Name of the point
	
	public Location(int x, int y, String passedName)
	{
		cord = new Point(x,y);  // Create coordinate based on passed in values
		name = passedName;      // Copy name string (Strings are constants!)
		System.err.println("New location @ " + cord);
	}
	public String toString()
	{
		return(name + " @ (" + cord.x + ", " + cord.y + ")");
	}
}