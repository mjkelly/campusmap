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
        
        // Prompt for a reading option
        s.ipanel.selectRead();
        
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
               ipanel.selectWrite();
               
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
        
        /**
         * For searching for a particular location
         */
        JButton searchLocation = new JButton("search");
        searchLocation.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		// Search for the location that is currently entered in
        		// getText
        		ipanel.searchAndFocusToLocation(locationName.getText());
        	}
        });
        
        // Add all the objects to the main window's content pane
        pane.add(scroll);
        pane.add(locationName);
        pane.add(statusBar);
        pane.add(clearButton);
        pane.add(searchLocation);
        
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
        
        // |                  (scroll)
        // +-------------------------------
        //                                     ^^^
        // (locationName) | clearButton | <<< searchLocation
        
        spring.putConstraint(SpringLayout.WEST, searchLocation, 5,
        		SpringLayout.EAST, clearButton);
        spring.putConstraint(SpringLayout.NORTH, searchLocation, 5,
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
     * Returns an ImageIcon, or null if the path was invalid.
     * @param path file path to image to load
     * */
    protected static ImageIcon createImageIcon(String path){
        java.net.URL imgURL = ShowImage.class.getResource(path);
        if (imgURL != null)
            return ( new ImageIcon(imgURL) );
        System.err.println("Couldn't find file: " + path);
        return (null);
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
    // The name of the location that was last searched
    private String lastLocationSearched;
    // The index "index" number of the path going out from the last
    // searched node.  
    private int lastVertex = 0;
    
    /*
     * Input/Output filenames
     */
    // Raw, unoptimized paths
	final String rawPathFile = "data/rawPath.dat";
	final String rawLocFile = "data/rawLocations.dat";
	
	// Optimized paths put back into unoptimized format for debugging
	final String optPathFile = "data/optimizedPath.dat";
	final String optLocFile = "data/optimizedLocations.dat";
	
	// Binary file output
	final String binaryPoints = "data/binPointData.dat";
	final String binaryLocations =  "data/binLocationData.dat";
	final String binaryEdges = "data/binEdgeData.dat";
	
	// List of all locations mapped
    final String locationsTextFile = "Locations.txt";
	
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
                // If you use any other buttton on your mouse...
                /**
                 * Middle click design to open a menu to select a location
                 * 
                 */
                else
                {
                	int index; 
                	System.err.println("Other mouse click");
                	final JDialog dialog = new JDialog(parent, 
                			"Point to location", true);
                	dialog.getContentPane().setLayout( new FlowLayout() );
                	JButton cancel = new JButton("Cancel");
                	final JLabel message = new JLabel("Choose a location:  ");

                	// add all the interface elements the dialog box
                	dialog.getContentPane().add(message);
                	
                	// create an array for location names
                	final String locationNames[]=new String[locations.size()];
                	
                	//Put  every location
                	for(index = 0; index<locations.size(); index++)
                	{
                		// stick the location's name in the array
                		locationNames[index] = 
                			((Location)locations.get(index)).name;
                	}
                	
                	//Create the JList, passing the array of location names
                	final JList locBox = new JList(locationNames);
                	
                	// Add the list
                	dialog.getContentPane().add(locBox);
                	
                	// add a cancel button
                	dialog.getContentPane().add(cancel);
                	
                	// let's go packing!  You can't have a pack smaller than
                	// what you're packing.
                	dialog.pack();
                	
                	// Can you hear the cricket's chirping?
                	locBox.addMouseListener( new MouseAdapter(){
                		public void mouseClicked(MouseEvent event){
        					
                			// Get the point of the selected location
        					int location = locBox.getSelectedIndex();
        					Point tempPoint= 
        						((Location)locations.get(location)).cord;
        					Point newPoint = new Point(tempPoint);	
        					
        	    			// If we're not focused on the end point,
        	    			// change the current in focus point to the 
        					//entered point
        	    			if (pointNumIndex < lines.size() - 1)
        	    			{
        	    				lines.set(pointNumIndex,newPoint);
        	    			}
        	    			else
        	    			{
        	    				// Create the new point.  
        	    				lines.add(newPoint);
        	    				pointNumIndex = lines.size() - 1;
        	    			}
        	    			// Do the repaint dance!
        	    			repaint();
        	    			
        	    			// Close
                			dialog.setVisible(false);
                			dialog.dispose();
                		}
                	});
                	
                	// If cancel button was hit, 
                	cancel.addActionListener(new ActionListener(){
                		public void actionPerformed(ActionEvent innere){
                			dialog.setVisible(false);
                			dialog.dispose();
                			parent.statusBar.setText(
                					"Location pasting canceled!");
                		}
                	});
                	
                	dialog.setVisible(true);
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
        	// Call a method to handle undoing the connection
        	undoConnection();
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
        // F6: Center on currently selected point
        else if(c == KeyEvent.VK_F6)
        {
        	if(pointNumIndex <= lines.size() - 1)
        	{
	        	Point center = ((Point)lines.get(pointNumIndex));
	            Rectangle r = new Rectangle(
	            		center.x - (getVisibleRect().width)/2, center.y - 
						(getVisibleRect().height)/2, getVisibleRect().width, 
						getVisibleRect().height);
	            
	            scrollRectToVisible(r);
	        
        	}
        }
        // F7: Save data into files
        else if(c ==  KeyEvent.VK_F7)
        {
            selectWrite();
        }
        // F8: load/read from files.
        else if(c ==  KeyEvent.VK_F8)
        {
        	selectRead();
        }
        // F9: Print locations to file
        else if(c == KeyEvent.VK_F9){
        	FileOutputStream textOutput;
        	PrintStream outStream;
        	
        	try{
        		textOutput =  new FileOutputStream(locationsTextFile);
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
    public void writeData(String pathFileName, String locFileName){
        boolean pathWriteSuccess = false;
    	boolean locationWriteSuccess = false;
		// Load files
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
    
    
    public void undoConnection ()
    {
    	// Only go back if there is a point to go to
        if(lines.size() >= 1)
        {
            // Get a pointer to the point that we want to undo
        	Point undoPoint = (Point)lines.get(pointNumIndex);
        	

        	// Returns -1 if no index was found.
        	int locIndex = findLocationAtPoint(undoPoint);

        	
        	// if there's a location and it has less than two
        	// connections, delete the location in addition to the point.
        	// Get the index of a location at the point.
            if(locIndex >= 0 && numOfPathsConnectedToPoint(undoPoint) < 2)
                locations.remove(locIndex);
            
            // remove the current node, decrement pointNumIndex
            lines.remove(pointNumIndex--);
            
            // Do checking of bounds on pointNumIndex
            setPointNumIndex(false);


            parent.statusBar.setText( statusBarText() );
            repaint();
        }
    }
    
    
    /**
     * Read data from disk.
     *
     */
    public void readData(String pathFileName, String locFileName){
    	
    	boolean pathLoadSuccess = false;
    	boolean locationLoadSuccess = false;

    	// Load files

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
    
    public void selectWrite()
    {
    	final JDialog dialog = new JDialog(parent,
    			"Writing data options", true);
    	dialog.getContentPane().setLayout( new FlowLayout() );
    	JButton writeRaw = new JButton("Save raw data to disk");
    	JButton createOpt = new JButton("Write optimization files");
    	JButton cancel = new JButton("Cancel");
    	
    	dialog.getContentPane().add(cancel);
    	dialog.getContentPane().add(createOpt);
    	dialog.getContentPane().add(writeRaw);
    	
    	dialog.pack();
    	
    	/**
    	 * If cancel button is pressed, close dialog box and
    	 * display notification in the status bar. 
    	 */
    	cancel.addActionListener(new ActionListener(){
    		public void actionPerformed(ActionEvent e){
    			dialog.setVisible(false);
    			dialog.dispose();
    			parent.statusBar.setText("File writing canceled!");
    		}
    	});
    	
    	/**
    	 * If the create optimized data button is clicked,
    	 * Close dialog box, 
    	 */
    	createOpt.addActionListener(new ActionListener(){
    		public void actionPerformed(ActionEvent e){
    			dialog.setVisible(false);
    			dialog.dispose();
    			
    	    	//PathOptimize pathOp = new PathOptimize();
    	    	PathOptimize.run(rawPathFile, rawLocFile, 
    	    			optPathFile, optLocFile,
						binaryPoints, binaryLocations, binaryEdges
				);
    		}
    	});
    	
    	writeRaw.addActionListener(new ActionListener(){
    		public void actionPerformed(ActionEvent e){
    			dialog.setVisible(false);
    			dialog.dispose();
    			writeData(rawPathFile, rawLocFile);
    		}
    	});
    	
    	dialog.setVisible(true);
    	
    	
    }
    
    /**
     * Pops up a dialog box that asks what type of datafile the user wants
     * to load.  
     */
    public void selectRead()
    {
    	final JDialog dialog = new JDialog(parent, 
    			"Loading data options", true);
    	dialog.getContentPane().setLayout( new FlowLayout() );
    	JButton readRaw = new JButton("Load Raw Data");
    	JButton readOptimized = new JButton("Load optimized data output");
    	JButton cancel = new JButton("Cancel");
    	
    	// add all the interface elements the dialog box
    	dialog.getContentPane().add(cancel);
    	dialog.getContentPane().add(readOptimized);
    	dialog.getContentPane().add(readRaw);
    	
    	// Fit subcomponents
    	dialog.pack();

    	cancel.addActionListener(new ActionListener(){
    		public void actionPerformed(ActionEvent e){
    			dialog.setVisible(false);
    			dialog.dispose();
    			parent.statusBar.setText("Reading of data canceled!");
    		}
    	});
    	
    	readRaw.addActionListener(new ActionListener(){
    		public void actionPerformed(ActionEvent e){
    			dialog.setVisible(false);
    			dialog.dispose();
    			readData(rawPathFile, rawLocFile);
    		}
    	});
    	
    	readOptimized.addActionListener(new ActionListener(){
    		public void actionPerformed(ActionEvent e){
    			dialog.setVisible(false);
    			dialog.dispose();
    			readData(optPathFile, optLocFile);
    		}
    	});

    	dialog.setVisible(true);
    	
    
    
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
    			// Fling the dialog box out of the window.
    			dialog.setVisible(false);
    			dialog.dispose();
    			
    			Point newPoint;  // The point to be added.  
    			try{
    				// create the point
        			newPoint = 
        				new Point(Integer.parseInt(xinput.getText()), 
        						Integer.parseInt(yinput.getText()));				
    			}
    			catch(NumberFormatException numException){
    				parent.statusBar.setText("No input entered! (Bad format)");
    				return;
    			}
    			
    			// If we're not focused on the end point,
    			// change the current in focus point to the entered point
    			if (pointNumIndex < lines.size() - 1)
    			{
    				lines.set(pointNumIndex,newPoint);
    			}
    			else
    			{
    				// Create the new point.  
    				lines.add(newPoint);
    				pointNumIndex = lines.size() - 1;
    			}
    			// Do the repaint dance!
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
    
    public int numOfPathsConnectedToPoint(Point pointToCompare)
    {
    	int numPathsConnected = 0;
    	// step over all paths
    	for(int i = 0; i < paths.size(); i++)
    		// step through every point in the path
    		for(int j = 0; j < ((Vector)paths.get(i)).size(); j++)
    			// return the path index if a match is found
    			if(getPointInPath(i,j).equals(pointToCompare))
    				numPathsConnected++;
    	return(numPathsConnected);	
    }
    
    
    public void searchAndFocusToLocation(String locationName)
    {
    	int locIndex;
	

		// we're searching for a path intersecting with the locataion.
		
		// Find a location with the name passed in...we will assume
		// that no more than one exists
		locIndex = findLocationWithName(locationName);
		
		if(locIndex < 0)
			return;

		Location locFound = (Location)locations.get(locIndex);
		

		
		 //If the location passed in is equal to the last 
		//location passed in
		System.err.println("comparing: " + locationName + " and " + lastLocationSearched);

		System.err.println("LastVertex (before):  " + lastVertex);
		if(locationName.equals(lastLocationSearched))
    	{	
    		// We have a location (a point), now we need to find a path
    		// that it's on. 
    		lastVertex = getNextPathInSearch(++lastVertex, locFound.cord);
    	}
    	else
    		// We have a location (a point), now we need to find a path
    		// that it's on. 
    		lastVertex = getNextPathInSearch(0, locFound.cord);
		
		System.err.println("LastVertex (after):  " + lastVertex);
		
		// If no path was found, return
		if(lastVertex < 0)
			return;
		
		if(lastVertex > paths.size() - 1)
		{
			System.err.println("lastVertex is larger than paths vector!");
			return;
		}
		else if (lastVertex < 0)
		{
			System.err.println("lastVertex is less than 0!  \n" +
					"You messed up BIG time");
		}
		pathNumIndex = lastVertex;
		
        lines = (Vector)paths.get(pathNumIndex);
		
		//Set point number index to the end.  
		setPointNumIndex(true);
		
		//Do the repaint dance!
		repaint();

		lastLocationSearched = locationName;
		parent.statusBar.setText("Path #" + (pathNumIndex + 1) +
				" intersects with location \"" + locationName + "\"");
        
    }
    
    /**
     * Searches for a point inside of all the paths
     * @param startingIndex Which path index to start from.  
     * @param pointToFind The point that we want to match to
     * @return The index of the path (index in the paths vector)
     */
    public int getNextPathInSearch(int startingIndex, Point pointToFind)
    {
    	// step over all remaining paths
    	for(int i = startingIndex; i < paths.size(); i++)
    		// step through every point in the path
    		for(int j = 0; j < ((Vector)paths.get(i)).size(); j++)
    			// return the path index if a match is found
    			if(getPointInPath(i,j).equals(pointToFind))
    				return(i);
    	
    	// Return an error & output a message if path couldn't be found.  
		parent.statusBar.setText("Next Point/Path couldn't be found");
    	return(-1);
    	
    }
    
    public Point getPointInPath(int pathIndex, int elementIndex)
    {
    	return((Point)((Vector)paths.get(pathIndex)).get(elementIndex));
    }
    
    public int findLocationWithName (String locationName)
	{
    	for(int i = 0; i < locations.size(); i++)
    		if((((Location)locations.get(i)).name.equals(locationName)))
				return(i);
		parent.statusBar.setText("Corresponding location could" +
			" not be found!");
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
        // Otherwise, return
        return ( super.getPreferredSize() );
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
	int ID;
	public Point cord;   // Coordinate of the point
	public String name;  // Name of the point
	
	static int IDcount = 1;
	
	public Location(int x, int y, String passedName)
	{
		cord = new Point(x,y);  // Create coordinate based on passed in values
		name = passedName;      // Copy name string (Strings are constants!)
		ID = IDcount++;
		System.err.println("New location @ " + cord);
	}
	
	public String toString()
	{
		return(name + " @ (" + cord.x + ", " + cord.y + ")");
	}
}