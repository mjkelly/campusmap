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
 -----------------------------------------------------------------*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * Class ShowImage
 * This class displays an image.  The rest is trivial.  
 *
 */
public class ShowImage extends JFrame{
    
    // CONSTANTS
    private final int LOCATION_FIELD_WIDTH = 30;
    
    private ImageIcon img;
    /**
     * Text field to enter in the name of a location
     */
    public JTextField locationNameEntry;
    /**
     * For writing status text descriptions on current point and 
     * error/success messages.
     */
    public JLabel statusBar;
    /**
     * Our scrollable picture, the class contains the methods
     * that we will operate on.  
     */
    public ScrollablePicture ipanel;
    
    //JCheckboxes for locations
    JCheckBox intersectBox;
    JCheckBox passBox;
    JCheckBox displayBox;

    
    // For accessing the locationName text field

    /**
     * Driver
     * @param args The passed in command line arguments
     *             Should containe the file name to check
     */
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
        locationNameEntry = new JTextField("", LOCATION_FIELD_WIDTH);
        // this text is overridden in the ScrollablePicture constructor
        statusBar = new JLabel("Status Text");
        
        // See class listing below
        // Pass in ImageIcon, and maxUnitIncrement (10).
        ipanel = new ScrollablePicture(img, 10, this);

        // Create the main window: a scroll pane with the new image panel
        JScrollPane scroll = new JScrollPane(ipanel);
        scroll.setPreferredSize(new Dimension(600, 500));
        scroll.setWheelScrollingEnabled(false);
        
        // a button to remove focus from the text field, so
        // we can use hotkeys
        JButton clearButton = new JButton("Unfocus");
        clearButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                //locationNameEntry.setText("");
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
                ipanel.searchAndFocusToLocation(locationNameEntry.getText());
            }
        });
        
        // Checkboxes that determine properties of a location, default=true
        intersectBox = new JCheckBox("Intersect", true);
        passBox = new JCheckBox("pass through", true);
        displayBox = new JCheckBox("Display name", true);
        
        // Add all the objects to the main window's content pane
        pane.add(scroll);
        pane.add(locationNameEntry);
        pane.add(statusBar);
        pane.add(clearButton);
        pane.add(searchLocation);
        pane.add(intersectBox);
        pane.add(passBox);
        pane.add(displayBox);
        
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
        spring.putConstraint(SpringLayout.WEST, locationNameEntry, 5, 
                SpringLayout.WEST, pane);
        spring.putConstraint(SpringLayout.NORTH, locationNameEntry, 5, 
                SpringLayout.SOUTH, scroll);
        
        // |                 (scroll)
        // +---------------------
        //                |      ^^^
        // (locationName) | <<< clearButton
        spring.putConstraint(SpringLayout.WEST, clearButton, 5, 
                SpringLayout.EAST, locationNameEntry);
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
        
        
        // |                  (scroll)
        // +-------------------------------
        //                                     
        // ...  searchLocation <<< intersectBox
        
        spring.putConstraint(SpringLayout.WEST, intersectBox, 5,
                SpringLayout.EAST, searchLocation);
        spring.putConstraint(SpringLayout.NORTH, intersectBox, 5,
                SpringLayout.SOUTH, scroll);
        
        // |                  (scroll)
        // +-------------------------------
        //                                     
        // ...  intersectBox <<< passBox
        
        spring.putConstraint(SpringLayout.WEST, passBox, 5,
                SpringLayout.EAST, intersectBox);
        spring.putConstraint(SpringLayout.NORTH, passBox, 5,
                SpringLayout.SOUTH, scroll);
        
        // |                  (scroll)
        // +-------------------------------
        //                                     
        // ...  passBox <<< displayBox
        
        spring.putConstraint(SpringLayout.WEST, displayBox, 5,
                SpringLayout.EAST, passBox);
        spring.putConstraint(SpringLayout.NORTH, displayBox, 5,
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
     * @return returns an Image Icon
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
 * 
 * ...with a couple extra features  :)
 */
