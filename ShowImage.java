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
	
	private JMenuItem prevPath, nextPath, newPath, deletePath, iteratePaths, goToPath,// path manipulation
	undoConnection, manualPlace, nextElement, prevElement, centerOnElement,//element manipulation
	read, write, createLocationFile, changeImage, loadDefinedLocations,
    scaleData, loadXMLLocations, loadXMLPaths, writeXMLLocations, writeXMLPaths,//IO
	
    locationEditor, editLocation, createLocation, selectLocation;
	
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
			if(e.getSource() == read)
				ipanel.selectRead();
			
			if(e.getSource() == write)
				ipanel.selectWrite();
	
			if(e.getSource() == scaleData)
				ipanel.scaleData();
			
			if(e.getSource() == prevPath)
				ipanel.goToPreviousPath();
			
			if(e.getSource() == nextPath)
				ipanel.goToNextPath();
			
			if(e.getSource() == undoConnection)
				ipanel.removeCurPoint();
			
			if(e.getSource() == nextElement)
				ipanel.goToNextElement();
			
			if(e.getSource() == prevElement)
				ipanel.goToPreviousElement();
			
			if(e.getSource() == centerOnElement)
				ipanel.centerOnSelectedPoint();
			
			if(e.getSource() == createLocationFile)
				ipanel.printLocationsToFile();
			
			if(e.getSource() == manualPlace)
				ipanel.manualPlaceDialog();
			
			if(e.getSource() == locationEditor)
				ipanel.locationEditor();
			
			if(e.getSource() == newPath)
				ipanel.createNewPath();
			
			if(e.getSource() == deletePath)
				ipanel.deleteCurPath();
			
			if(e.getSource() == goToPath)
				ipanel.goToPath();
			
			if(e.getSource() == changeImage)
				changeImage(); //Do change image stuff here
			
			if(e.getSource() == editLocation)
				ipanel.editLocation();
			
			if(e.getSource() == createLocation)
				ipanel.createLocation();

			if(e.getSource() == selectLocation)
				ipanel.selectLocation(
                        ScrollablePicture.SelectLocationJobID.CENTER);

			if(e.getSource() == iteratePaths)
				ipanel.iterateThroughPathsAtPoint(ipanel.getCurrentPoint());
			
			if(e.getSource() == loadDefinedLocations)
				ipanel.readCustomLocationInformation();

            if(e.getSource() == loadXMLLocations)
                ipanel.loadXMLLocations();

            if(e.getSource() == loadXMLPaths)
                ipanel.loadXMLPaths();
  
            if(e.getSource() == writeXMLLocations)
            	ipanel.writeXMLLocations();
            
            if(e.getSource() == writeXMLPaths)
            	ipanel.writeXMLPaths();
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
		//   Y Y Y Y Y Y   Y   Y Y Y Y Y Y     Y Y             Y Y
		// IO
		final int READ_KEY 				= KeyEvent.VK_O; 
		final int WRITE_KEY 			= KeyEvent.VK_S;
		final int LOCATION_FILE_KEY 	= KeyEvent.VK_P;
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
		final int LOC_EDITOR_KEY 		= KeyEvent.VK_E;
		final int EDIT_LOCATION			= KeyEvent.VK_T;
		final int CREATE_LOCATION		= KeyEvent.VK_L;
		final int SELECT_LOCATION		= KeyEvent.VK_G;
		final int ITERATE_PATHS			= KeyEvent.VK_I;
		
		/** Setup the pretty menu bar!  **/
		MenuListener listener = new MenuListener();
		JMenuBar bar = new JMenuBar();
		
		/** Menu associated with file I/O options **/
		JMenu file = new JMenu("File I/O");
		
		read = file.add(makeJMenuItem("Open files", listener, READ_KEY));
		write = file.add(makeJMenuItem("Save files", listener, WRITE_KEY));
		
        file.addSeparator();
        
        writeXMLLocations = 
        	file.add(makeJMenuItem("Write XML Locations", listener, 0));
        writeXMLPaths = 
        	file.add(makeJMenuItem("Write XML Paths", listener, 0));
        
        loadXMLLocations = 
            file.add(makeJMenuItem("Load XML locations", listener, 0));
        loadXMLPaths = 
            file.add(makeJMenuItem("Load XML paths", listener, 0));

        file.addSeparator();
        
        loadDefinedLocations = 
            file.add(makeJMenuItem("Load defined locations", listener, 0));
		createLocationFile = file.add(makeJMenuItem(
				"Print Location List", listener, LOCATION_FILE_KEY));
		changeImage = file.add(makeJMenuItem("Change Image", listener, 0));
		
		scaleData = file.add(makeJMenuItem("Data Scaling", listener, 0));
		
		/** Menu associated with path options **/
		JMenu path = new JMenu("Path Editing");
		// Path Manipulation path
		newPath = path.add(makeJMenuItem("Create New Path", 
				listener, NEW_PATH_KEY));
		path.addSeparator();
		prevPath = path.add(makeJMenuItem("Previous Path (-)", 
				listener, 0));
		nextPath = path.add(makeJMenuItem("Next Path (+)", 
				listener, 0));
		goToPath = path.add(makeJMenuItem("Go to path number", listener, 0));
		iteratePaths = path.add(makeJMenuItem("Iterate through paths on Current Point", 
				listener, ITERATE_PATHS));
		path.addSeparator();
		deletePath = path.add(makeJMenuItem("Delete Path", 
				listener, DELETE_PATH_KEY));
		
		
		/** Menu associated with element options **/
		JMenu element = new JMenu("Element Editing");
		
		prevElement = element.add(makeJMenuItem("Previous Element in path", 
				listener, PREV_ELEMENT_KEY));
		nextElement = element.add(makeJMenuItem("Next Element in path", 
				listener, NEXT_ELEMENT_KEY));
		centerOnElement = element.add(
				makeJMenuItem("Center on selected element", 
						listener, CENTER_KEY));
		element.addSeparator();
		undoConnection = element.add(makeJMenuItem("Undo last created connection", 
				listener, UNDO_CONNECTION_KEY));
		manualPlace = element.add(makeJMenuItem("Manually Place Element", 
				listener, MANUAL_PLACE_KEY));
		
		/** Menu associated with locations **/
		JMenu location = new JMenu("Locations");
		locationEditor = location.add(makeJMenuItem("Locations Editor", 
				listener, LOC_EDITOR_KEY));
		editLocation = location.add(makeJMenuItem("Edit Current Location", 
				listener, EDIT_LOCATION));
		selectLocation = location.add(makeJMenuItem(
				"Go to location", listener, SELECT_LOCATION));
		createLocation = location.add(makeJMenuItem(
				"Create New Location (At current point)", 
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
	final String XMLPathFile = "data/rawPath.xml";
	final String XMLLocFile  = "data/rawLocations.xml";
	
	// Optimized paths put back into unoptimized format for debugging
	final String optPathFile = "data/optimizedPath.dat";
	final String optLocFile = "data/optimizedLocations.dat";
	
	// Binary file output
	final String binaryPoints = "data/binPointData.dat";
	final String binaryLocations =  "data/binLocationData.dat";
	final String binaryEdges = "data/binEdgeData.dat";
	
	// List of all locations mapped
	final String LOCATIONS_TXT_FILE = "Locations.txt";
    
    // actions taken by selectLocation():
    // center on the selected location, or create a new point connected to the
    // selected location
    enum SelectLocationJobID { CENTER, NEW };
	
	/**
	 * 
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
				int x = e.getX();
				int y = e.getY();
				if(SwingUtilities.isLeftMouseButton(e)) {
                    // Left control-click ==> select closest location
                    // if only control is down
				    if(e.isControlDown() && !e.isAltDown() && !e.isShiftDown()){
                        double dist = -1.0;
                        double minDist = -1.0;
                        Location closestLoc = null;
                        // select the location closest to the click
                        for(Location l: locations){
                            double dx = e.getX() - l.cord.x;
                            double dy = e.getY() - l.cord.y;
                            dist = Math.sqrt(dx*dx + dy*dy);
                            if(minDist == -1.0 || minDist > dist){
                                minDist = dist;
                                closestLoc = l;
                            }
                        }
                        
                        // select the closest point we found
                        if(closestLoc != null){
                            iterateThroughPathsAtPoint(closestLoc.cord);
                        }
				    }
                    // Left click ==> create new point in the current path
				    // regular click; no modifiers
				    else{
				        createNewPointInCurPath(new Point(x, y));
				    }
				}
				else if(SwingUtilities.isRightMouseButton(e)) {
					/** CTRL + right click == select closest path **/
					if(e.isControlDown() && !e.isAltDown() && !e.isShiftDown())
					{
                        double dist = -1.0;
                        double minDist = -1.0;
                        double dx, dy;   //gogo calc!  :)
                        Point closestPoint = null;
                        // select the location closest to the click
                        for(Vector<Point> path: paths)
                        	for(Point p : path)
                        	{
                        		dx = e.getX() - p.x;
                        		if(dx > minDist)
                        			continue;
                        		dy = e.getY() - p.y;
                        		if(dy > minDist)
                        			continue;
                        		dist = Math.sqrt(dx*dx + dy*dy);  //pathag
                        		if(minDist == -1.0 || minDist > dist)
                        		{
                        			minDist = dist;
                        			closestPoint = p;
                        		}
                        	}
                        
                        if(closestPoint != null)
                        {
                        	iterateThroughPathsAtPoint(closestPoint);
                        }
				    }
					else // plain right click (no modifiers)
					{
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
		
		//add the point to the current path
		curPath.add(pointToAdd);
		
		// Set the element focus to the last element (the one just created)
		setPointNumIndexToEnd();
		
		// and redraw immediately to see the changes
		repaint();
		
		// update the status bar
		setDefaultStatusBar();
		
		//set focus (listeners) back onto the picture
		this.requestFocus();
	}
	
	/**
	 * Get the current point!
	 * @return The current point or null if the current path has no points
	 * (or if pathNumIndex somehow got set too large.)
	 */
	public Point getCurrentPoint()
	{
		if(pointNumIndex < 0)
			return null;
		if(pointNumIndex > curPath.size() - 1)
			return null;
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
		// Get the old point -- The point before the move...
		Point oldPoint = new Point(getCurrentPoint());
		
		// check if there's an existing point
		// findLocationAtPoint returns an index if found, -1 if not found.
		int locIndex = findLocationAtPoint(getCurrentPoint()); 
		
		//When we find a location at the point.  
		if(locIndex >= 0){
			// move the location
			Location toMove = locations.get(locIndex);
			toMove.cord = pointToMoveTo;
			// Move all points connected to the location
			// For all paths
			for(Vector<Point> path: paths)
			{
				// For each path, loop through all points
				for(Point ptInPath: path)
				{
					// If a point is equal to the old point
					if(ptInPath.equals(oldPoint))
					{
						// Set the point equal to the old point to new
						// new coordinates
						ptInPath.setLocation(pointToMoveTo);
					}
				}
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
		// F11: Location editor!
		else if(c == KeyEvent.VK_F11)
		{
			locationEditor();
		}
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
				"Choose a path # between 0 and " + (paths.size() - 1), 
				"Choose a path", JOptionPane.QUESTION_MESSAGE);
		int pathNum = pathNumIndex;
		try
		{
			pathNum = Integer.parseInt(selected);
		}
		catch(NumberFormatException e)
		{
			setStatusBarText("Please enter a integer value");
		}
		goToPathNumber(pathNum);
	}
	
	/**
	 * Go to the passed in path number
	 * @param pathNum path number to go to.
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
			setStatusBarText( "PathNumber out of range!  " +
					"Please enter a path between: 0 and " + (paths.size() -1));
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
		final String LOCATION_FILE_HEADER = "Locations:";
		final String WRITE_SUCCESS = "Locations written to txt file: " + 
			LOCATIONS_TXT_FILE;
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
					new BufferedOutputStream(
							new FileOutputStream(pathOutputFile)));
			
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
					new BufferedOutputStream(
							new FileOutputStream(locationOutputFile)));
			
			// Write out the locations version number
			locout.writeInt(LOCATION_VERSION_NUMBER);
			
			if(LOCATION_VERSION_NUMBER == 1)
			{
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
					locout.writeObject(loc.getName());
				}
			}
			if(LOCATION_VERSION_NUMBER == 2 || LOCATION_VERSION_NUMBER == 3)
			{
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
					locout.writeObject(loc.getName());
					if(LOCATION_VERSION_NUMBER == 3)
						locout.writeObject(loc.getAliases());
					
					// The following two lines are the difference between
					// version 1 and version 2
					locout.writeObject(loc.getBuildingCode());
					locout.writeObject(loc.getKeywords());
				}
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
			"Exception reading \"" + pathFileName + "\"!";
		final String LOCATION_IO_ERROR =
			"Exception reading \"" + locFileName + "\"!";
		
		final String ERROR_READING_DATA = "Error occured in reading data!"
			+ " See stderr output.";
		
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
			System.err.println(PATHS_IO_ERROR + ": " + e + ": " + e.getMessage());
		}
		
		//Get the locations vector
		try{
			// Open stream
			ObjectInputStream locin = new ObjectInputStream(
					new FileInputStream(locationsInputFile));
			
			int locVersionNumber = locin.readInt();
			
			// Version 1: nothing much to say here, it's what I started with
			if(locVersionNumber == 1)
			{
				// Get the number of locations
				int numLocations = locin.readInt();
				
				// Get the count on IDs
				Location.IDcount = locin.readInt();
				
				// Temporary Location pointer to store the location while
				// it's properties are flowed in.
				Location tempLocation;
				
				// Temporary boolean values for location variable
				boolean tempIntersect;
				boolean tempCanPass;
				boolean tempDisplayName;
				
				// temporary coordinate
				Point tempCord;
				
				// Temporary ID number for binary files
				int tempID;
				
				// Temporary name of location
				String tempName;
				
				// Clean out the locations vector to store in the new info
				locations.clear();
				for(int locNum = 0; locNum < numLocations; locNum++)
				{
					// Read in the fields in the order that they were written
					tempIntersect = locin.readBoolean();
					tempCanPass = locin.readBoolean();
					tempCord = (Point)locin.readObject();
					tempDisplayName = locin.readBoolean();
					tempID = locin.readInt();
					tempName = (String)locin.readObject();
					
					//public Location(int x, int y, String passedName, 
					// ShowImage parent)
					// Create the new location with the passed in values
					tempLocation = new Location(tempCord.x, tempCord.y, 
							tempName);
					// Add the location into the locations vector
					locations.add(locNum, tempLocation);
					// Set the boolean values
					tempLocation.setAllowIntersections(tempIntersect);
					tempLocation.setCanPassThrough(tempCanPass);
					tempLocation.setDisplayName(tempDisplayName);
					// Set the binary file ID #
					tempLocation.ID = tempID;
				}
			}
			/**
			 * Version 2:
			 * Added two text field to location: buildingCode and keywords
			 */
			else if(locVersionNumber == 2 || locVersionNumber == 3)
			{
				// Get the number of locations
				int numLocations = locin.readInt();
				
				// Get the count on IDs
				Location.IDcount = locin.readInt();
				
				// Temporary Location pointer to store the location while
				// it's properties are flowed in.
				Location tempLocation;
				
				// Temporary boolean values for location variable
				boolean tempIntersect;
				boolean tempCanPass;
				boolean tempDisplayName;
				
				// temporary coordinate
				Point tempCord;
				
				// Temporary ID number for binary files
				int tempID;
				
				// Temporary name of location
				String tempName;
				
				// Temporary buildingCode for location
				String tempBuildingCode;
				
				// Temporary location keywords
				String tempKeywords;
				
				Vector <String> tempAliases = null;
				
				// Clean out the locations vector to store in the new info
				locations.clear();
				for(int locNum = 0; locNum < numLocations; locNum++)
				{
					// Read in the fields in the order that they were written
					tempIntersect = locin.readBoolean();
					tempCanPass = locin.readBoolean();
					tempCord = (Point)locin.readObject();
					tempDisplayName = locin.readBoolean();
					tempID = locin.readInt();
					tempName = (String)locin.readObject();
					
					if(locVersionNumber == 3)
						tempAliases = (Vector<String>)locin.readObject();

					
					tempBuildingCode = (String)locin.readObject();
					tempKeywords = (String)locin.readObject();
					
					
					//public Location(int x, int y, String passedName, 
					// ShowImage parent)
					// Create the new location with the passed in values
					tempLocation = new Location(tempCord.x, tempCord.y, 
							tempName);
					// Add the location into the locations vector
					locations.add(locNum, tempLocation);
					// Set the boolean values
					tempLocation.setAllowIntersections(tempIntersect);
					tempLocation.setCanPassThrough(tempCanPass);
					tempLocation.setDisplayName(tempDisplayName);
					if(locVersionNumber == 3)
						tempLocation.setAliases(tempAliases);
					
					// Set the two strings: Building Code and 
					// Location keywords
					tempLocation.setBuildingCode(tempBuildingCode);
					tempLocation.setKeywords(tempKeywords);
					
					// Set the binary file ID #
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
			System.err.println(LOCATION_IO_ERROR + ": " + e + ": " + e.getMessage());
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
			curPath = paths.get(pathNumIndex);
			// Set the point
			setPointNumIndexToEnd();
			
			// Do the repaint dance...woooo!
			repaint();
		}
		else
		{
			parent.statusBar.setText(ERROR_READING_DATA);
			
			/**
			 * If the path data didn't load, but location did...
			 * Create a new path for each location and add the location's point
			 * to that path.
			 */
			if(!pathLoadSuccess && locationLoadSuccess)
			{
				System.err.println("Path loading failed, " +
				"location loading sucessful");
				
				System.err.println("Adding new path and point " +
						"for each location.");
				// For every location entry
				for (Location tempLoc : locations) {
					// create a new path
					createNewPath();
					// Add the location's coordinate to that path
					createNewPointInCurPath(tempLoc.cord);
				}
			}
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
     * Load locations from an XML file. Woohoo.
     */
    public void loadXMLLocations(){
        Vector<Location> newLocs = XMLFileIO.loadLocations(XMLLocFile);
        for(Location l: newLocs){
            // add this location
            locations.add(l);
            // create a new path
            createNewPath();
            // Add the location's coordinate to that path
            createNewPointInCurPath(new Point(l.cord.x, l.cord.y));
        }
        
        // you know the drill...
        setDefaultStatusBar();
        repaint();
    }
    
    /**
     * Load paths from an XML file.
     */
    public void loadXMLPaths(){
        Vector<Vector<Point>> newPaths = XMLFileIO.loadPaths(XMLPathFile);
        for(Vector<Point> p: newPaths){
            paths.add(p);
        }
        // cha-cha-cha!
        setDefaultStatusBar();
        repaint();
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

		return ( "Path Number: " + (pathNumIndex + 1) + " of " + paths.size()+
				",  Element: " + (elementNumber) + " of " + curPath.size() +
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

/**
 * Interface: Used for the ComponentEditor class
 * Note: Indexes in get and set MUST match
 *       The number of values must match the # of descriptions
 */
interface ComponentElement
{
	/**
	 * Sets the passed element at passed index
	 * @param value modification to string
	 * @param index index to place modification
	 * @return String describing an error, if one occured, else null.
	 */
	String setElementValue(Object value, int index);
	/**
	 * Gets the value of an element @ index
	 * @param index Index to get element from
	 * @return value of element
	 */
	Object getElementValue(int index);
	/**
	 * Gets descriptions of each component element
	 * @return Associated with each element by common index
	 */
	String[] getComponentDescriptions();
}

/**
 * Location class.  Simply an object contatining a point (Point cord) and 
 * a description of the point (String name)
 **/
class Location implements Serializable, ComponentElement
{
	final static long serialVersionUID = 1; // beats me
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
	private String name;
	
	private Vector <String> aliases;
	
	/**
	 * Building code for a location
	 */
	private String buildingCode = "";
	
	/**
	 * Description of the a location
	 */
	private String keywords = "";
	
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
	 * The easy, simple, the way it should be constructor...you get it
	 * @param p The point for the location to be at
	 * @param name The location's name
	 */
	public Location(Point p, String name)
	{
		cord = p;
		this.name = name;
		ID = IDcount++;         // Get the current ID and increment
		aliases = new Vector<String>();
		//addAlias("Foo");
		//addAlias("Bar");
	}
	
	/**
	 * Generic location constructor.  Also assigns the ID# of the location
	 * @param x x coordinate for the location's Point field
	 * @param y y coordinate for the location's Point field
	 * @param passedName The name of the location
	 */
	public Location(int x, int y, String passedName)
	{
		this(new Point(x,y), passedName);
	}
	
	/**
	 * Sets the aliases vector to the passed in vector
	 * @param newVector The string vector to set
	 */
	public void setAliases(Vector <String> newVector)
	{
		aliases = newVector;
	}
	
	/**
	 * Returns a vector that it is the current aliases vector
	 * @return  The aliases vector
	 */
	public Vector<String> getAliases()
	{
		return(aliases);
	}
	
	/**
	 * Adds the passed string to the aliases vector.
	 * @param s String to add to the aliases vector.
	 */
	public void addAlias(String s)
	{
		aliases.add(s);
	}
	
	/**
	 * Remove the alias at passed index.
	 * @param index The index to remove at
	 * @return The alias the was removed, null if bad index passed.  
	 */
	public String removeAlias(int index)
	{
		try{
			return(aliases.remove(index));
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			System.err.println("Bad index was passed to " +
					"Location's removeAlias()");
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Sets the alias at passed index to passed string
	 * @param index The index to set at
	 * @param s String to set
	 */
	public void setAlias(int index, String s)
	{
		try{
			aliases.set(index, s);
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			System.err.println("Bad index was passed to " +
					"Location's setAlias()");
			e.printStackTrace();
		}
	}
	/**
	 * Gets the alias at the specified index
	 * @param index The index to get the alias at.
	 * @return The string at the specified index, else null.
	 */
	public String getAlias(int index)
	{
		try{
			return(aliases.get(index));
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			System.err.println("Bad index was passed to " +
					"Location's getAlias()");
			e.printStackTrace();
		}
		return null;
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
	
	/**
	 * Gets the name field
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name field
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Returns the building code the location
	 * @return The building code of the location
	 */
	public String getBuildingCode() {
		return buildingCode;
	}
	/**
	 * Sets the building code of the location
	 * @param buildingCode The building code of the location
	 */
	public void setBuildingCode(String buildingCode) {
		this.buildingCode = buildingCode;
	}
	/**
	 * Returns the string with the location descriptions
	 * @return keywords that apply to the location
	 */
	public String getKeywords() {
		return keywords;
	}
	/**
	 * Sets the location keywords
	 * @param keywords The keywords to apply
	 */
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
	
	/**
	 * Required by interface ComponentElement
	 * Returns an array of descriptions describing
	 * the elements to be modified by the ComponentEditor
	 */
	public String[] getComponentDescriptions()
	{
		String [] descriptions 
		= {"Name of Location @ (" + this.cord.x + ", " + this.cord.y + "), ID " + this.ID //Name
				, "ID", "Keywords", "canPassThrough", 
				"allowIntersections", "displayName", "Aliases"};
		return (descriptions);
	}
	
	/**
	 * Required by interface ComponentElement
	 * @param index The index to set at
	 * @retun String describing element at index
	 */
	public Object getElementValue(int index)
	{
		switch (index)
		{
		case 0:
			return getName();
		case 1:
			return getBuildingCode();
		case 2:
			return getKeywords();
		case 3:
			return new Boolean(isCanPassThrough());
		case 4:
			return new Boolean(isAllowIntersections());
		case 5:
			return new Boolean(isDisplayName());
		case 6:
			return getAliases();
		default:
			return "ERROR!";
		}
	}
	
	/**
	 * Required by interface ComponentElement
	 * @param value The value to set
	 * @param index the index to set at
	 */
	public String setElementValue(Object value, int index)
	{
		switch (index)
		{
		case 0:
			// Errror condition: setting name to empty string
			if(value.equals(""))
			{
				return "Error: Name string is empty!";
			}
			setName((String)value);
			break;
		case 1:
			setBuildingCode((String)value);
			break;
		case 2:
			setKeywords((String)value);
			break;
		case 3:
			setCanPassThrough(((Boolean)value).booleanValue());
			break;
		case 4:
			setAllowIntersections(((Boolean)value).booleanValue());
			break;
		case 5:
			setDisplayName(((Boolean)value).booleanValue());
			break;
		case 6:
			this.setAliases((Vector<String>)value);
			break;
		}
		return null;
	}
}

interface FieldEditor
{
	/**
	 * Gets the variable value of a variable identified by the specified index
	 * @param id The identifier for the variable
	 * @return The variable value as an object
	 */
	Object getVariableValue(int id);
	
	/**
	 * Set the value of a variable (choosing the variable using the passed id
	 * @param newVal The value to change to
	 * @param id The value to change to.
	 * @return error string if an error occured, else returns the empty string
	 */
	String setVariableValue(Object newVal, int id);
}

/**
 * Interface that defines what a EditorBox has in order to connect
 * to the EditorComponent.
 */
interface EditorBox
{
	/**
	 * Submit color
	 */
	final static Color SUBMIT_COLOR = Color.GREEN;
	/**
	 * Error color
	 */
	final static Color ERROR_COLOR  = Color.RED;
	/**
	 * Change color
	 */
	final static Color CHANGE_COLOR = Color.LIGHT_GRAY;
	/**
	 * Links the EditorBox to a specific variable of the ComponentEditor.
	 * @param parent The ComponentEditor to link to
	 * @param id for identification of the variable that can be used
	 * to get and set the variable in calls to parent.
	 */
	public void linkToVariable(FieldEditor parent, int id);
    
    /**
     * Links the EditorBox to the Window that contains it. This is used, e.g.,
     * if the implementing class changes its container size and needs to
     * repack the parent.
     * @param parent the Window that contains the EditorBox (JFrames and
     *      JDialogs are Windows)
     */
    public void linkToParent(Window parent);
    
	/**
	 * This method should save changes to the variable that the editor box
	 * controls.
	 * @return String of errors if any exits, empty string otherwise.
	 */
	public String saveChange();
}

/**
 * This class aids in the editing of the string fields of objects using an
 * editor.  It is extended from JPanel, allowing for the instantiation of
 * a JPanel object that contains the fields of an objects in JTextAreas.
 * Labels describing the boxes are also provided.
 * Any changes to the fields cause the field to be highlighted.
 * A submit button is provided for saving the fields of the object back 
 * into the object.  (which causes a second level of highlighting).  
 * @author David Lindquist
 */
class ComponentEditor extends JPanel implements FieldEditor
{
	final static long serialVersionUID = 1;
	private EditorBox [] boxes;
	private ComponentElement element;
	
	JButton submitButton;
	
	/**
	 * Initializes the Component Editor
	 * Takes a ComponentElement that is guaranteed to have methods
	 * that allow for the getting and setting of elements (and getting
	 * their descriptions).  
	 * @param passedElement The obedient object.
	 */
	public ComponentEditor(ComponentElement passedElement, Window parent)
	{
		super();
		this.element = passedElement;
		String [] descriptions = element.getComponentDescriptions();
		int numElements = descriptions.length;
		
		// Set boxes array to size
		boxes = new EditorBox[numElements];
		
		// Create the StringEditorBoxes
		for(int i = 0; i < numElements; i++)
		{
			if(element.getElementValue(i) instanceof String)
			{
				// instantiate the StringEditorBoxes
				boxes[i] = new StringEditorBox();
			}
			else if(element.getElementValue(i) instanceof Boolean)
			{
				// Instantiate the BooleanEditorBoxes
				boxes[i] = new BooleanEditorBox();
			}
			else if(element.getElementValue(i) instanceof Vector)
			{
				boxes[i] = new StringVectorEditorBox();
                boxes[i].linkToParent(parent);
			}
			else
			{
				System.err.println("We got an unknown type!" + element.getElementValue(i));
			}
			boxes[i].linkToVariable(this, i);
		}
		
		// create the submitButton
		JButton submit = new JButton("Submit changes");
		
		// Add listener to submit button
		submit.addActionListener(new SubmitButtonListener(this));
		
		// Set the layout type...all columns...description/element + submit
		// button
		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints constraint = new GridBagConstraints();
		setLayout(gridBag);
		
		constraint.gridwidth = GridBagConstraints.REMAINDER;
		
		
		// Assign the elements into the panel
		for(int i = 0; i < numElements; i++)
		{
			JLabel descript = new JLabel(descriptions[i]);
			if(element.getElementValue(i) instanceof Boolean)
				constraint.gridwidth = GridBagConstraints.RELATIVE;
				
			gridBag.setConstraints(descript, constraint);
			add(descript);  // add description
			
			constraint.gridwidth = GridBagConstraints.REMAINDER;
			
			try
			{
				JComponent c = (JComponent)boxes[i];
				gridBag.setConstraints(c, constraint);
				this.add(c);  // add the JTextBox
			}
			catch(ClassCastException e)
			{
				System.err.println("Class implementing EditorBox must be a " +
						"JComponent!  (But Java does not support multiple " +
						"inheritance)");
			}
		}
		// Add the submit button
		this.add(submit);
		gridBag.setConstraints(submit, constraint);
	}
	
	/**
	 * Set the value of a variable (choosing the variable using the passed id
	 * @param newVal The value to change to
	 * @param id The value to change to.
	 * @return error string if an error occured, else returns the empty string
	 */
	public String setVariableValue(Object newVal, int id)
	{
		repaint();
		// update value
		String possibleError = element.setElementValue(newVal, id);
		return possibleError;
	}
	
	/**
	 * Gets the variable value of a variable identified by the specified index
	 * @param id The identifier for the variable
	 * @return The variable value as an object
	 */
	public Object getVariableValue(int id)
	{	return element.getElementValue(id); }
	
	/**
	 * Saves all of the variables in the boxes.
	 * Loops through all boxes, sending saveChange requests
	 */
	public void saveVariables()
	{
		String result = "";
		for(EditorBox box : boxes)
			result += box.saveChange();
		if(!result.equals(""))
			System.err.println(result);
	}
	
	/**
	 * Response for the submit button
	 * Responds to an action by sending a saveVariables request to it's 
	 * initializing ComponentEditor.
	 */
	class SubmitButtonListener implements ActionListener
	{
		// The component to listen to
		private ComponentEditor elementToListen;
		/**
		 * Constructor for SubmitButtonListener...sets the component to listen
		 * to
		 * @param elementToListen The component to listen to.
		 */
		public SubmitButtonListener(ComponentEditor elementToListen)
		{
			this.elementToListen = elementToListen;
		}
		/**
		 * On action performed, send a saveVariables request.
		 * @param ActionEvent e An action
		 */
		public void actionPerformed(ActionEvent e)
		{
			// send request to save all variables in the panel
			elementToListen.saveVariables();
		}
	}
}

/**
 * This class is both an editorBox and a field editor!
 * It is an EditorBox for a Vector<String> object, while being a FieldEditor for 
 * all of the Strings in the object.
 */
class StringVectorEditorBox extends JPanel implements EditorBox, FieldEditor
{
	final static long serialVersionUID = 6; //removes warning
	private Vector<StringEditorBox> stringEditors = 
		new Vector<StringEditorBox>();
	private FieldEditor parent = null;
	private GridBagConstraints constraint;
	private GridBagLayout layout;
	int vectorId;
    
    private Window windowParent = null;
	/**
	 * Default constructor for StringVectorEditorBox
	 */
	public StringVectorEditorBox()
	{
		constraint =  new GridBagConstraints();
		layout = new GridBagLayout();
		setLayout(layout);
	}

	public void linkToVariable(FieldEditor parent, int id)
	{
		this.parent = parent;
		this.vectorId = id;

		//draw stuff
		regenerate();
	}
    
    public void linkToParent(Window parent){
        this.windowParent = parent;
    }

	/**
	 * Refreshes the box fully
	 */
	public void regenerate()
	{
		this.removeAll();
		stringEditors.clear();
		// Add button
		JButton addButton = new JButton("Add");
		add(addButton);
		
		Vector<String> initialVec = getParentVector();
		for(int i = 0; i < initialVec.size(); i++)
		{
			StringEditorBox box = new StringEditorBox(30);
			box.linkToVariable(this, i);
			stringEditors.add(i, box);
			
            JButton removeButton = new JButton("Remove");
            
			add(box);
			constraint.gridwidth = GridBagConstraints.RELATIVE;
			layout.setConstraints(box, constraint);
			add(removeButton);
			constraint.gridwidth = GridBagConstraints.REMAINDER;
			layout.setConstraints(removeButton, constraint);
			
			final StringEditorBox passedBox = box;
			removeButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e)
				{
					removeField(stringEditors.indexOf(passedBox));
					regenerate();
				}
			});
			repaint();
			
		}

		layout.setConstraints(addButton, constraint);
		addButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				addField();
				regenerate();
			}
		});
        
		for(Component c : getComponents())
		{
			//System.err.println(c.toString());
			c.setVisible(true);
			c.repaint();
		}
		this.setVisible(true);
		//repaint();
        setSize(400,400);
        
        // since we might have changed the size of the window,
        // repack our containing JFrame
        if(windowParent != null)
            windowParent.pack();
	}
	
	/**
	 * Adds a new Box, returns the box to allow it to be added
	 * to the display.
	 * @return The new box.
	 */
	public StringEditorBox addField()
	{
		// Get parent's vector of strings
		Vector<String> tempVect = getParentVector();
		// Add new element
		tempVect.add("<New>");
		// Create a new StringEditor box to use for the new field
		StringEditorBox newBox = new StringEditorBox();
		// Link up the variable (that we created earlier)
		newBox.linkToVariable(this, stringEditors.size());
		// Add the box to the stringEditor Boxes
		stringEditors.add(newBox);
		repaint();
		return(newBox);
	}

	/**
	 * Removes an element from "stored" vector at the passed index
	 * @param i The index of the element to remove.
	 */
	public void removeField(int i)
	{
		Vector<String> tempVect = getParentVector();
		tempVect.remove(i);
		stringEditors.remove(i);
		repaint();
	}

	/**
	 * Make all of the StringEditorBoxes save
	 * @return The empty string...always
	 */
	public String saveChange()
	{
		for(StringEditorBox box: stringEditors)
		{
			box.saveChange();
		}
		return "";
	}

	/**
	 * Gets the variable value of a variable identified by the specified index
	 * @param id The identifier for the variable
	 * @return The variable value as an object
	 */
	public Object getVariableValue(int id) {
		Vector<String> tempVect = getParentVector();
		return(tempVect.get(id));
	}
	
	/**
	 * Returns the parent's vector (Assumes that the vectorId was 
	 * correctly assigned).
	 * @return The Vector&lt;String&gt; that is associated with this
	 * object's ID.  
	 */
	public Vector<String> getParentVector()
	{
		return (Vector<String>)parent.getVariableValue(vectorId);
	}

	/**
	 * Set the value of a variable (choosing the variable using the passed id
	 * @param newVal The value to change to
	 * @param id The value to change to.
	 * @return error string if an error occured, else returns the empty string
	 */
	public String setVariableValue(Object newVal, int id) {
		Vector<String> tempVect = (Vector<String>)parent.getVariableValue(vectorId);
		tempVect.set(id,(String)newVal);
		return null;
	}
}
/**
 * This class provides a customized JTextArea that has methods to detect
 * if it's content has changed from a value identified by the box's ID.
 * Has the ability to save request a save on the value if it's different.
 * Also contains a method to reset it's color to a default value.  
 */
class StringEditorBox extends JTextArea implements EditorBox
{
	final static long serialVersionUID = 1;
	private FieldEditor parent;  // parent for sending messages
	//for background colors
	private int id; // The ID of the string element that the box is storing
	private Color defaultBackgroundColor;
	
	/**
	 * This is the constructor for the component box.
	 * Sets the size of the text box and adds its actionListener.
	 */
	public StringEditorBox(){
        this(50);
    }
    
    /**
     * This constructor allows the width of the JTextBox to be specified.
     * @param width the width of the text box, in characters
     */
    public StringEditorBox(int width)
	{
		// Create the JText area: height = 1, width = 60
		super(1, width);
		defaultBackgroundColor = getBackground();
		this.addKeyListener(new KeyListener(){
			/** Stub */
			public void keyPressed(KeyEvent e){}
			/**
			 * When a key is pressed...
			 * If there is a difference, highlight the color, else set it back
			 * to its default color.
			 * @param e The key event -- unused.
			 */
			public void keyReleased(KeyEvent e){
				if(changeInElement() != null)  // if not the same
					setBackground(EditorBox.CHANGE_COLOR); //highlight
				else
					resetColor(); //same, so remove highlight
			}
			/**  Stub */
			public void keyTyped(KeyEvent e){}
		});
	}
	
	/**
	 * Links the textbook to a certain variable (and it's parent
	 * to do method calls on the variable.  
	 * @param parent Allows for calls to set/get Variable values
	 * @param id integer ID: the array index of the passed variable, used to 
	 * determine what variable to get/set in calls to the parent.
	 */
	public void linkToVariable(FieldEditor parent, int id)
	{
		// Set the default text
		this.setText((String)parent.getVariableValue(id));
		this.parent = parent;
		this.id = id;
	}
    
    /** Stub */
    public void linkToParent(Window parent){}
	
	/**
	 * Checks to see if there are changes between the stored value and
	 * the displayed value.  (uses changeInBox()) If there are changes, 
	 * this method calls the it's setVariableValue() using it's id and
	 * performs the proper operation based on whether or not an error occured.
	 * The box will be highlighted by the submitColor if there was no error.
	 * The box will be highlighted by the errorColor if there was an error and
	 * the error string will be returned.  
	 * @return String passes back an error if one occured while  setting the 
	 * value
	 */
	public String saveChange()
	{
		// Status string describing change
		String error = "";
        // String currently displayed in text box
		String displayedVar;
        displayedVar = changeInElement();
        if(displayedVar != null)
        {
        	// Use setVariableValue() javadoc for info
        	error = parent.setVariableValue(displayedVar,id);
        	if(error == null)
        	{
        		this.setBackground(EditorBox.SUBMIT_COLOR);
        	}
        	else // error occured
        	{
        		if(error.equals(""))
        			error = "Error was an empty string!";
        		this.setText((String)parent.getVariableValue(id));
        		this.setBackground(EditorBox.ERROR_COLOR);
        		return error + "\n";
        	}
        }
    	return "";
	}
	
	/**
	 * Check for change between the text area and the stored variable
	 * @return The displayed string if there is a different, else returns null.
	 */
	public String changeInElement()
	{
		// Get the stored variable value (this is the old value)
		String storedVal = (String)parent.getVariableValue(id);
		// Get the displayed variable value (this is the new value)
		String displayedVal = this.getText();
		// If they're not the same
		if(!storedVal.equals(displayedVal))
		{
			// Return the displayed value
			return(displayedVal);
		}
		// Otherwise: return null
		return null;
	}
	
	/**
	 * Reset the color of the Text box
	 */
	public void resetColor()
	{ this.setBackground(defaultBackgroundColor); }
}


class BooleanEditorBox extends JCheckBox implements EditorBox
{
	final static long serialVersionUID = 5;
	FieldEditor parent;
	int id;
	private Color defaultBackgroundColor;
	/**
	 * Default constructor for the editor box...gets the background color.
	 */
	public BooleanEditorBox()
	{
		super();
		defaultBackgroundColor = getBackground();
	}
	
	public void linkToVariable(FieldEditor parent, int id)
	{
		this.parent = parent;
		this.id = id;
		setSelected(parentValue());
		this.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{	if(changeInElement())
					setBackground(EditorBox.CHANGE_COLOR);
				else
					setBackground(defaultBackgroundColor);
			}
		});
	}
    
    public void linkToParent(Window parent){}

	/**
	 * Gets the value of the variable
	 * @return The value of the variable.
	 */
	public boolean parentValue()
	{
		return ((Boolean)parent.getVariableValue(id)).booleanValue();
	}
	
	/**
	 * Saves changes to the variable.
	 * @return String passes back an error if one occured while setting the 
	 * value
	 */
	public String saveChange()
	{
		String error = "";
		if(changeInElement())
		{
			error =  parent.setVariableValue(isSelected(), id);
			if(error != null)  // if there is an error message
			{
				setBackground(EditorBox.ERROR_COLOR);
				return error;
			}
			else
				setBackground(EditorBox.SUBMIT_COLOR);
		}
		return "";
	}
	
	/**
	 * Determins if there is a change in the box.
	 * @return True if changed, else false.  
	 */
	public boolean changeInElement()
	{
		return(parentValue() != isSelected());
	}
}