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
    public JTextField locationName = new JTextField(LOCATION_FIELD_WIDTH);
    public JLabel statusBar = new JLabel("Status Text");
    // For accessing the locationName text field


    /** Driver */
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
        
        SpringLayout spring = new SpringLayout();

        // Create flow Layout fow placing stuff
        pane.setLayout( spring );
        
        
        // img: private ImageIcon
        img = createImageIcon(filename);

        // See class listing below
        // Pass in ImageIcon, and maxUnitIncrement (10).
        ScrollablePicture ipanel = new ScrollablePicture(img, 10, this);

        // Create scroll, passing in the scrollable picture
        JScrollPane scroll = new JScrollPane(ipanel);
        scroll.setPreferredSize(new Dimension(600, 500));
        scroll.setWheelScrollingEnabled(false);

        pane.add(scroll);
        pane.add(locationName);
        pane.add(statusBar);
        
        // Add image with scroll
        spring.putConstraint(SpringLayout.WEST, scroll, 0, 
        		SpringLayout.WEST, pane);
        spring.putConstraint(SpringLayout.NORTH, scroll, 0, 
        		SpringLayout.NORTH, pane);
        // Add locationName to the pannel
        spring.putConstraint(SpringLayout.WEST, locationName, 0, 
        		SpringLayout.WEST, pane);
        spring.putConstraint(SpringLayout.NORTH, locationName, 0, 
        		SpringLayout.SOUTH, scroll);
        
        //Add statusbar
        spring.putConstraint(SpringLayout.WEST, statusBar, 0, 
        		SpringLayout.WEST, pane);
        spring.putConstraint(SpringLayout.NORTH, statusBar, 0, 
        		SpringLayout.SOUTH, locationName);
        
        // Wrap pane
        spring.putConstraint(SpringLayout.EAST, pane, 0, 
        		SpringLayout.EAST, scroll);
        spring.putConstraint(SpringLayout.SOUTH, pane, 0, 
        		SpringLayout.SOUTH, statusBar);
        locationName.setText("");
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
                    locations.add(new Location(x, y, parent.locationName.getText()));
                }

                parent.locationName.setText("");
                
                
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
        // Undo option
        else if(c ==  KeyEvent.VK_F1)
        {
            if(lines.size() >= 1)
            {
                lines.remove(lines.size() -1);
                repaint( getVisibleRect() );
                repaint();
            }
        }
        // Advance path
        else if(c == KeyEvent.VK_F3)
        {
            if(pathNumIndex < paths.size() - 1)
            {
                lines = (Vector)paths.get(++pathNumIndex);
                parent.statusBar.setText( statusBarText() );
                repaint();
            }
        }
        //      Previous path option
        else if(c == KeyEvent.VK_F2)
        {
            if(pathNumIndex >= 1){
                lines = (Vector)paths.get(--pathNumIndex);
                parent.statusBar.setText( statusBarText() );
                repaint();
            }
        }
        // New path option
        else if(c == KeyEvent.VK_F12)
        {
            pathNumIndex = paths.size();
            paths.add( new Vector() );
            lines = (Vector)paths.get(pathNumIndex);
            parent.statusBar.setText( statusBarText() );
            repaint();
        }
        // Save file
        else if(c ==  KeyEvent.VK_F7)
        {
            File pathOutputFile = new File("rawPathData.dat");
            File locationOutputFile = new File("rawLocationData.dat");
            
            try{
                ObjectOutputStream pathout = new ObjectOutputStream(
                        new FileOutputStream(pathOutputFile));
                pathout.writeObject(paths);
                
                ObjectOutputStream locout = new ObjectOutputStream(
                        new FileOutputStream(locationOutputFile));
                locout.writeObject(locations);
            }
            catch(IOException e){
                System.err.println("Output stream create failed!");
            }
            parent.statusBar.setText("Paths/locations written to file");
        }
        // Load file
        else if(c ==  KeyEvent.VK_F8)
        {
            File pathInputFile = new File("rawPathData.dat");
            File locationInputFile = new File("rawLocationData.dat");
            try{
                ObjectInputStream pathin = new ObjectInputStream(
                        new FileInputStream(pathInputFile));
                paths = (Vector)pathin.readObject();
                
                ObjectInputStream locin = new ObjectInputStream(
                        new FileInputStream(locationInputFile));
                locations = (Vector)locin.readObject();
                
                parent.statusBar.setText("Input read from file");

                
                // Active path
                if(pathNumIndex > paths.size() - 1)
                    pathNumIndex = paths.size();
                lines = (Vector)paths.get(pathNumIndex);
                repaint();
            }
            catch(Exception e){}
        }
        else{
            System.err.println("other key");
        }
    }

    public String statusBarText (){
    	return ( "Path Number: " + (pathNumIndex + 1) +
			",  Number of elements: " + lines.size() + 
			", Number of paths: " + paths.size() +
			((locations.size() > 0)
			        ? (", Locations: " + ((Location)locations.get(0)).name)
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

    /*
     * Function name: paintComponent()
     * Function prototype:
     *         public void paintComponent(Graphics g)
     * Description: override the paintComponent method so we can draw lines
     *              
     * Parameters: Graphics g
     * Side Effects: None.  
     * Error Conditions: None.
     * Return Value: None.  Void function.  
     */
    public void paintComponent(Graphics g){
        // first we paint ourselves according to our parent;
        // this should take care of drawing the actual image
        super.paintComponent(g);

        Rectangle visible = getVisibleRect();
        g.setColor(Color.RED);
        
        Point cur = null, prev = null;
        // draw all the dots that are VISIBLE
        for(int i = 0; i<paths.size(); i++)
        {
		    for(int j = 0; j < ( (Vector)paths.get(i) ).size(); j++){

		        
		        cur = (Point) ((Vector)paths.get(i)).get(j);
		
		        // if we have a "previous" dot to connect to, and at least ONE of
		        // the dots is visible, connect the two with a line
		        if(prev != null
		        && (visible.contains(cur) || visible.contains(prev)))
		            connectTheDots(g, prev, cur);
		        
		        // the last dot is the "active" one, so we print it in a different
		        // color
		        if(j == ((Vector)paths.get(i)).size()-1)
		        {
			        	g.setColor(Color.BLUE);
		        }   
		        else
		        {
		        	if(lines == (Vector)paths.get(i))
		        		g.setColor(Color.GREEN);
		        	else
			        	g.setColor(Color.RED);
		        }

		
		        // now draw the actual dot
		        if(visible.contains(cur))
		            drawDot(g, cur);
		        
		        prev = cur;
		    }
		    prev = null;
        }
    }

    
    /*
     * Function name: drawDot()
     * Function prototype:
     *     public void drawDot(Graphics g, Point p)
     * Description: draw a dot on the specified Graphics object at the specified Point
     * Parameters: Graphics g
     *             Point p
     * Side Effects: Draws a rectangle centered at the point p.
     *               Prints out error stream the point drawn.  
     * Error Conditions: None.
     * Return Value: None.  Void function.  
     */
    public void drawDot(Graphics g, Point p){
            g.fillRect( (int)p.getX()-2, (int)p.getY()-2, 5, 5 );
            //System.err.println("drawing @ ("
            //    + (int)p.getX() + ", " + (int)p.getY() + ")");
    }

    /*
     * Function name: connectTheDots()
     * Function prototype:
     *    public void connectTheDots(Graphics g, Point start, Point end)
     * Description: connect two points. this is really just a call to drawLine(), 
     *              but it's so ugly with all the method calls and casts, it's 
     *              worth it to have a little wrapper...
     * Parameters: Graphics g
     *             Point start (Starting point)
     *             Point end   (ending point)
     * Side Effects: Calls drawLine on the passed in graphics object.
     *               This draws a line between the start and the end point.
     * Error Conditions: None.
     * Return Value: None.  Void function.  
     */
    public void connectTheDots(Graphics g, Point start, Point end){
        g.drawLine( (int)start.getX(), (int)start.getY(),
                    (int)end.getX(), (int)end.getY() );
    }
}

/*
 * This is the location class!
 */
class Location implements Serializable
{
	public Point cord;
	public String name;
	public Location(int x, int y, String passedName)
	{
		cord = new Point(x,y);
		name = passedName;
	}
}