class ScrollablePicture extends JLabel implements Scrollable, 
                                                  MouseMotionListener{
	//XXX: Here is the path and location version numbers.  
	final static int PATH_VERSION_NUMBER = 1;
	final static int LOCATION_VERSION_NUMBER = 1;
	
    private int maxUnitIncrement = 5;
    private boolean missingPicture = false;
    
    // The current path that we're focusing on
    private Vector <Point>lines;
    
    // Vector of paths -- a vector where each element is a
    // vector of points (vector of points == path)
    private Vector <Vector<Point>>paths;
    
    //  Vector of locations (a location is basically point with a name)
    private Vector <Location>locations;

    //Index of where we are  in the paths array (paths vector).
    private int pathNumIndex = 0;
    
    //Index of where we are in the current path (lines vector)
    private int pointNumIndex = 0;
    
    // This should be a pointer to ShowImage to allow us to modify
    // fields such as the statusBar.
    private static ShowImage parent;
    
    // The name of the location that was last searched
    private String lastLocationSearched;
    
    // The index in the paths vector of the last used path out of
    // a searched locations.  
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
    
    /**
     * 
     * @param i The image to display
     * @param maxUnitPassed 
     * @param newParent
     */
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
        
        paths = new Vector<Vector<Point>>(128);
        locations = new Vector<Location>(128);

        paths.add( new Vector<Point>() );
        lines = paths.get(pathNumIndex);
        parent.statusBar.setText( statusBarText() );
        
        // save the curent 'this' for the inner class
        final ScrollablePicture thisParent = this;
        
        /* add the mouse-click handler */
        addMouseListener(new MouseAdapter(){
            
            public void mouseClicked(MouseEvent e){
                int x = e.getX();
                int y = e.getY();

                // Left click ==> create new point in the current path
                if(SwingUtilities.isLeftMouseButton(e)) {
                    createNewPointInCurPath(x, y);
                }
                // Right click.
                else if(SwingUtilities.isRightMouseButton(e)) {
                    // Change the coordinates of the currently selected
                    // point
                    changeCurSelectedPointCord(x, y);
                    
                }
                // If you use any other buttton on your mouse...
                else
                {
                    // Open menu that allows you to select a location to go
                    // to.  
                    middleClickGoToLocation();
                }
                
            }
        });

    }

    
    /**
     * This method creates a new point in the currently selected path
     * at the passed in (x,y) pair.  (Will also create a location
     * if text is entered in the text bar).  
     * @param x -- x coordinate to create the point at. 
     * @param y -- y coordinate to create the point at.
     */
    public void createNewPointInCurPath(int x, int y)
    {
        // store the click coordinates in a new Point object
        Point pointToAdd = new Point(x, y);

        // If the new point is going to be the first element in the current 
        // path, allocate a vector for the path before adding 
        // the Point to the path.  
        if(lines == null)
        {
            // Allocate new vector
            lines = new Vector<Point>();
        }
        
        //add the point to the current path
        lines.add(pointToAdd);
    
        // Set the element focus to the last element (the one just created)
        setPointNumIndex(true);
        
        // and redraw immediately to see the changes
        repaint( getVisibleRect() );
        
        // if the location field was filled, add a location
        if( !parent.locationNameEntry.getText().equals("") )
        {
            // Constructor call to create new location
            locations.add(new Location(x, y,
                    parent.locationNameEntry.getText(), parent));
        }
        
        // update the status bar
        parent.statusBar.setText( statusBarText() );
        
        // clear the location name entry field
        parent.locationNameEntry.setText("");
        
        //set focus (listeners) back onto the picture
        this.requestFocus();
    }

    /**
     * Method: void changeCurSelectedPointCord(int x, int y)
     * Change the coordinates of the currently selected point to the passed
     * in x-y coordinate.
     * 3 cases, in call of them we reasign the coordinates of the point
     * in the paths array to the passed in points and refresh the status
     * bar<p>
     * 1: Location exists at previous point, but no new location name was 
     * entered.  ==> Delete the previous point's location from the locations
     * vector
     * <p>
     * 2: Location exists at previous point, and a new location name was 
     * entered.  ==> replace the location corresponding with the previous
     * point with a new location based on the passed in points and the name
     * in the location name bar.
     * <p>
     * 3: No location exists at previous point, and a new location name was
     * entered. ==> Add a location to the locations vector based on the name
     * entered and the passed in points.  
     * 4: No location exists at previous point, and no new location name was 
     * entered.  ==> Do what we do for everything.  
     * @param x -- The x coordinate to change to.
     * @param y -- The y coordinate to change to.
     */
    public void changeCurSelectedPointCord(int x, int y)
    {
        //if the pathNumIndex is greater than it should be, exit from method.
        if( pathNumIndex >= lines.size() )
            return;
        
        // check if there's an existing point
        // findLocationAtPoint returns an index if found, -1 if not found.
        int locIndex = findLocationAtPoint(
                (Point)lines.get(pointNumIndex));
        
        //When we find a location at the point.  
        if(locIndex >= 0){
            // if the text field is empty, delete the location
            if(parent.locationNameEntry.getText().equals("")){
                locations.remove(locIndex);
            }
            // otherwise, we replace the location at this point
            // with another that has the new (x,y) coordinate and 
            // the name the user entered
            else{
                locations.set(locIndex, new Location(x, y,
                        parent.locationNameEntry.getText(), parent));

                parent.locationNameEntry.setText("");
                
                //set focus (listeners) back onto the picture
                this.requestFocus();
            }
           
        }
        // if we DIDN'T find a matching point, and the text
        // field isn't empty, create a new point
        // this looks very similar to the code above, but
        // it's not quite the same.
        else if(!parent.locationNameEntry.getText().equals("")){
            locations.add(new Location(x, y,
                    parent.locationNameEntry.getText(), parent));

            parent.locationNameEntry.setText("");
            
            //set focus (listeners) back onto the picture
            this.requestFocus();
        }
        
        // move the point
        ((Point)lines.get(pointNumIndex)).setLocation(x,y);
        
        // set statusbar text
        parent.statusBar.setText( statusBarText() );
        repaint();
    }

    /**
     * This method sets up a dialog box with a list of locations
     * on the map.  The user can click on one of the items in the list
     * and then a point is drawn to that location.  
     */
    public void middleClickGoToLocation()
    {
        // Index into the locationNames[] and locations vector
        int index; 
        
        // Create the dialog box...
        final JDialog dialog = new JDialog(parent, 
                "Place Point at location", true);
        
        // Set the layout of the dialog box...
        dialog.getContentPane().setLayout( new FlowLayout() );
        
        // Cancel button...
        JButton cancel = new JButton("Cancel");
        
        // Message
        final JLabel message = new JLabel("Choose a location:");

        // add the message to the dialog box...rest is added later...reason?
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
		JScrollPane scrollPane = new JScrollPane(locBox);

        // Add the list of locations and the cancel button to the dialog box.
        dialog.getContentPane().add(scrollPane);
        dialog.getContentPane().add(cancel);
        
        // Let's go packing!  You can't have a pack smaller than
        // what you're packing...
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
        // Make the dialog box visible (show it).
        dialog.setVisible(true);
    }
    
    /**
     * This method handles the key events.
     * The method is called by the anonymous KeyAdapter subclassed defined
     * in the constructor
     * We have events designated for F1-F10 + F12.  
     * @param k Key event to determine what key was pressed.  
     */
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
            // Call method to go to previous path
            goToPreviousPath();
        }
        // F3: Go to the next path (if exists)
        else if(c == KeyEvent.VK_F3)
        {
            goToNextPath();
        }
        // F4: Move backwards along elements in a path
        else if(c == KeyEvent.VK_F4){
            goToPreviousElement();
        }
        // F5: Move forwards along elements in a path
        else if(c == KeyEvent.VK_F5){
            goToNextElement();
        }
        // F6: Center on currently selected point
        else if(c == KeyEvent.VK_F6)
        {
            centerOnSelectedPoint();
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
            printLocationsToFile();
        }
        // F10: Manually place dialog
        else if(c == KeyEvent.VK_F10)
        {
            manualPlaceDialog();
        }
        // F11: Refresh statusBar
        else if(c == KeyEvent.VK_F11)
        {
            statusBarText();
        }
        // F12: Create new path
        else if(c == KeyEvent.VK_F12)
        {
            createNewPath();
        }
        // Take a wild gusss :)
        else{
            parent.statusBar.setText("Key does not have an action associated" +
                    "with it!");
        }
    }
    
    /**
     * Go back to the previous path in the paths vector. 
     * <br>
     * Ensure that this operation does not create an array out of bounds error.  
     * <br>
     * Assign the active element to be the last element in the path
     * <br>
     * Refresh the status bar
     */
    public void goToPreviousPath()
    {
        // Only go back if a paths exists
        if(pathNumIndex >= 1){
            // Set focus
            lines = paths.get(--pathNumIndex);
            // Automatically focus on the last element
            setPointNumIndex(true);
            // Set statusbar
            parent.statusBar.setText( statusBarText() );
            repaint();
        }
    }
    
    /**
     * Go back to the next path in the paths vector. 
     * <br>
     * Ensure that this operation does not create an array out of bounds error.  
     * <br>
     * Assign the active element to be the last element in the path
     * <br>
     * Refresh the status bar
     */
    public void goToNextPath()
    {
        // Only advance to the next path if it exists
        if(pathNumIndex < paths.size() - 1)
        {
            // Advance
            lines = paths.get(++pathNumIndex);
            // Automatically focus on the last element
            setPointNumIndex(true);
            // Set statusBar
            parent.statusBar.setText( statusBarText() );
            repaint();
        }
    }
    
    
    
    /**
     * Go back to the previous element in the current path
     * <br>
     * Ensure that this operation does not create an array out of bounds error.  
     * <br>
     * Refresh the status bar
     */
    public void goToPreviousElement()
    {
        //Ensure that we can go backwards
        if(pointNumIndex > 0)
        {
            // Decrement index
            pointNumIndex--;
            repaint();
            // Show status bar
            parent.statusBar.setText( statusBarText() );
        }
    }

    /**
     * Go back to the next element in the current path
     * <br>
     * Ensure that this operation does not create an array out of bounds error.  
     * <br>
     * Refresh the status bar
     */
    public void goToNextElement()
    {
        // only if pointNumIndex < max possible size
        if(pointNumIndex < lines.size() - 1)
        {
            // Increment
            pointNumIndex++;
            repaint();
            // Show status bar
            parent.statusBar.setText( statusBarText() );
        }
    }

    /**
     * Prints out the listing of all locations on the map to a text file
     * 
     */
    public void printLocationsToFile()
    {
        final String locTextHeader = "Locations:";
        final String locStatusBar = "Locations listing created";
        FileOutputStream textOutput;
        PrintStream outStream;
        
        try{
            textOutput =  new FileOutputStream(locationsTextFile);
            outStream = new PrintStream( textOutput );
            
            outStream.println(locTextHeader);
            
            // loop through each location and print its name and
            // (x,y) coordinates
            for(int locIndex = 0; locIndex < locations.size(); locIndex++)
            {
                outStream.println("Location " + (locIndex + 1) + " of " +
                        locations.size() + ": " + 
                        getLocation(locIndex).toString());
            }
            //Set the status bar for success
            parent.statusBar.setText(locStatusBar);
            //close the file stream
            outStream.close();
        }
        catch(Exception e){
            System.err.println("Error writing to file" + locationsTextFile);
        }
    }
    
    /**
     * Creates a new path and adds it to the paths array.  
     *
     */
    public void createNewPath()
    {
        // The current pathNumIndex is the sizeof the vector before
        // The new element is created.
        pathNumIndex = paths.size();
        // Create the space for the new path.
        paths.add( new Vector<Point>() );
        // Set focus
        lines = paths.get(pathNumIndex);

        //Start at the 0th point.  
        pointNumIndex = 0;
        // Status bar
        parent.statusBar.setText( statusBarText() );
        // dance, dance, dance!
        repaint();
    }
    
    /**
     * Centers the visible window to the currently selected (in focus) point.
     */
    public void centerOnSelectedPoint()
    {
        // Only center if pointNumIndex in range
        if(pointNumIndex <= lines.size() - 1)
        {
            //Get a pointer to the Point that we want to center too
            Point center = ((Point)lines.get(pointNumIndex));
            /*
             * Calculate the rectangle we want to center to
             * Use the getVisibleRect() function to get the size of the
             * current window and use them as an offset to get the
             * window to scroll to.  
             */
            Rectangle r = new Rectangle(
                    center.x - (getVisibleRect().width)/2, center.y - 
                    (getVisibleRect().height)/2, getVisibleRect().width, 
                    getVisibleRect().height);
            
            // Scroll to the newly created rectangle
            scrollRectToVisible(r);
        }
    }
    /**
     * Write data to disk.
     * @param pathFileName The passed in name for the raw path data file.  
     * @param locFileName The passed in name for the raw location data file.
     * @param pathsToWrite The paths vector to write
     * @param locationsToWrite the locations vector to write
     */
    public static void writeData(String pathFileName, String locFileName,
			Vector <Vector<Point>> pathsToWrite, 
			Vector <Location> locationsToWrite){
		
        // Booleans to tell if the writing of both path and location data
        // was a success.
        boolean pathWriteSuccess = false;
        boolean locationWriteSuccess = false;

        // Define error strings
        // File not found errors
        final String pathNotFound = 
            "File \"" + pathFileName + "\" not found!     ";
        final String locNotFound = "File \"" + locFileName + "\" not found!";
        
        // IO errors
        final String PATH_IO_ERROR = 
            "IO Error in writing \"" + pathFileName + "\"!";
        
		final String LOCATION_IO_ERROR = 
			"Error in writing \"" + locFileName + "\"!";
		
		// Status bar messages on success or fail...
		final String WRITE_SUCCESS = "Paths and locations written to file";
		final String WRITE_FAIL = 
			"Path and location writing failed...see stderr";
		
        // Define output files for paths and locations.
        File pathOutputFile = new File(pathFileName);
        File locationOutputFile = new File(locFileName);

		
		
        // Write out the paths vector...
        try{
            // Open stream
            ObjectOutputStream pathout = new ObjectOutputStream(
                    new FileOutputStream(pathOutputFile));

			pathout.writeInt(PATH_VERSION_NUMBER);
			
            // Write out the object...
            pathout.writeObject(pathsToWrite);

            // Close stream
            pathout.close();
            // If we get to this point, we were successful
            pathWriteSuccess = true;
        }
        // Catch exceptions...pathWriteSuccess will not change value,
        // so it will stay false.
        catch(FileNotFoundException e){
            System.err.println(pathNotFound);
            parent.statusBar.setText(pathNotFound);
        }
        catch(IOException e){
            System.err.println(PATH_IO_ERROR);
        }

        // Write out the locations vector...
        try{
            //Open stream
            ObjectOutputStream locout = new ObjectOutputStream(
                    new FileOutputStream(locationOutputFile));
            
			// Write out the locations version number
			locout.writeInt(LOCATION_VERSION_NUMBER);
			
			// print out the size of the location Vector
			locout.writeInt(locationsToWrite.size());
			
			// write out the static ID variable...IDcount
			locout.writeInt(Location.IDcount);
			
			// For each location in the location Vector
			for (Location loc : locationsToWrite) {
				locout.writeBoolean(loc.isAllowIntersections());
				locout.writeBoolean(loc.isCanPassThrough());
				locout.writeObject(loc.cord);
				locout.writeBoolean(loc.isDisplayName());
				locout.writeInt(loc.ID);
				locout.writeObject(loc.name);
			}
            
            // Close stream
            locout.close();
            
            // If we get to this point, the writting was successful, so mark.
            locationWriteSuccess = true;
        }
        catch(FileNotFoundException e){
            System.err.println(locNotFound);
            parent.statusBar.setText(parent.statusBar.getText() + locNotFound);
        }
        catch(IOException e){
            System.err.println(LOCATION_IO_ERROR);
        }
        if(locationWriteSuccess && pathWriteSuccess){
            // Set status
            parent.statusBar.setText(WRITE_SUCCESS);
        }
        else
		{
			parent.statusBar.setText(WRITE_FAIL);
		}
    }
    
    /**
     * This method deletes the current link.
     */
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
     * @param pathFileName The passed in name for the raw path data file.  
     * @param locFileName The passed in name for the raw location data file.
     */
    public void readData(String pathFileName, String locFileName){
        
		// Booleans to determin if the reading of paths and locations were
		// successful
        boolean pathLoadSuccess = false;
        boolean locationLoadSuccess = false;

		// Error strings...
		// Files could not be found...
        final String PATHS_FILE_NOT_FOUND = 
            "File \"" + pathFileName + "\" not found!     ";
        final String LOC_FILE_NOT_FOUND = 
			"File \"" + locFileName + "\" not found!";

		// IO Errors occured in attempting to read
		final String PATHS_IO_ERROR = 
			"General Exception in \"" + pathFileName + "\"!";
		final String LOCATION_IO_ERROR =
			"General Exception in \"" + locFileName + "\"!";
		
		final String ERROR_READING_DATA = "Error occured in reading data!";
		
		final String INPUT_READ_SUCCESS = "Input read from file";
		
        // Define input files for paths and locations.
        File pathsInputFile = new File(pathFileName);
        File locationsInputFile = new File(locFileName);

		// Get the paths vector
        try{
			int pathVersionNumber;
            // Open stream
            ObjectInputStream pathin = new ObjectInputStream(
                    new FileInputStream(pathsInputFile));
			
			pathVersionNumber = pathin.readInt();
			
			if(pathVersionNumber == 1)
			{
	            // Read in the vector of paths...causes Java 5 warning
	            paths = (Vector)pathin.readObject();
			}
			else
			{
				System.err.println(
					"No method for reading pathVersion number = " 
						+ pathVersionNumber);
			}
			//close stream
            pathin.close();

			// If we get to this point, we were successful in reading the 
			// path
			pathLoadSuccess = true;
        }
        catch(FileNotFoundException e){
            System.err.println(PATHS_FILE_NOT_FOUND);
            parent.statusBar.setText(PATHS_FILE_NOT_FOUND);
        }
        catch(Exception e){
			// Indicate that an IO error occured.  
            System.err.println(PATHS_IO_ERROR);
        }
		
		
		
		/*
			// print out the size of the location Vector
			locout.writeInt(locations.size());
			
			// write out the static ID variable...IDcount
			locout.writeInt(Location.IDcount);
			
			
		 */
		
		//Get the locations vector
	    try{
	        // Open stream
	        ObjectInputStream locin = new ObjectInputStream(
	                new FileInputStream(locationsInputFile));
			
			int locVersionNumber = locin.readInt();
			
			if(locVersionNumber == 1)
			{
				int numLocations = locin.readInt();
				
				Location.IDcount = locin.readInt();

				Location tempLocation;
				boolean tempIntersect;
				boolean tempCanPass;
				Point tempCord;
				boolean tempDisplayName;
				int tempID;
				String tempName;
				locations.clear();
				for(int locNum = 0; locNum < numLocations; locNum++)
				{
					tempIntersect = locin.readBoolean();
					tempCanPass = locin.readBoolean();
					tempCord = (Point)locin.readObject();
					tempDisplayName = locin.readBoolean();
					tempID = locin.readInt();
					tempName = (String)locin.readObject();

					//public Location(int x, int y, String passedName, 
					// ShowImage parent)
					tempLocation = new Location(tempCord.x, tempCord.y, 
							tempName, parent);
					locations.add(locNum, tempLocation);
					tempLocation.setAllowIntersections(tempIntersect);
					tempLocation.setCanPassThrough(tempCanPass);
					tempLocation.setDisplayName(tempDisplayName);
					tempLocation.ID = tempID;
				}
			}
			else
			{
				System.err.println("We don't have a read method for version" +
						"locVersionNumber");
			}
			locationLoadSuccess = true;
	        //close stream
	        locin.close();
            
	    }
	    catch(FileNotFoundException e){
	    	System.err.println(LOC_FILE_NOT_FOUND);
	    	parent.statusBar.setText(parent.statusBar.getText() 
					+ LOC_FILE_NOT_FOUND);
	    }
	    catch(Exception e){
	    	System.err.println(LOCATION_IO_ERROR);
	    }

		// If both reading of locations and paths was successful...
        if( locationLoadSuccess && pathLoadSuccess ){

            // set the starting point for the IDS of any new locations that
            // are created.
            Location.IDcount += locations.size();
            
            // now we do a little housekeeping: check for collisions between
            // location IDs
            for(int i = 0; i < locations.size(); i++){
                for(int j = 0; j < locations.size(); j++){
                    if(i == j) continue;
                    // if the IDs of two of the locations are the same...
                    if(getLocation(i).ID == getLocation(j).ID){
                        // reassign one of them
                        getLocation(i).ID = Location.IDcount++;
                        System.err.println("Location ID collision! " +
                        		"(Bad input data.)" + " Reassigning to " + 
                        		getLocation(i).ID);
                    }
                }
            }
            
            // Set status bar
            parent.statusBar.setText(INPUT_READ_SUCCESS);
    
            // Set the active path (also termed path in "focus")...
            
            // If the current pathNumIndex (path focus) is greater 
            // than the size of the paths array that was just read in...
            // Then attach focus to the last path in the paths array.
            if(pathNumIndex > paths.size() - 1)
                pathNumIndex = paths.size();
            
            //Set lines
            lines = paths.get(pathNumIndex);
            
            // Do the repaint dance...woooo!
            repaint();
        }
        else
		{
			parent.statusBar.setText(ERROR_READING_DATA);
		}
    }


    /**
     * Set the pointNumber index.  <p>
     * 
     * If true is passed in, the function will set the pointNumIndex
     * to the last element in the currently selected path.  <br>
     * If false is pass in, the function will ensure that the
     * point is either at the first element or the last element in the 
     * currently selected path.
     * @param setAtEndPoint Determination of job to do.  
     *
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
     * Opens up a dialog box for the user to select how to save the data
     * the data that has been entered.  
     *
     */
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
         * Make a call to PathOptimize to have it run an optimized on
         * the raw in the current raw data files.  Outputs binary files also.
         */
        createOpt.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                dialog.setVisible(false);
                dialog.dispose();
                
                //PathOptimize pathOp = new PathOptimize();
                PathOptimize.run(paths, locations, 
                        optPathFile, optLocFile,
                        binaryPoints, binaryLocations, binaryEdges
                );
            }
        });
        
        /**
         * If the write raw data button is clicked,
         * Write out the raw data.  
         */
        writeRaw.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                dialog.setVisible(false);
                dialog.dispose();
                writeData(rawPathFile, rawLocFile, paths, locations);
            }
        });
        
        // Show dialog box.  
        dialog.setVisible(true);
    }
    
    /**
     * Pops up a dialog box that asks what type of datafile the user wants
     * to load.  Takes the input and calls the functions to load the proper
     * data files.  (Raw data or optimized data).  
     */
    public void selectRead()
    {
        final String dialogBoxTitle = "Loading data options";
        final String readRawButton  = "Load Raw Data";
        final String readOptButton  = "Load optimized data output";
        final String cancelButton   = "Cancel";

        // Create new JDialog
        final JDialog dialog = new JDialog(parent, dialogBoxTitle, true);
        
        // Let's make it easy and just use FlowLayout.
        dialog.getContentPane().setLayout( new FlowLayout() );
        
        // Create the JButtons, the reading options.
        JButton readRaw = new JButton(readRawButton);
        JButton readOptimized = new JButton(readOptButton);
        JButton cancel = new JButton(cancelButton);
        
        // add all the interface elements to the dialog box
        dialog.getContentPane().add(cancel);
        dialog.getContentPane().add(readOptimized);
        dialog.getContentPane().add(readRaw);
        
        // Fit subcomponents
        dialog.pack();

        // What to do if cancel is pressed
        cancel.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                final String canceledMessage = "Reading of data canceled!";
                dialog.setVisible(false);
                dialog.dispose();
                parent.statusBar.setText(canceledMessage);
            }
        });
        
        // What to do if the read raw button is pressed
        readRaw.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                dialog.setVisible(false);
                dialog.dispose();
                // Read data from the raw data files.  
                readData(rawPathFile, rawLocFile);
            }
        });
        
        // What to do if the read optimized button is pressed
        readOptimized.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                dialog.setVisible(false);
                dialog.dispose();
                // Read data from the optimized files.  
                readData(optPathFile, optLocFile);
            }
        });

        // place dialog box
        dialog.setVisible(true);
    }
    
    /**
     * Display a dialog that allows the user to place a point (node) at a
     * specified & entered x/y coordinate. If nothing (or an invalid integer)
     * is entered in either field, that coordinate remains unchanged.
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
                
                // the new X and Y coordinates, wherever they may be
                // (they might even be the current coords)
                int newX, newY;
                Point newPoint;  // The point to be added.
                
                // try to set the X value to what the user entered.
                try
                {
                    newX = Integer.parseInt(xinput.getText());            
                }
                // if we can't, use the current value
                catch(NumberFormatException numException)
                {
                    newX = lines.get(pointNumIndex).x;
                }
                
                // try to set the Y value to what the user entered.
                try
                {
                    newY = Integer.parseInt(yinput.getText());          
                }
                // if we can't, use the current value
                catch(NumberFormatException numException)
                {
                    newY = lines.get(pointNumIndex).y;
                }
                
                // change the location of the current point, and do all the
                // associated mangling of locations, and whatnot
                changeCurSelectedPointCord(newX, newY);
            }
        });
        
        dialog.setVisible(true);
    }
    
    
    /**
     * Return the status bar text
     * Write: Current path number in focus, number of elements in current
     *        path, number of paths, and any location string associated
     *        with the current in focus point.  
     * @return The statusbar string to display
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
     * This method is used to by setStatusBar.  
     * @return name of location if exist, else null string.
     */
    public String printCurrentLocation () {
        // Only print if locations exists and there are points
        // on the currently selected path.  (Null exception guards)
        String stringToReturn = "";
        if( locations.size() > 0 && lines.size() > 0)
        {
            int locIndex=findLocationAtPoint((Point)lines.get(pointNumIndex));
            // If a valid index was found for in the locations array...
            if (locIndex >= 0)
            {
                stringToReturn = ", Locations: ";
                Location locToPrint = getLocation(locIndex);
                
                stringToReturn += locToPrint.name;
                
                if(locToPrint.isAllowIntersections())
                {
                    stringToReturn += "--> intersect: true, ";
                    parent.intersectBox.setSelected(true);
                }
                else
                {
                    stringToReturn += "--> intersect: false, ";
                    parent.intersectBox.setSelected(false);
                }
                
                if(locToPrint.isCanPassThrough())
                {
                    stringToReturn += "pass: true, ";
                    parent.passBox.setSelected(true);
                }
                else
                {
                    stringToReturn += "pass: false, ";
                    parent.passBox.setSelected(false);
                }
                
                if(locToPrint.isDisplayName())
                {
                    stringToReturn += " displayable: true.";
                    parent.displayBox.setSelected(true);
                }
                else
                {
                    stringToReturn += " displayable: false.";
                    parent.displayBox.setSelected(false);
                }
    
                return(stringToReturn);
            }
            // The status we are attempting to refresh is not a location
            else
            {
                // Set select boxes to default values
                parent.intersectBox.setSelected(true);
                parent.displayBox.setSelected(true);
                parent.passBox.setSelected(true);
            }
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
     * Returns the number of paths connect to a given point.
     * <br>This method is used to determine if the location connected
     * to a point should be deleted when a undo command is executed.
     * <br>Note: Yes, it's really inefficient, but it's the best we
     * can do with the limited structure of class ShowImage.  
     * @param pointToCompare The point to check for connected paths.  
     * @return The number of paths connected to the point.  
     */
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
    
    /**
     * Searches for a location and changes current focus to a path
     * that intersects with the location.  <br>
     * If there is multiple paths intersecting a location, 
     * calling this method again will cycle through the paths.  
     * @param locationName The location name to search for.  
     */
    public void searchAndFocusToLocation(String locationName)
    {
        // Message to display on successful completion
        String success = "Path #" + (pathNumIndex + 1) +
            " intersects with location \"" + locationName + "\"";
        int locIndex;    // The index of the location inside of the
                        // locations array.  

        // we're searching for a path intersecting with the locataion.
        
        // Find a location with the name passed in...we will assume
        // that no more than one location with the same name exists
        locIndex = findLocationWithName(locationName);

        // Return if location could not be found.  
        if(locIndex < 0)
            return;

        // Get the actual location object associated with the name.  
        Location locFound = (Location)locations.get(locIndex);

        
         //If the location passed in is equal to the last 
        //location passed in...
        if(locationName.equals(lastLocationSearched))
        {    
            // We have a location (a point), we need to find the next path
            // (after the last one), so pass that information in.  
            lastVertex = getNextPathInSearch(++lastVertex, locFound.cord);
        }
        else
            // We have a location (a point), now we need to find a path
            // that it's on. 
            lastVertex = getNextPathInSearch(0, locFound.cord);
        
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
        
        //If we didn't return, then lastVertex is the index of that
        // path we should focus to.  
        pathNumIndex = lastVertex;
        
        // Set current path
        lines = paths.get(pathNumIndex);
        
        
        //Set point number index to the end.  
        setPointNumIndex(true);
        
        //Do the repaint dance!
        repaint();

        //Hold the location searched for the next time the function is called
        lastLocationSearched = locationName;
        
        // Set the status bar to the successful completion message
        parent.statusBar.setText(success);
    }
    
    /**
     * Searches for a point inside of all the paths
     * @param startingIndex Which path index to start from.  
     * @param pointToFind The point that we want to match to
     * @return The index of the path (index in the paths vector)
     */
    public int getNextPathInSearch(int startingIndex, Point pointToFind)
    {
        final String allListed = 
            "All intersecting paths have been previously listed";

        // step over all remaining paths
        for(int i = startingIndex; i < paths.size(); i++)
            // step through every point in the path
            for(int j = 0; j < ((Vector)paths.get(i)).size(); j++)
                // return the path index if a match is found
                if(getPointInPath(i,j).equals(pointToFind))
                    return(i);
        
        // Return an error & output a message if path couldn't be found.  
        parent.statusBar.setText(allListed);
        return(-1);
        
    }
    
    /**
     * Return in the point in a path specified by the two indexes.  
     * @param pathIndex That path to choose
     * @param elementIndex The element in the path to choose
     * @return A choosen point in a choosen path.  
     */
    public Point getPointInPath(int pathIndex, int elementIndex)
    {
        // cast away!
        return ( (paths.get(pathIndex)).get(elementIndex) );
    }
    
    
    /**
     * Scans through locations array looking for a location that has
     * the passed in location name.  
     * @param locationName The name to compare with the locations.  
     * @return The index in the locations array where the location
     * with the matching name was found.  
     */
    public int findLocationWithName (String locationName)
    {
        for(int i = 0; i < locations.size(); i++)
            if(((Location)locations.get(i)).name.equals(locationName))
                return(i);
        parent.statusBar.setText("Corresponding location could" +
            " not be found!");
        return(-1);        
    }
    
    
    /**
     * Print the (x,y) coordinates of the current point.<br>
     * Prints out non-null string only if the size of the current
     * in focus path is greater than zero. 
     * @return string as described, else empty string.
     */
    public String printCurrentPoint (){
        // Print out point only if the size is greater than 0 &
        // The pointNumIndex is greater than zero.  
        if( lines.size() > 0)
        {
            return(",  @ (" + ((Point)lines.get( pointNumIndex )).x + 
                    ", " + ((Point)lines.get( pointNumIndex )).y + ")");
        }
        return("");
    }
    //Methods required by the MouseMotionListener interface:
    /**
     * Required method of the MouseMotionListener interface.
     * @param e It's a mouse event!  Pass the cheese!
     */
    public void mouseMoved(MouseEvent e) { }

    /**
     * Required method of the MouseMotionListener interface.
     * Refocuses the window.
     * @param e It's a mouse event!  Pass the cheese!
     */
    public void mouseDragged(MouseEvent e) {
        //The user is dragging us, so scroll!
        Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
        scrollRectToVisible(r);
    }

    /**
     * @return The prefered dimension to display
     */
    public Dimension getPreferredSize() {
        if (missingPicture) {
            return ( new Dimension(320, 480) );
        } 
        // Otherwise, return
        return ( super.getPreferredSize() );
    }

    /**
     * @return The prefered dimension to display
     */
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    /**
     * @param visibleRect Ignored
     * @param orientation Ignored
     * @param direction Ignored
     * @return the maximum unit increment
     */
    public int getScrollableUnitIncrement(Rectangle visibleRect,
                                          int orientation,
                                          int direction) {
        return maxUnitIncrement;
    }

    /**
     * @param visibleRect Ignored
     * @param orientation Ignored
     * @param direction Ignored
     * @return the maximum unit increment
     */
    public int getScrollableBlockIncrement(Rectangle visibleRect,
                                           int orientation,
                                           int direction) {
        return maxUnitIncrement;
    }

    /**
     * @return false always
     */
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    /**
     * @return false always
     */
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    /**
     * Set the maxUnitIncrement using passed in value.
     * @param pixels the passed in value.
     */  
    public void setMaxUnitIncrement(int pixels) {
        maxUnitIncrement = pixels;
    }

    /**
     * Override the paintComponent method so we can draw lines
     * Draws all of the paths in memory according to the 
     * paths field (Vector).  Color codes the paths according to
     * their current focus.  
     * @param g The graphics component
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
                cur = ( (paths.get(pathsIndex)).get(pointInPath) );
                
                
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


    /**
     * Method created to eliminate the ugly casting.  Returns a location
     * at the passed in locations field index.  
     * @param locIndex The index to choose
     * @return The location @ that index.  
     */
    public Location getLocation(int locIndex){
        return((Location)locations.get(locIndex));
    }
    
    /**
     * Draw a dot on the specified Graphics object at the specified Point
     * Fills a rectangle around the point, paints according to the Graphic
     * component.
     * @param g The graphics component (will specify color)
     * @param p The point to draw the dot at.  
     **/
    public void drawDot(Graphics g, Point p){
            g.fillRect( (int)p.getX()-2, (int)p.getY()-2, 5, 5 );
    }

    /**
     * Connect the two passed in points. 
     * This is really just a call to drawLine()
     * Paints according to the passed in Graphics object.  
     * @param g The graphics component (will specify color)
     * @param start Line endpoint
     * @param end Line endpoint
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
    /**
     * ID number of the location object, used for binary file output
     */
    public int ID;
    /**
     * (x,y) coordinate of the location
     */
    public Point cord;
    /**
     * The name associated with the location
     */
    public String name;
    
    // Determines if we can use this point when linking together sets of
    // paths
    private boolean canPassThrough;
    
    // Determines if we can allow intersections.  
    private boolean allowIntersections;
    
    // Determines if we should display the name of the location
    private boolean displayName;
    
    // For creating binary file IDs
    static int IDcount = 1;
    
    /**
     * @return Returns the allowIntersections.
     */
    public boolean isAllowIntersections() {
        return allowIntersections;
    }
    /**
     * @param allowIntersections The allowIntersections to set.
     */
    public void setAllowIntersections(boolean allowIntersections) {
        this.allowIntersections = allowIntersections;
    }
    /**
     * @return Returns the canpassThrough.
     */
    public boolean isCanPassThrough() {
        return canPassThrough;
    }
    /**
     * @param canpassThrough The canpassThrough to set.
     */
    public void setCanPassThrough(boolean canpassThrough) {
        this.canPassThrough = canpassThrough;
    }
    /**
     * @return Returns the displayName.
     */
    public boolean isDisplayName() {
        return displayName;
    }
    /**
     * @param displayName The displayName to set.
     */
    public void setDisplayName(boolean displayName) {
        this.displayName = displayName;
    }
    /**
     * Generic location constructor.  Also assigns the ID# of the location
     * @param x x coordinate for the location's Point field
     * @param y y coordinate for the location's Point field
     * @param passedName The name of the location
     * @param parent Allows for access to checkboxes.  
     */
    public Location(int x, int y, String passedName, ShowImage parent)
    {
        cord = new Point(x,y);  // Create coordinate based on passed in values
        name = passedName;      // Copy name string (Strings are constants!)
        ID = IDcount++;         // Get the current ID and increment

        // Determines if we can use this point when linking together sets of
        // paths
        if(parent.passBox.isSelected())
            canPassThrough = true;
        else
            canPassThrough = false;
        
        // Determines if we can allow intersections.
        if(parent.intersectBox.isSelected())
            allowIntersections = true;
        else
            allowIntersections = false;

        // Determines if we should display the name of the location
        if(parent.displayBox.isSelected())
            displayName = true;
        else
            displayName = false;
    
    }
    
    /**
     * Overloaded toString method for outputing the name and coordinates of
     * a location.  Used in painting the coordinates to the screen
     * and in outputing the list of locations to a text file
     * @return Name and coordinates in a nice format.  
     */
    public String toString()
    {
        return(name + " @ (" + cord.x + ", " + cord.y + ")");
    }
}