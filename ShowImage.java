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
	final static long serialVersionUID = 47;  //huh?
	private ImageIcon img;
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
    
    // path manipulation
	private JMenuItem prevPath, nextPath, newPath, 
		deletePath, iteratePaths, goToPath;
    
    // element manipulation
	private JMenuItem undoConnection, manualPlace, nextElement, 
		prevElement, centerOnElement;
	
	//IO
	private JMenuItem read, write, writeOptimize, readOptimize, 
		createLocationFile, changeImage, loadDefinedLocations, scaleData,
        clearPathsAndLocations, quit;
	
	// Locations
    private JMenuItem locationEditor, editLocation, createLocation, 
    	selectLocation;
	
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
		int confirmReturn = JOptionPane.showConfirmDialog(s, 
				"Load data from XML files?", 
				"Read raw data XML files", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if(confirmReturn == JOptionPane.YES_OPTION)
		{
            s.ipanel.setStatusBarText("Loading data...");
			s.ipanel.loadXMLLocations(ScrollablePicture.XMLLocFile);
			s.ipanel.loadXMLPaths(ScrollablePicture.XMLPathFile);
			s.ipanel.setStatusBarText("Raw data loaded.");
			//XXX: warning needs to go here too!
		}
	}
	
	/**
	 * This method may or may not work properly, it was just a test.
	 * This isn't the method you're looking for...*waves hand*
	 */
	public void changeImage()
	{
		JFileChooser chooser = new JFileChooser();
		int returnVal = chooser.showOpenDialog(this);
		System.err.println("Returnval = " + returnVal);
		if(returnVal == JFileChooser.APPROVE_OPTION)
			System.out.println("You chose to open this file: " +
					chooser.getSelectedFile().getName());
		ImageIcon icon = ShowImage.createImageIcon(chooser.getSelectedFile().getName()); 
		ipanel.setIcon(icon);
	}
	
	
	/**
	 * This is the mouse listener for the menu items.
	 * It simply uses it's actionPerformed method to determine
	 * which menu was clicked and then call the appropriate method
	 * to handle the event.  
	 * @author David Lindquist
	 */
	class MenuListener implements ActionListener
	{
		/**
		 * Passes on the task based on which menu was clicked.
		 */
		public void actionPerformed(ActionEvent e)
		{
            Object src = e.getSource();
            
            //XXX: This is UGLY. There MUST be a better way to do this.
			if(src == read)
				ipanel.readXMLDialog();
			
			if(src == write)
				ipanel.writeXMLDialog();
	
			if(src == scaleData)
				ipanel.scaleData();
			
			if(src == prevPath)
				ipanel.goToPreviousPath();
			
			if(src == nextPath)
				ipanel.goToNextPath();
			
			if(src == undoConnection)
				ipanel.removeCurPoint();
			
			if(src == nextElement)
				ipanel.goToNextElement();
			
			if(src == prevElement)
				ipanel.goToPreviousElement();
			
			if(src == centerOnElement)
				ipanel.centerOnSelectedPoint();
			
			if(src == createLocationFile)
				ipanel.printLocationsToFile();
			
			if(src == manualPlace)
				ipanel.manualPlaceDialog();
			
			if(src == locationEditor)
				ipanel.locationEditor();
			
			if(src == newPath)
				ipanel.createNewPath();
			
			if(src == deletePath)
				ipanel.deleteCurPath();
			
			if(src == goToPath)
				ipanel.goToPath();
			
			if(src == changeImage)
				changeImage(); //Do change image stuff here
			
			if(src == editLocation)
				ipanel.editLocation();
			
			if(src == createLocation)
				ipanel.createLocation();

			if(src == selectLocation)
				ipanel.selectLocation(
                        ScrollablePicture.SelectLocationJobID.CENTER);

			if(src == iteratePaths)
				ipanel.iterateThroughPathsAtPoint(ipanel.getCurrentPoint());
			
			if(src == loadDefinedLocations)
				ipanel.readCustomLocationInformation();
			
			if(src == writeOptimize)
				ipanel.writeOptimize();
			
			if(src == readOptimize)
				ipanel.readOptimize();
            
            if(src == clearPathsAndLocations)
                ipanel.clearPathsAndLocations();
            
            if(src == quit)
                quit();
            
		}
	}
	
	/**
	 * Overrided method, reduces the number of parameters needed
	 * for the call.  
	 * @param name The name to call the menu
	 * @param listener The listener to assign to the menu
	 * @param keyCode The key code to use as a shortcut to the menu.
	 * @return The result of calling the real makeJMenuItem method.
	 */
	public JMenuItem makeJMenuItem(String name, ActionListener listener, 
			int keyCode)
	{
		return makeJMenuItem(name, listener, keyCode, keyCode, 
				KeyEvent.CTRL_MASK);
	}
	
	/**
	 * Creates a new JMenu item binded to the alias of the keyEvent.
	 * Adds the action listener, and the mod + 
	 * @param name Name for the menu
	 * @param listener The menu's listener for events
	 * @param keyEvent The alias key for the menu
	 * @param accel ?
	 * @param accel_mod ?
	 * @return The created menu.
	 */
	public JMenuItem makeJMenuItem(String name, ActionListener listener, 
			int keyEvent, int accel, int accel_mod)
	{
		JMenuItem newMenu = new JMenuItem(name, keyEvent);
		newMenu.addActionListener(listener);
		if(accel > 0 && accel_mod > 0)
			newMenu.setAccelerator(KeyStroke.getKeyStroke(accel, accel_mod));
		return(newMenu);
	}
	
	/**
	 * Create a new window holding the specified image.
	 * @param filename filename of image to open
	 */
	public ShowImage(String filename){
		
		super("UCSDMap Editor: " + filename);  // JFrame
		
		// USED:
		// A B C D E F G H I J K L M N O P Q R S T U V W X Y Z + -
		//   Y Y Y   Y Y   Y   Y Y Y Y Y Y Y   Y Y             Y Y
		// IO
		final int READ_KEY 				= KeyEvent.VK_O; 
		final int WRITE_KEY 			= KeyEvent.VK_S;
		final int LOCATION_FILE_KEY 	= KeyEvent.VK_P;
        final int QUIT_KEY              = KeyEvent.VK_Q;
		// Path
		final int NEW_PATH_KEY 			= KeyEvent.VK_N;
		final int DELETE_PATH_KEY		= KeyEvent.VK_D;
		// Element
		final int NEXT_ELEMENT_KEY 		= KeyEvent.VK_F;
		final int PREV_ELEMENT_KEY 		= KeyEvent.VK_B;
		final int UNDO_CONNECTION_KEY 	= KeyEvent.VK_K;
		final int CENTER_KEY 			= KeyEvent.VK_C;
		final int MANUAL_PLACE_KEY		= KeyEvent.VK_M;
		// Locations
		final int EDIT_LOCATION			= KeyEvent.VK_T;
		final int CREATE_LOCATION		= KeyEvent.VK_L;
		final int SELECT_LOCATION		= KeyEvent.VK_G;
		final int ITERATE_PATHS			= KeyEvent.VK_I;


		/** Setup the pretty menu bar!  **/
		MenuListener listener = new MenuListener();
		JMenuBar bar = new JMenuBar();
		
		/** Menu associated with file I/O options **/
		JMenu file = new JMenu("File");

		read = file.add(makeJMenuItem("Open files...", listener, READ_KEY));
		write = file.add(makeJMenuItem("Save files...", listener, WRITE_KEY));
        file.addSeparator();
		writeOptimize = file.add(makeJMenuItem("Optimize Data...", listener, 0));
		readOptimize = file.add(makeJMenuItem("View Optimized Data...", listener, 0));
        file.addSeparator();
        loadDefinedLocations = 
            file.add(makeJMenuItem("Import Locations", listener, 0));
		createLocationFile = file.add(makeJMenuItem(
				"Print Location List", listener, LOCATION_FILE_KEY));
		changeImage = file.add(makeJMenuItem("Change Image...", listener, 0));
		
		scaleData = file.add(makeJMenuItem("Scale Data...", listener, 0));
        file.addSeparator();
        clearPathsAndLocations = file.add(
                makeJMenuItem("Clear Paths And Locations...", listener, 0));
        file.addSeparator();
        quit = file.add(makeJMenuItem("Quit", listener, QUIT_KEY));
        
		/** Menu associated with path options **/
		JMenu path = new JMenu("Paths");
		// Path Manipulation path
		newPath = path.add(makeJMenuItem("New Path", 
				listener, NEW_PATH_KEY));
		path.addSeparator();
		prevPath = path.add(makeJMenuItem("Previous Path (-)", 
				listener, 0));
		nextPath = path.add(makeJMenuItem("Next Path (+)", 
				listener, 0));
		goToPath = path.add(makeJMenuItem("Go To Path...", listener, 0));
		iteratePaths = path.add(
                makeJMenuItem("Iterate Through Paths On Current Point",
                        listener, ITERATE_PATHS));
		path.addSeparator();
		deletePath = path.add(makeJMenuItem("Delete Current Path...", 
				listener, DELETE_PATH_KEY));;
		
		
		/** Menu associated with element options **/
		JMenu element = new JMenu("Points");
		
		prevElement = element.add(makeJMenuItem("Previous Point", 
				listener, PREV_ELEMENT_KEY));
		nextElement = element.add(makeJMenuItem("Previous Point", 
				listener, NEXT_ELEMENT_KEY));
		centerOnElement = element.add(
				makeJMenuItem("Center On Selected Point", 
						listener, CENTER_KEY));
		element.addSeparator();
		undoConnection = element.add(makeJMenuItem("Delete Current Point", 
				listener, UNDO_CONNECTION_KEY));
		manualPlace = element.add(makeJMenuItem("Manually Place Point...", 
				listener, MANUAL_PLACE_KEY));
		
		/** Menu associated with locations **/
		JMenu location = new JMenu("Locations");
		locationEditor = location.add(makeJMenuItem("Locations Editor...", 
				listener, 0));
		editLocation = location.add(makeJMenuItem("Edit Current Location...", 
				listener, EDIT_LOCATION));
		selectLocation = location.add(makeJMenuItem(
				"Go to location...", listener, SELECT_LOCATION));
		createLocation = location.add(makeJMenuItem(
				"New Location At Current Point...", 
				listener, CREATE_LOCATION));
		
		/** Add the menus **/
		bar.add(file);
		bar.add(path);
		bar.add(element);
		bar.add(location);
		
		/** Add the bar **/
		setJMenuBar(bar);
		
		
		// Set background color
		this.setBackground(Color.LIGHT_GRAY);
		
		Container pane = this.getContentPane();
		
		// don't immediately exit on close, but rather call this handler
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
                quit();
			}
		});
		
		// use a SpringLayout
		SpringLayout spring = new SpringLayout();
		pane.setLayout( spring );
		
		// img: private ImageIcon
		img = createImageIcon(filename);
		
		// create the other objects in the main window
		// this text is overridden in the ScrollablePicture constructor
		statusBar = new JLabel("Status Text");
		
		// See class listing below
		// Pass in ImageIcon, and maxUnitIncrement (10).
		ipanel = new ScrollablePicture(img, 10, this);
		
		// Create the main window: a scroll pane with the new image panel
		JScrollPane scroll = new JScrollPane(ipanel);
		scroll.setPreferredSize(new Dimension(1000, 650));
		scroll.setWheelScrollingEnabled(false);
		
		// Add all the objects to the main window's content pane
		pane.add(scroll);
		pane.add(statusBar);
		
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
		
		// |  scroll
		// +--------------------------
		// |                   ^^^
		// | <<< statusBar
		spring.putConstraint(SpringLayout.WEST, statusBar, 5, 
				SpringLayout.WEST, pane);
		spring.putConstraint(SpringLayout.NORTH, statusBar, 5, 
				SpringLayout.SOUTH, scroll);
		
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
    
    /**
     * Quit the program (maybe). Ask the user to confirm, save, etc.
     */
    private void quit(){
        //      show a dialog asking the user to confirm
        int choice = JOptionPane.showConfirmDialog(this,
                "Save changes to path and location data?", "Save Changes",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        switch(choice){
        // if yes, save the data
        case JOptionPane.YES_OPTION:
            ipanel.writeAllXML();
            // fall through...
            
        // if no, just quit the program
        case JOptionPane.NO_OPTION:
            setVisible(false);
            dispose();
            System.exit(0);
            break;
            
        // if anything else happens, we don't quit
        default:
            break;
        }
        
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
	final static long serialVersionUID = 37;  //huh?
	
	//XXX: Here are the path and location version numbers.  
	final static int PATH_VERSION_NUMBER = 1;
	final static int LOCATION_VERSION_NUMBER = 2;
	
	private int maxUnitIncrement = 5;
	private boolean missingPicture = false;
	
	// The current path that we're focusing on
	private Vector <Point>curPath;
	
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
	
	// Prviously Searched Point
	private Point previouslySearchedPoint;
	
	// The index in the paths vector of the last used path out of
	// a searched locations.  
	private int lastVertex = 0;
	
	/*
	 * Input/Output filenames
	 */
	// Raw, unoptimized paths
	final String rawPathFile = "data/rawPath.dat";
	final String rawLocFile  = "data/rawLocations.dat";
	final static String XMLPathFile = "data/rawPath.xml";
	final static String XMLLocFile  = "data/rawLocations.xml";
	
	// Optimized paths put back into unoptimized format for debugging
	final static String optPathFile = "data/optimizedPath.xml";
	final static String optLocFile = "data/optimizedLocations.xml";
	
	// Binary file output
	final static String binaryPoints = "data/binPointData.dat";
	final static String binaryLocations =  "data/binLocationData.dat";
	final static String binaryEdges = "data/binEdgeData.dat";
	
	// List of all locations mapped
	final String LOCATIONS_TXT_FILE = "Locations.txt";
    
    // actions taken by selectLocation():
    // center on the selected location, or create a new point connected to the
    // selected location
    enum SelectLocationJobID { 
    	/**
    	 * Job = center 
    	 */
    	CENTER, 
    	/**
    	 * Job = create new
    	 */
    	NEW };
	
	/**
	 * @param i The image to display
	 * @param maxUnitPassed 
	 * @param newParent ?
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
		curPath = paths.get(pathNumIndex);
		setPointNumIndexToEnd();
		setDefaultStatusBar();
		
		/* add the mouse-click handler */
		addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e)) {
					/** CTRL + left click == select closest point **/
					if(e.isControlDown() /*&& !e.isAltDown()*/)
					{
                        Point closestPoint = getClosestPoint(e.getPoint());
                        if(closestPoint != null)
                        {
                        	if(e.isShiftDown())  // connect to point
                        	{
                        		createNewPointInCurPath(closestPoint);
                        	}
                        	else  // select point
                        		iterateThroughPathsAtPoint(closestPoint);
                        }
				    }
					else // plain left click (no modifiers)
					{
				        createNewPointInCurPath(e.getPoint());
					}
				}
				else if(SwingUtilities.isRightMouseButton(e)) {
                    // Left control-click ==> select closest location
                    // if only control is down
				    if(e.isControlDown() && !e.isAltDown() && !e.isShiftDown()){
				    	Location closestLoc = getClosestLocation(e.getPoint());
                        
                        // select the closest point we found
                        if(closestLoc != null){
                            iterateThroughPathsAtPoint(closestLoc.cord);
                        }
				    }
				    /** Ctrl + shift + right click ==> Move to closest point **/
				    else if(e.isControlDown() && e.isShiftDown())
				    {
				    	Point closestPoint = getClosestPoint(e.getPoint());
				    	changeCurSelectedPointCord(closestPoint);
				    }
                    // Right ==> change coordinates
				    // regular right click; no modifiers
				    else{
						// Change current point's coordinates
						changeCurSelectedPointCord(e.getPoint());					
				    }
				}
				// If you use any other buttton on your mouse...
				else{
					// Use a menu to select a location 
					middleClickGoToLocation();
				}
			}
		});
		
	}
	
	/**
	 * Returns the closest location to the passed point that is contained
	 * in the collection of locations.
	 * @param inputPoint Point to calculate distances to each location
	 * @return The location closest to the passed in point
	 */
	public Location getClosestLocation(Point inputPoint)
	{
		double dist = -1.0;
        double minDist = -1.0;
        Location closestLoc = null;
        // select the location closest to the click
        for(Location l: locations){
            double dx = inputPoint.x - l.cord.x;
            double dy = inputPoint.y - l.cord.y;
            dist = Math.sqrt(dx*dx + dy*dy);
            if(minDist == -1.0 || minDist > dist){
                minDist = dist;
                closestLoc = l;
            }
        }
        return closestLoc;
	}
	
	/**
	 * Returns the closest point to the passed point that is contained
	 * by one of the paths.
	 * @param inputPoint Point to calculate distances to each point
	 * in the paths vector
	 * @return The closest point to the passed inputPoint
	 */
	public Point getClosestPoint(Point inputPoint)
	{
        double dist = -1.0;
        double minDist = -1.0;
        double dx, dy;   //gogo calc!  :)
        Point closestPoint = null;
        // select the location closest to the click
        for(Vector<Point> path: paths)
        	for(Point p : path)
        	{
        		dx = inputPoint.x - p.x;
        		if(dx > minDist)
        			continue;
        		dy = inputPoint.y - p.y;
        		if(dy > minDist)
        			continue;
        		dist = Math.sqrt(dx*dx + dy*dy);  //pathag
        		if(minDist == -1.0 || minDist > dist)
        		{
        			minDist = dist;
        			closestPoint = p;
        		}
        	}
        return closestPoint;
	}
	
	
	/**
	 * This method creates a new point in the currently selected path
	 * at the passed in point.
	 * @param pointToAdd The point to add the location at
	 */
	public void createNewPointInCurPath(Point pointToAdd)
	{
		// If the new point is going to be the first element in the current 
		// path, allocate a vector for the path before adding 
		// the Point to the path.  
		if(curPath == null)
		{
			// Allocate new vector
			curPath = new Vector<Point>();
		}

		//add the point to the current path (at the next possible index)
		// Increment the pointNumIndex to set focus to the newly created point
		curPath.add(++pointNumIndex, pointToAdd);

		// update the status bar
		setDefaultStatusBar();

		// and redraw immediately to see the changes
		repaint();
	}

	/**
	 * Get the current point!
	 * @return The current point or null if the current path has no points
	 * (or if pathNumIndex somehow got set too large.)
	 */
	public Point getCurrentPoint()
	{
		if(pointNumIndex < 0)
		{
			System.err.println("pointNumIndex < 0 in getCurrentPoint!");
			return null;
		}
		if(pointNumIndex > curPath.size() - 1)
		{
			System.err.println(
					"PointNumIndex > curPath.size() -1 in getCurrentPoint!");
			return null;
		}
		return(curPath.get(pointNumIndex));
	}
	
	/**
	 * Change the coordinates of the currently selected point to the passed
	 * in point.
	 * <p>
	 * 2 cases:<br>
	 * 1:  Location exists at the previous point:<br>
	 * Move the location and all points that it intersects with to the
	 * new point<br><br>
	 * 2:  Location does not exist at the previous point:<br>
	 * Move the previous point to the new point
	 * 
	 * @param pointToMoveTo The point to move the current point to
	 */
	public void changeCurSelectedPointCord(Point pointToMoveTo)
	{
		
		int locIndex = -1;
		// check if there's an existing point
		// findLocationAtPoint returns an index if found, -1 if not found.
		locIndex = findLocationAtPoint(getCurrentPoint()); 
		
		//When we find a location at the point.  
		if(locIndex >= 0){
			// Get the old point -- The point before the move...
			Point oldPoint = new Point(getCurrentPoint());
			// move the location
			Location toMove = locations.get(locIndex);
			toMove.cord = pointToMoveTo;
			// Move all points connected to the location
			for(Vector<Point> path: paths)  // For each path in set of paths
				for(Point ptInPath: path)   // For each point in path
					// If a point is equal to the old point
					if(ptInPath.equals(oldPoint))
					{
						// Move point in path to new point
						ptInPath.setLocation(pointToMoveTo);
					}
		}
		else  // no location...just move the point
		{
			// move the point
			getCurrentPoint().setLocation(pointToMoveTo);
		}
		// set statusbar text
		setDefaultStatusBar();
		repaint();
	}
	
	/**
	 * Creates a JDialog box that allows the user to click on a location name
	 * to select it for either editing or creating a new point at. 
	 * @param passedJobId What to do with the selected location
	 */
	public void selectLocation(SelectLocationJobID passedJobId)
	{
		//Index into the locationNames[] and locations vector
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
				((Location)locations.get(index)).getName();
		}
		
		Arrays.sort(locationNames);
		
		//Create the JList, passing the array of location names
		final JList locBox = new JList(locationNames);
		JScrollPane scrollPane = new JScrollPane(locBox);
		
		// Add the list of locations and the cancel button to the dialog box.
		dialog.getContentPane().add(scrollPane);
		dialog.getContentPane().add(cancel);
		
		// Let's go packing!  You can't have a pack smaller than
		// what you're packing...
		dialog.pack();
		
		//final ScrollablePicture thisElement = this;?
		final SelectLocationJobID jobID = passedJobId;
		
	
		// Can you hear the cricket's chirping?
		locBox.addMouseListener( new MouseAdapter(){
			public void mouseClicked(MouseEvent event){
				
				// Get the point of the selected location
				int indexSelected = locBox.getSelectedIndex();
				
				Location [] sortedLocs = getSortedLocationArray();
				Location selectedLocation = sortedLocs[indexSelected];
				
				switch (jobID)
				{
				case CENTER:  // center on the selected location
					iterateThroughPathsAtPoint(selectedLocation.cord);
					centerOnSelectedPoint();
					break;
				case NEW:  // middle click --> go to location
					createNewPointInCurPath(selectedLocation.cord);
					break;
				default:
					System.err.println("Bad jobID in SelectLocation!");
					break;
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
	 * This method sets up a dialog box with a list of locations
	 * on the map.  The user can click on one of the items in the list
	 * and then a point is drawn to that location.  
	 */
	public void middleClickGoToLocation()
	{
		selectLocation(SelectLocationJobID.NEW);
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
	//		System.err.println("Key entered = " + KeyEvent.getKeyText(c));
			
			// F1: Erase current point (and location, if applicable)
			if(c ==  KeyEvent.VK_F1)
			{
				// Call a method to handle undoing the connection
				removeCurPoint();
			}
			// F2: Go to previous path option
			else if(c == KeyEvent.VK_F2 || c == KeyEvent.VK_MINUS)
			{
				// Call method to go to previous path
				goToPreviousPath();
			}
			// F3: Go to the next path (if exists)
			else if(c == KeyEvent.VK_F3 || c == KeyEvent.VK_EQUALS)
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
				writeXMLDialog();
			}
			// F8: load/read from files.
			else if(c ==  KeyEvent.VK_F8)
			{
				readXMLDialog();
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
//			// F11: Location editor!
//			else if(c == KeyEvent.VK_F11)
//			{
//				locationEditor();
//			}
			// F12: Create new path
			else if(c == KeyEvent.VK_F12)
			{
				createNewPath();
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
			curPath = paths.get(--pathNumIndex);
			// Automatically focus on the last element
			setPointNumIndexToEnd();
			// Set statusbar
			setDefaultStatusBar();
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
			curPath = paths.get(++pathNumIndex);
			// Automatically focus on the last element
			setPointNumIndexToEnd();
			// Set statusBar
			setDefaultStatusBar();
			repaint();
		}
	}
	
	/**
	 * Uses a JOptionPane dialog box to select a path number
	 * If valid input...uses the goToPathNumber(int) method to 
	 * set focus to the path.
	 */
	public void goToPath()
	{
		String selected = null;
		selected = JOptionPane.showInputDialog(parent, 
				"Choose a path # between 1 and " + paths.size(), 
				"Choose a path", JOptionPane.QUESTION_MESSAGE);
		int pathNum = pathNumIndex;
		try
		{
			pathNum = Integer.parseInt(selected);
		}
		catch(NumberFormatException e)
		{
			setStatusBarText("Invalid input: Please enter a integer value.");
            return;
		}
        
        // we do our own error checking here, because goToPathNumber
        // is lower-level and uses array indices (and prints error messages
        // accordingly)
        if(pathNum < 1 || pathNum > paths.size())
            setStatusBarText("Path number out of range!"
                    + " Enter number between 1 and " + paths.size() + ".");
        else
            goToPathNumber(pathNum-1);
	}
	
	/**
	 * Go to the given path number.
	 * @param pathNum path number to go to, between 0 and paths.size() - 1.
	 */
	public void goToPathNumber(int pathNum)
	{
		// if number passed is in range
		if(pathNum >= 0 && pathNum <= paths.size() - 1)
		{
			pathNumIndex = pathNum;
			curPath = paths.get(pathNum);
			//autofocus
			setPointNumIndexToEnd();
			// statusBar
			setDefaultStatusBar();
			repaint();
		}
		else
			System.err.println("goToPathNumber(): pathNum " + pathNum
                    + " out of range! Pass a number between 0 and "
                    + (paths.size()-1));
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
			setDefaultStatusBar();
			repaint();
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
		// If we can go forwards...
		if(pointNumIndex < curPath.size() - 1)
		{
			// Increment
			pointNumIndex++;
			repaint();
			// Show status bar
			setDefaultStatusBar();
		}
	}
	
	/**
	 * Prints out the listing of all locations on the map to a text file
	 */
	public void printLocationsToFile()
	{
		final String LOCATION_FILE_HEADER = "Locations:\n";
		final String WRITE_SUCCESS = "Locations written to text file: " + 
			LOCATIONS_TXT_FILE + ".";
		PrintStream outStream;
		
		Location [] sortedLocs = getSortedLocationArray();
		
		try{
			outStream = new PrintStream( new BufferedOutputStream(
							new FileOutputStream(LOCATIONS_TXT_FILE)));
			
			outStream.println(LOCATION_FILE_HEADER);  // print header
			
			// Print out a location info string for each location
			for(Location loc: sortedLocs)
			{
				outStream.println( loc.toString() );
				for(String alias: loc.getAliases()){
				    outStream.println("\t" + alias);
				}
			}
			//close the file stream
			outStream.close();
			// Notify of success
			setStatusBarText(WRITE_SUCCESS);
		}
		catch(Exception e){
			System.err.println("Error writing to file" + LOCATIONS_TXT_FILE);
		}
	}
	
	/**
	 * Creates a new path and adds it to the paths array.  
	 */
	public void createNewPath()
	{
		// The current pathNumIndex is the sizeof the vector before
		// The new element is created.
		pathNumIndex = paths.size();
		// Create the space for the new path.
		paths.add( new Vector<Point>() );
		// Set focus
		curPath = paths.get(pathNumIndex);
		
		setPointNumIndexToEnd();
		
		// Status bar
		setDefaultStatusBar();
		// dance, dance, dance!
		repaint();
	}
	
	/**
	 * This method puts up a JOptionPane confirmation box to determine
	 * if the user is sure they want to delete the currently selected
	 * path.  If Yes is entered, the path is removed by first deleting
	 * all of the points in the path, and then the path itself is
	 * removed from the paths vector.  
	 */
	public void deleteCurPath()
	{
		int confirmReturn;
		int numElements = curPath.size();
		confirmReturn = JOptionPane.showConfirmDialog(parent, 
				"Are you sure you want to delete path " + pathNumIndex + 
				" which contains " + numElements + " elements?", 
				"Delete Path", JOptionPane.YES_NO_OPTION, 
				JOptionPane.WARNING_MESSAGE);
		
		if(confirmReturn == JOptionPane.YES_OPTION)
		{
			if(paths.size() > 0)
			{
				// Remove all points ont the path
				while(removeCurPoint());
				if(paths.size() > 1)
					paths.remove( pathNumIndex );
				if(pathNumIndex > paths.size() -1)
					pathNumIndex = paths.size() -1;
				// Get the current path
				curPath = paths.get(pathNumIndex);
				setPointNumIndexToEnd();
				setStatusBarText("Path deleted: " + numElements 
						+ " elements deleted");
			}
			else
			{
				setStatusBarText("No paths exists");
			}
		}
	}
	
	/**
	 * Centers the visible window to the currently selected (in focus) point.
	 */
	public void centerOnSelectedPoint()
	{
		// Only center if pointNumIndex in range
		if(pointNumIndex <= curPath.size() - 1 && pointNumIndex >= 0)
		{
			//Get a pointer to the Point that we want to center too
			Point center = getCurrentPoint();
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
		else
		{
			setStatusBarText("Please select a point to center on!");
		}
	}

	/**
	 * Create location on the current point (if it is valid)
	 */
	public void createLocation()
	{
		final String DEFAULT_NAME = "<Enter Location name here>";
		final String NO_POINT_SELECTED = "Please select a point first!"; 
		if(curPath == null || curPath.size() == 0)
		{
			parent.statusBar.setText(NO_POINT_SELECTED);
			return;
		}
		Point curPoint = getCurrentPoint();
		Location newLoc = new Location(curPoint, DEFAULT_NAME);
		newLoc.setCanPassThrough(false);
		newLoc.setAllowIntersections(true);
		newLoc.setDisplayName(true);
		
		// Create the dialog box (parent == ShowImage object)
		final JDialog dialog = new JDialog(parent, "Create Location");
		
		// Set the layout
		dialog.getContentPane().setLayout( new FlowLayout() );
		// Create the JPanel for the location
		ComponentEditor componentPanel = new ComponentEditor(newLoc, dialog);
		
		//buttons...ComponentEditor
		JButton save = new JButton("Save");
		JButton cancel = new JButton("Cancel");
		
		// add buttons
		dialog.add(componentPanel);
		dialog.add(save);
		dialog.add(cancel);
		
		// Create actionlistener for registering choice
		ActionListener changeListener 
			= new LocationCreationChoiceListener(newLoc, dialog, componentPanel, save, cancel);
		
        
		save.addActionListener(changeListener);
		cancel.addActionListener(changeListener);
        
		dialog.pack();
		dialog.setVisible(true);
	}

	/**
	 * Listner for save of cancel used by createLocation()
	 */
	class LocationCreationChoiceListener implements ActionListener
	{
		// The location to potentially add
		private Location locationToAdd;
		// The calling JDialog box
		private JDialog caller;
		// Buttons...make sure you get the order right in the constructor
		private JButton save;
		private JButton cancel;
		ComponentEditor cpannel;
		
		/**
		 * Constructor for the listner...saves values
		 * @param locToAdd The location to add
		 * @param caller Who called the listner
		 * @param cpannel The component editor to call in order to do a save
		 * @param save To determine the action
		 * @param cancel To determine the action
		 */
		public LocationCreationChoiceListener(Location locToAdd, JDialog caller, 
				ComponentEditor cpannel, JButton save, JButton cancel)
		{	
			this.locationToAdd = locToAdd;
			this.caller = caller;
			this.save = save;
			this.cancel = cancel;
			this.cpannel = cpannel;
		}
		public void actionPerformed(ActionEvent e)
		{
			if(e.getSource() == save)
			{
				cpannel.saveVariables();
				locations.add(locationToAdd);
				parent.statusBar.setText("Location " + locationToAdd.toString() 
						+ "added.");
			}
			else if(e.getSource() == cancel)
			{
				parent.statusBar.setText("Creation of location canceled");
			}
			else
			{
				System.err.println("Passed bad button!");
			}
			caller.dispose();
			caller.setVisible(false);
			repaint();
		}
	}
	
	/**
	 * Edit a perticular object location
	 * First checks to see if there is a currently selected location
	 */
	public void editLocation()
	{
		final String NO_LOCATION_SELECTED = "Please select a location first!"; 
		if(curPath == null || curPath.size() == 0)
		{
			parent.statusBar.setText(NO_LOCATION_SELECTED);
			return;
		}
		Point curPoint = getCurrentPoint();
		if(curPoint == null)
		{
			parent.statusBar.setText(NO_LOCATION_SELECTED);
		}
		
		int locationIndex = findLocationAtPoint(curPoint);
		if(locationIndex < 0)
		{
			parent.statusBar.setText(NO_LOCATION_SELECTED);
			return;
		}
		Location locToEdit = getLocation(locationIndex);
		editLocation(locToEdit);
	}

	/**
	 * Uses a dialog box to edit a single location.
	 * Uses the component editor to provide to edit the location.
	 * Adds a close and delete button.  The close button
	 * has an actionlistener that disposes of the editing dialog
	 * box.  Delete actionlistener calls deleteLocation(Location l).
	 * @param locToEdit The location to edit
	 */
	public void editLocation(Location locToEdit)
	{
		// Create the dialog box (parent == ShowImage object)
		final JDialog dialog = new JDialog(parent, "Edit Location");
		
		// Set the layout
		dialog.getContentPane().setLayout( new FlowLayout() );
		// Create the JPanel for the location
		JPanel componentPanel = new ComponentEditor(locToEdit, dialog);
		JButton close = new JButton("close");
		JButton delete = new JButton("Delete Location");
		// Add JPanel
		dialog.add(componentPanel);
		// add button
		dialog.add(close);
		dialog.add(delete);
		dialog.pack();
		dialog.setVisible(true);
        
		// Hack to pass down the location
		final Location passLocDown = locToEdit;
		/**
		 * Action listener for deleting the location button
		 */
		delete.addActionListener(new ActionListener(){
			/**
			 * On press of the delete button in the edit location window,
			 * call delete locations and dispose of the main dialog box.
			 */
			public void actionPerformed(ActionEvent e)
			{
				// Call the deleteLocation function to display a confirm dialog
				// box...if yes is clicked, the location is deleted
				deleteLocation(passLocDown);
				dialog.dispose();
				dialog.setVisible(false);
			}
		});
		
		/**
		 * Action listener that closes the dialog box
		 */
		close.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{	
				repaint();
				dialog.dispose();
				dialog.setVisible(false);
			}
		});
	}

	/**
	 * Displays a confirmation dialog box on deleting a location.
	 * If the user clicks yes, the location will be deleted (removed from
	 * Vector locations).
	 * If the user clicks no, no change is made.  
	 * @param locToDelete The location to delete
	 */
	public void deleteLocation(Location locToDelete)
	{
		int confirmReturn = 0;
		confirmReturn = JOptionPane.showConfirmDialog(parent, 
				"Are you sure you want to delete location " + locToDelete, 
				"Delete Path", JOptionPane.YES_NO_OPTION, 
				JOptionPane.WARNING_MESSAGE);
		
		if (confirmReturn == JOptionPane.YES_OPTION)
		{
			String deletedLocName = locToDelete.getName();
			// remove location
			locations.remove(locToDelete);
			setStatusBarText("Location: " + deletedLocName + " deleted!");
			repaint();
		}
		else
			setStatusBarText("Deletion canceled");
	}
    
    /**
     * Remove all paths and locations. Pretty much the opposite of loading
     * from disk. Protected, of cource, by a big scary dialog box.
     */
    public void clearPathsAndLocations()
    {
        int choice = JOptionPane.showConfirmDialog(parent,
                "Are you sure you want to delete all paths and locations?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        
        if(choice == JOptionPane.YES_OPTION){
            setStatusBarText("All paths and locations deleted");
            
            // empty everything
            locations.clear();
            paths.clear();
            
            // add back the default path
            paths.add(new Vector<Point>());
            
            // set indices, etc, correctly
            pathNumIndex = 0;
            curPath = paths.get(pathNumIndex);
            setPointNumIndexToEnd();
        }
        else
            setStatusBarText("Deletion canceled");
    }
	
	/**
	 * This method launches a dialog box hereby known as the locationEditor
	 * This all powerful God of an editor allows for the editing of data in the
	 * location vector.  
	 */
	public void locationEditor()
	{
		// The overall dialog box, this contains everything need...
		final JDialog dialog = new JDialog(parent, "Location Editor");

		// Set layout of the dialog box
		dialog.setLayout( new FlowLayout() );

		// JPannel to contain the scroll
		//JPanel panel = new JPanel();
		JPanel panel = new JPanel();
		// To list all of the locations in rows (with submit buttons)
		panel.setLayout( new GridLayout(locations.size() + 1, 1));


		//Create the action listeners!
		Location[] sortedLocs = getSortedLocationArray();

		// For every location in the sortedLocation array
		for (Location loc : sortedLocs) {
			JPanel locPanel = new ComponentEditor(loc, dialog);
			panel.add(locPanel);
		}

		// create a new scroll over the panel
		JScrollPane scroll = new JScrollPane(panel);
		// set size of scroll
		scroll.setPreferredSize(new Dimension(780,650));
		// Force the scroll bars
		scroll.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setHorizontalScrollBarPolicy(
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		JButton close = new JButton("Close");
		dialog.add(scroll);
		dialog.add(close);
		dialog.pack();
		//dialog.setSize(800,700);
		dialog.setVisible(true);

		close.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{	
				repaint();
				dialog.dispose();
				dialog.setVisible(false);
			}
		});
	}	

	/**
	 * This method deletes the current point from the path.
	 * @return True if successful, false otherwise.
	 */
	public boolean removeCurPoint ()
	{
		// Only go back if there is a point to go to
		if(curPath.size() > 0)
		{
			// Get a pointer to the point that we want to undo
			Point undoPoint = getCurrentPoint();


			// Returns -1 if no index was found.
			int locIndex = findLocationAtPoint(undoPoint);

			// if there's a location and it has less than two
			// connections, delete the location in addition to the point.
			// Get the index of a location at the point.
			if(locIndex >= 0 && numOfPathsConnectedToPoint(undoPoint) < 2)
				locations.remove(locIndex);
			
			// remove the current node, decrement pointNumIndex
			curPath.remove(pointNumIndex);
			if(curPath.size() == 0 || pointNumIndex > 0)
				pointNumIndex--;
			
			setDefaultStatusBar();
			repaint();
			return true;
		}
		else
		{
			setStatusBarText("Error: Attempted to remove non-existing point");
			return false;
		}
	}	
	
	/**
	 * Set the pointNumIndex to the last element in the currently selected path.
	 */
	public void setPointNumIndexToEnd()
	{
			pointNumIndex = curPath.size() - 1;
	}
	
	/**
	 * Scales all of the data to a specified 
	 * x offset, y offset, x scale, or y scale.
	 */
	public void scaleData()
	{
		final int NUM_COLUMNS = 10;
		final String DEFAULT_OFFSET = "0";
		final String DEFAULT_SCALE = "1";
		final JDialog dialog = new JDialog(parent, "Scale Data", true);
		dialog.setLayout( new FlowLayout() );
		JLabel xOffsetLabel = new JLabel("X Offset");
		JLabel yOffsetLabel = new JLabel("Y Offset");
		JLabel xScaleLabel  = new JLabel("X scale");
		JLabel yScaleLabel  = new JLabel("Y scale");
		
		final JTextArea xOff = new JTextArea(DEFAULT_OFFSET, 1, NUM_COLUMNS);
		final JTextArea yOff = new JTextArea(DEFAULT_OFFSET, 1, NUM_COLUMNS);
		
		final JTextArea xScale = new JTextArea(DEFAULT_SCALE, 1, NUM_COLUMNS);
		final JTextArea yScale = new JTextArea(DEFAULT_SCALE, 1, NUM_COLUMNS);
		
		dialog.add(xOffsetLabel);
		dialog.add(xOff);
		
		dialog.add(yOffsetLabel);
		dialog.add(yOff);
		
		dialog.add(xScaleLabel);
		dialog.add(xScale);
		
		dialog.add(yScaleLabel);
		dialog.add(yScale);
		
		JButton submit = new JButton("Submit");
		JButton cancel = new JButton("Cancel");
		
		submit.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				boolean error = false;
				double xOffValue = 0, yOffValue = 0, 
						xScaleValue = 1, yScaleValue = 1;
				try
				{
					xOffValue = Double.parseDouble(xOff.getText());
					yOffValue = Double.parseDouble(yOff.getText());
					xScaleValue = Double.parseDouble(xScale.getText());
					yScaleValue = Double.parseDouble(yScale.getText());
				}
				catch(NumberFormatException exc)
				{
					setStatusBarText(
							"Number format exception converting value!");
					error = true;
				}
				
				// Apply to paths
				for(Vector<Point> path : paths) // for all paths
				{
					for(Point p : path)  // for all points in each path
					{
						p.x = (int)(p.x*xScaleValue) + (int)xOffValue;
						p.y = (int)(p.y*yScaleValue) + (int)yOffValue;
					}
				}
				
				// Apply to locations
				for(Location loc : locations)
				{
					Point p = loc.cord;
					p.x = (int)(p.x*xScaleValue) + (int)xOffValue;
					p.y = (int)(p.y*yScaleValue) + (int)yOffValue;
				}
				parent.repaint();
				dialog.dispose();
				dialog.setVisible(false);
				if(!error)
					setStatusBarText("Data scaled");
			}
		});
		
		cancel.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				dialog.dispose();
				dialog.setVisible(false);
				setStatusBarText("Data scaling canceled");
			}
		});
		
		dialog.add(submit);
		dialog.add(cancel);
		
		dialog.pack();
		dialog.setVisible(true);
	}
	
	/**
	 * Prompts with a JOptionPane confirmation dialog box
	 * to confirm that the user wants to run path Optimize.
	 * 
	 * If the user confirms, then path optimize is ran and 
	 * a dialog is displaying showing the filenames writen to.
	 * Otherwise, a simple message is displayed.
	 */
	public void writeOptimize()
	{
		int confirmReturn = JOptionPane.showConfirmDialog(parent, 
				"Would you like to write out optimized data?", 
				"Optimize", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if(confirmReturn == JOptionPane.YES_OPTION)
		{
			PathOptimize.run(paths, locations, 
				optPathFile, optLocFile,
				binaryPoints, binaryLocations, binaryEdges);
			setStatusBarText("Data optimization completed, " +
					"output written to: " + binaryPoints + ", " + 
					binaryLocations + ", and " + binaryEdges);
		}
		else
		{
			setStatusBarText("Optimization canceled");
		}
	}
	
	/**
	 * Prompts for confirmation to load the optimized representation of
	 * the data.  If user responds with yes, call the load routines
	 * using the optimized data files.  
	 */
	public void readOptimize()
	{
		int confirmReturn = JOptionPane.showConfirmDialog(parent, 
				"Would you like to read in optimized data?", 
				"Load Optimized data", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if(confirmReturn == JOptionPane.YES_OPTION)
		{
			loadXMLLocations(ScrollablePicture.optLocFile);
			loadXMLPaths(ScrollablePicture.optPathFile);
			setStatusBarText("Optimized data loaded");
		}
		else
		{
			setStatusBarText("Reading canceled");
		}
	}
	
	/**
	 * Reads a tab diliminated file that contains location names
	 * followed by their coordinates.  Adds each location with a new path
	 * and point at the location's coordinate.
	 */
	public void readCustomLocationInformation()
	{
		// Where in the tab deliminated field the following entries are located
        final int TYPE_FIELD = 0;
		final int NAME_FIELD = 1;
		final int LOCAL_X_FIELD = 2;
		final int LOCAL_Y_FIELD = 3;
		BufferedReader stream = null;  //"Initialize" our stream
		final String LOCATION_FILE_NAME = "locations.tab";  // name of the file
		try{
			// Stream wrapping
			stream = new BufferedReader(
							new InputStreamReader(
									new FileInputStream(LOCATION_FILE_NAME)));
			// keep track of the previous location in the file
            Location prevLoc = null;

            while(stream.ready())   // While the stream can be read...
			{
				String line = stream.readLine();  // get line from stream
				// Tab deliminated file ==> split on regex \t
				String [] split = line.split("\t");
				String name = split[NAME_FIELD];
				// initialize the X/Y coordinates
                if(split[TYPE_FIELD].equals("OFFICIAL"))
                {
                    int xCord = 0;
                    int yCord = 0;
                    try
                    {
                        // Grab the X/Y coordinate from the split line
                        xCord = Integer.parseInt(split[LOCAL_X_FIELD]);
                        yCord = Integer.parseInt(split[LOCAL_Y_FIELD]);
                    }
                    // Make sure that we didn't mess up and pass non-integers 
                    // somehow
                    catch (NumberFormatException e)
                    {
                        System.err.println("Bad integer value for " + name);
                        e.printStackTrace();
                    }
                    // Add the line as a location
                    prevLoc = new Location(new Point(xCord, yCord), name);
                    locations.add(prevLoc);
                    // create a new path
                    createNewPath();
                    // Add the location's coordinate to that path
                    createNewPointInCurPath(new Point(xCord, yCord));
                    
                    //System.err.println("Added location: " + prevLoc);
                }
                else if(split[TYPE_FIELD].equals("ALIAS"))
                {
                    // make sure there's actually a previous location
                    if(prevLoc != null)
                    {
                        // add this alias to the previous location
                        prevLoc.addAlias(split[NAME_FIELD]);
                        //System.err.println("Alias for " + prevLoc + ": "
                        //       + split[NAME_FIELD]);
                    }
                    else
                    {
                        // this shouldn't happen...
                        System.err.println("Malformed file: Alias listed with" +
                                " no previous location.");
                    }
                }
                else
                {
                    // ...and neither should this
                    System.err.println("Oh no! Unknown field type: "
                            + split[TYPE_FIELD]);
                }
			}

		}
		// If there was a problem opening the file
		catch(FileNotFoundException e)
		{
			System.err.println("Could not find file: " + LOCATION_FILE_NAME);
			e.printStackTrace();
		}
		// If there was an IO exception
		catch(IOException e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * This method prompts for confirmation to load the default raw
	 * data files.  If user confirms, the files are loaded by delegated methods
	 */
	public void readXMLDialog(){
        final String prompt = "Are you sure you want to load the XML data?";
		int confirmReturn = JOptionPane.showConfirmDialog(parent, 
				prompt, "Load Data", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if(confirmReturn == JOptionPane.YES_OPTION)
		{
            readAllXML();
		}
		//XXX: Need to check for errors!
	}
    
    /**
     * Read raw path and location data from the XML file.
     */
    public void readAllXML()
    {
        setStatusBarText("Loading data...");
        loadXMLLocations(XMLLocFile);
        loadXMLPaths(XMLPathFile);
        setStatusBarText("Raw data loaded.");
    }
	
	/**
	 * This creates the dialog prompting for confirmation to write files.
	 * If user confirms, then the current data is writeen out to XML files.
	 */
	public void writeXMLDialog(){
        final String prompt = "Are you sure you want to save the XML data?";
		int confirmReturn = JOptionPane.showConfirmDialog(parent, 
				prompt, "Save Data", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if(confirmReturn == JOptionPane.YES_OPTION)
		{
		    writeAllXML();
		}
		else
			setStatusBarText("Writing canceled");
	}
    
    /**
     * Write raw path and location data to disk, and print any errors to the
     * status bar.
     */
    public void writeAllXML()
    {
        String errors = null;
        setStatusBarText("Writing...");
        String returnStatus = XMLFileIO.writeLocations(XMLLocFile, locations);
        if(returnStatus != null)
            errors = returnStatus;
        returnStatus = XMLFileIO.writePaths(XMLPathFile, paths);
        if(returnStatus != null)
            errors += returnStatus;
        if(errors == null)
            setStatusBarText("Raw data written to XML files: " 
                    + XMLLocFile + " and " + XMLPathFile + ".");
        else
            setStatusBarText(errors);
    }
    
    /**
     * Load locations from an XML file. Woohoo.
     * @param locationFile The name of the file to load locations from
     */
    public void loadXMLLocations(String locationFile){
        Vector<Location> newLocs = XMLFileIO.loadLocations(locationFile);
        locations = newLocs;
        goToPathNumber(paths.size() - 1);
        
		// Set the element focus to the last element in current path
		setPointNumIndexToEnd();
		// you know the drill...
        setDefaultStatusBar();
        repaint();
    }

    /**
     * Load paths from an XML file.
     * @param pathFile the pathfile to load
     */
    public void loadXMLPaths(String pathFile){
        Vector<Vector<Point>> newPaths = XMLFileIO.loadPaths(pathFile);
        paths = newPaths;
        // cha-cha-cha!
        goToPathNumber(paths.size() - 1);
        setPointNumIndexToEnd();
        
        // we ALWAYS set the status bar after calling this method...
        //setDefaultStatusBar();
        //repaint();
    }
    
    /**
     * Write locations out to an XML file, delegates to XMLFileIO method
     */
    public void writeXMLLocations(){
    	String returnStatus = XMLFileIO.writeLocations(XMLLocFile, locations);
    	if(returnStatus != null)
    		setStatusBarText(returnStatus);
    	else
    		setStatusBarText("Location writing complete");
    }
    
    /**
     * Write paths out to an XML file, delegates to XMLFileIO method
     */
    public void writeXMLPaths()
    {
    	String error = XMLFileIO.writePaths(XMLPathFile, paths);
    	if(error == null)
    		setStatusBarText("XML Path data writen");
    	else
    		setStatusBarText(error);
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
				
				// try to set the X value to what the user entered.
				try
				{
					newX = Integer.parseInt(xinput.getText());            
				}
				// if we can't, use the current value
				catch(NumberFormatException numException)
				{
					System.err.println("Could not parse X value, " +
							"using current point's x value.");
					setStatusBarText("Please enter integer values only!");
					newX = getCurrentPoint().x;
				}
				
				// try to set the Y value to what the user entered.
				try
				{
					newY = Integer.parseInt(yinput.getText());          
				}
				// if we can't, use the current value
				catch(NumberFormatException numException)
				{
					System.err.println("Could not parse Y value, " +
					"using current point's y value.");
					setStatusBarText("Please enter integer values only!");
					newY = getCurrentPoint().y;
				}
				
				// change the location of the current point, and do all the
				// associated mangling of locations, and whatnot
				changeCurSelectedPointCord(new Point(newX, newY));
			}
		});
		
		dialog.setVisible(true);
	}
	
	/**
	 * Set the statusbar to the default text (see statusBarText())
	 */
	public void setDefaultStatusBar()
	{
		parent.statusBar.setText( statusBarText() );
	}
	
	/**
	 * Set status bar to a passed in String
	 * @param stringToSet String to set the statusbar to
	 */
	public void setStatusBarText( String stringToSet )
	{
		parent.statusBar.setText( stringToSet );
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
		int pathNumber = pathNumIndex + 1;

		return ( "Path Number: " + pathNumber  + " of " + paths.size()+
				",  Element: " + elementNumber + " of " + curPath.size() +
				printCurrentPoint() + getCurrentLocationDescription() );
	}
	
	
	/**
	 * Print the name of the location if there is a location that intersects
	 * with the current point.
	 * This method is used to by setStatusBar.  
	 * @return name of location if exist, else null string.
	 */
	public String getCurrentLocationDescription () {
		// Only print if locations exists and there are points
		// on the currently selected path.  (Null exception guards)
		String stringToReturn = "";
		if( locations.size() > 0 && curPath.size() > 0)
		{
			int locIndex = findLocationAtPoint(getCurrentPoint());
			// If a valid index was found in the locations array...
			if (locIndex >= 0)
			{
				stringToReturn = ", Locations: ";
				Location locToPrint = getLocation(locIndex);
				
				stringToReturn += locToPrint.getName();
				
				if(locToPrint.isAllowIntersections())
				{
					stringToReturn += "--> intersect: true, ";
				}
				else
				{
					stringToReturn += "--> intersect: false, ";
				}
				
				if(locToPrint.isCanPassThrough())
				{
					stringToReturn += "pass: true, ";
				}
				else
				{
					stringToReturn += "pass: false, ";
				}
				
				if(locToPrint.isDisplayName())
				{
					stringToReturn += " displayable: true.";
				}
				else
				{
					stringToReturn += " displayable: false.";
				}
				
				return(stringToReturn);
			}
		}
		// No location ==> don't print info.
		return("");
	}
	
	/**
	 * Find the index of any location that exists at a given point.
	 * @param pointToCompare Point to search at
	 * @return the index of the matching location, or -1 if none is found.
	 */
	public int findLocationAtPoint(Point pointToCompare){
		if(pointToCompare == null)
		{
			System.err.println("Point passed to compare to is null!");
			return -1;
		}
        for(Location l: locations)
            if(l.cord.equals(pointToCompare))
                return(locations.indexOf(l));  //we really should be just
                                               // returning a loc object
		return -1;
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
	 * Cyles through the paths that go through a specified point.
	 * Uses global variable: Point previouslySearchedPoint
	 * @param p The point to search at
	 */
	public void iterateThroughPathsAtPoint(Point p)
	{
		if(p == null)
		{
			setStatusBarText("Please select a point to iterate over");
			return;
		}
		if(!p.equals(previouslySearchedPoint))
		{
			previouslySearchedPoint = p;
			lastVertex = -1;
		}
		
		// Get the next path in the search
		lastVertex = getNextPathInSearch(++lastVertex, p);

		// If no path could be found...
		if(lastVertex < 0)
		{
			//System.err.println("No path found...starting again");
			lastVertex = -1;  // start at beginning again
			iterateThroughPathsAtPoint(p);  // search
		}
		
		// Look for weird error
		if(lastVertex > paths.size() - 1)
		{
			System.err.println("lastVertex is larger than paths vector!");
			return;
		}

		// Set current path & number
		curPath = paths.get(pathNumIndex = lastVertex);
		
		for(int index = 0; index < curPath.size(); index++)
		{
			if(p.equals(curPath.get(index)))
			{
				pointNumIndex = index;  //Set the pointNumIndex to point p
				repaint();
				setDefaultStatusBar();
				return;
			}
		}
		System.err.println("Boo!  You will never get printed!");
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
				{
					return(i);
				}
		
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
			if(((Location)locations.get(i)).getName().indexOf(
					locationName) >= 0 ){
				return(i);
			}
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
		// Print out point only if the size is greater than 0
		if( curPath.size() > 0)
		{
			return(",  @ (" + getCurrentPoint().x + 
					", " + getCurrentPoint().y + ")");
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
		g.setFont(new Font("Times New Roman", 0, 10));
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
				if(curPath == (Vector)paths.get(pathsIndex))
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
					if(prev != null /*&& 
							(visible.contains(cur) || visible.contains(prev))*/){
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
			g.drawString( getLocation(locIndex).getName(), x, y);
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
	
	/**
	 * Sorts the location vector into an array.
	 * @return A sorted array of the location objects.  
	 */
	public Location [] getSortedLocationArray()
	{
		
		// Create an array for sorted locations, slam in all of the locations
		// in the locations vector
		Location [] sortedLocs = new Location[locations.size()];
		int index = 0;
		for (Location loc : locations) {
			sortedLocs[index] = loc;
			index++;
		}
		// sort the array
		Arrays.sort(sortedLocs, new Comparator<Location>(){
			public int compare(Location l1, Location l2)
			{  return(l1.getName().compareTo(l2.getName()));  }
		});
		// return the sorted Array
		return sortedLocs;
	}
}