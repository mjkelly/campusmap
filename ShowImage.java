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
    
    public JScrollPane scroll;
    public JTextField locationName;
    public JLabel statusBar;
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

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // use a SpringLayout
        SpringLayout spring = new SpringLayout();
        pane.setLayout( spring );
        
        ImageIcon img = createImageIcon(filename);

        // create the other objects in the main window
        locationName = new JTextField("", LOCATION_FIELD_WIDTH);
        // this text is set in the ScrollablePicture constructor
        statusBar = new JLabel();
        // Pass in ImageIcon, maxUnitIncrement (10), and the parent window
        // (this).
        ScrollablePicture ipanel = new ScrollablePicture(img, 10, this);

        // Create the main window: a scroll pane with the new image panel
        scroll = new JScrollPane(ipanel);
        scroll.setPreferredSize(new Dimension(600, 500));
        scroll.setWheelScrollingEnabled(false);
        
        // Add all the objects to the main window's content pane
        pane.add(scroll);
        pane.add(locationName);
        pane.add(statusBar);
        
        // Everything that follows is layout/alignment stuff, with illustrations
        
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
        spring.putConstraint(SpringLayout.WEST, locationName, 0, 
                SpringLayout.WEST, pane);
        spring.putConstraint(SpringLayout.NORTH, locationName, 0, 
                SpringLayout.SOUTH, scroll);
        
        // | (locationName)
        // +-------------
        // |     ^^^
        // | <<< statusBar
        spring.putConstraint(SpringLayout.WEST, statusBar, 0, 
                SpringLayout.WEST, pane);
        spring.putConstraint(SpringLayout.NORTH, statusBar, 0, 
                SpringLayout.SOUTH, locationName);
        
        //           scroll | <<<
        // statusBar        |
        // -----------------+
        // ^^^
        spring.putConstraint(SpringLayout.EAST, pane, 0, 
                SpringLayout.EAST, scroll);
        spring.putConstraint(SpringLayout.SOUTH, pane, 0, 
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
                                                  MouseMotionListener, 
                                                  KeyListener{
    private int maxUnitIncrement = 5;
    private boolean missingPicture = false;
    private Vector lines; // keep track of lines we've drawn
    private Vector paths; // Vector of paths (super of lines).
    private Vector locations;
    private int pathNumIndex = 0; //Index of where we are  in the paths array.
    private ShowImage parent;
    
    // Constructor
    public ScrollablePicture(ImageIcon i, int maxUnitPassed, ShowImage newParent) {
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

        // we'll listen for our own key events
        addKeyListener(this);
        // ...but also need to make sure we can receive (key?) focus
        setFocusable(true);
        
        paths = new Vector(128);
        locations = new Vector(128);

        paths.add( new Vector() );
        lines = (Vector)paths.get(pathNumIndex);
        parent.statusBar.setText( statusBarText() );
        
        // save a reference to "this" so we can access it inside the
        // anonymous class
        final ScrollablePicture superThis = this;
        
        /* add the mouse-click handler */
        addMouseListener(new MouseAdapter(){
        	
            public void mouseClicked(MouseEvent e){
                int x = e.getX();
                int y = e.getY();

                
                if(SwingUtilities.isLeftMouseButton(e)) {
                    System.err.print("Left mouse click @ (" + x + ", " + y + ")");
                    // store the click location in a Point object
                    Point p = new Point(x, y);

                    // add this point to the list...
                    if(lines == null)
                    {
                    	lines = new Vector(128);
                    }
                    lines.add(p);
                    // and redraw immediately to see the changes
                    repaint( getVisibleRect() );
                    parent.statusBar.setText( statusBarText() );
                }
                else if(SwingUtilities.isRightMouseButton(e)) {
                    System.err.print("Right mouse click @ (" + x + ", " + y + ")");
                }
                else{
                    System.err.print("Other mouse click @ (" + x + ", " + y + ")");
                }
                System.err.println();

                if(!parent.locationName.getText().equals(""))
                {
                    System.err.println(parent.locationName.getText());
                    superThis.requestFocus();
                    locations.add(new Location(x, y, parent.locationName.getText()));
                }

                parent.locationName.setText("");
                parent.statusBar.setText( statusBarText() );
                
            }
        });

    }

    // for KeyListener
    public void keyPressed(KeyEvent e){ handleKey(e); }
    public void keyReleased(KeyEvent e){ }
    public void keyTyped(KeyEvent e){ handleKey(e); }

    // this actually handles the key events
    private void handleKey(KeyEvent k){
        
        int c = k.getKeyCode();
        
        // Misc leftoovers from testing...
        if(c == KeyEvent.VK_LEFT){
            System.err.println("left");
        }
        else if(c == KeyEvent.VK_RIGHT){
            System.err.println("right");
        }
        else if(c == KeyEvent.VK_UP){
            System.err.println("up");
        }
        else if(c == KeyEvent.VK_DOWN){
            System.err.println("down");
        }
        
        
        // F1: Erase last line & point
        else if(c ==  KeyEvent.VK_F1)
        {
        	// Only erase if there is a point to erase
            if(lines.size() >= 1)
            {
            	// Remove the last element
                lines.remove(lines.size() -1);
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
                // Set statusBar
                parent.statusBar.setText( statusBarText() );
                repaint();
            }
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
            // Status bar
            parent.statusBar.setText( statusBarText() );
            repaint();
        }
        // F7: Save data into files
        else if(c ==  KeyEvent.VK_F7)
        {
        	// Define output files for paths and locations.
            File pathOutputFile = new File("rawPathData.dat");
            File locationOutputFile = new File("rawLocationData.dat");
            
            try{
            	// Write out the paths vector
                ObjectOutputStream pathout = new ObjectOutputStream(
                        new FileOutputStream(pathOutputFile));
                pathout.writeObject(paths);
                
                // Write out the locations vector
                ObjectOutputStream locout = new ObjectOutputStream(
                        new FileOutputStream(locationOutputFile));
                locout.writeObject(locations);
            }
            catch(IOException e){
                System.err.println("Output stream create failed!");
            }
            // Set status
            parent.statusBar.setText("Paths/locations written to file");
        }
        /*
         * F8: load/read from files.  
         */
        else if(c ==  KeyEvent.VK_F8)
        {
        	// Define input files for paths and locations.
            File pathsInputFile = new File("rawPathData.dat");
            File locationsInputFile = new File("rawLocationData.dat");
            try{
            	// Get the paths vector
                ObjectInputStream pathin = new ObjectInputStream(
                        new FileInputStream(pathsInputFile));
                paths = (Vector)pathin.readObject();
                
                // Get the locations vector
                ObjectInputStream locin = new ObjectInputStream(
                        new FileInputStream(locationsInputFile));
                locations = (Vector)locin.readObject();
                
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
            // Catch ANY exceptions from the above try block
            catch(Exception e){
            	System.err.println("Error in reading path or" +
            			"location file");
            }
        }
        // Take a wild gusss :)
        else{
            System.err.println("I'm sorry, this key does not have a" +
            		"have a defined option");
        }
    }

    /**
     * Return the status bar text
     * Write: Current path number in focus, number of elements in current
     *        path, number of paths, and any location string associated
     *        with the current in focus point.  
     **/
    public String statusBarText (){
    	return ( "Path Number: " + (pathNumIndex + 1) +
			",  Number of elements: " + lines.size() + 
			", Number of paths: " + paths.size() +
			((locations.size() > 0)
			        ? (", Locations: " + 
			        		((Location)locations.get(pathNumIndex)).name)
			        : "")
			);
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
		    	// path from paths Vector, caste to a Vector (as it should be)
		    	// and then get the point in the vector & cast it to a Point.
		        cur = (Point) ((Vector)paths.get(pathsIndex)).get(pointInPath);
		
		        // if we have a "previous" dot to connect to, and at least
		        // ONE of the dots is visible, connect the two with a line
		        if(prev != null 
		        		&& (visible.contains(cur) || visible.contains(prev))){
		            connectTheDots(g, prev, cur);
		        }

		        
		        // the last dot is the "active" one, so we print it in a
		        // different color
		        if(pointInPath == ((Vector)paths.get(pathsIndex)).size()-1)
		        {
		        	g.setColor(LASTPLACED_DOT);
		        }
		        // Else, it's at's a previously placed dot
		        else
		        {
		        	// If the path that we're drawing is the current one in
		        	if(lines == (Vector)paths.get(pathsIndex))
		        		g.setColor(IN_FOCUS_PATH);
		        	else
			        	g.setColor(OUT_OF_FOCUS_PATH);
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
	}
}