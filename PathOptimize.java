/**
 * $Id$
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class PathOptimize
{
	// These fields only hold data from readData
    private Vector readPaths; // Vector of paths
    private Vector readLocations;  // Vector of locations (see class Locations)

    private Vector pathPoints;
    
    private Vector graphPoints;
    
    // These fields are for the vectors to be written out in a format
    // that can be read by ShowImage
    private Vector outPaths;
    private Vector outLocations;

    /** Driver */
    public static void main(String[] args)
    {
    	PathOptimize pathOp = new PathOptimize();
    	
    	pathOp.readPoints();
    	pathOp.convertPointsToPathPoints();
    	pathOp.condensePoints();
    	pathOp.intersections();
    	pathOp.convertPathPointsToGraphPoints();
    	pathOp.convertPathPointsToPoints();
    	pathOp.writePoints();
    }
    
    /**
     * Generic constructor. It just initializes all the member fields.
     */
    public PathOptimize()
    {
    	outLocations = new Vector(128);
    	outPaths = new Vector(128);
    	pathPoints = new Vector(128);
    	readLocations = new Vector(128);
    	readPaths = new Vector(128);
    }
    
    /**
     * Method readData,  loads the paths and locations fields from 
     * files.  
     */
    public void readPoints(){
    	boolean pathLoadSuccess = false;
    	boolean locationLoadSuccess = false;
		// Load files
		final String pathFileName = "rawPathDataS.dat";
		final String locFileName = "rawLocationDataS.dat";
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
	        readPaths = (Vector)pathin.readObject();
	        pathLoadSuccess = true;
	        pathin.close();
	    }
	    catch(FileNotFoundException e){
	    	System.err.println(pathNotFound);
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
	        readLocations = (Vector)locin.readObject();
	        locationLoadSuccess = true;
	        locin.close();
	    }
	    catch(FileNotFoundException e){
	    	System.err.println(locNotFound);
	    }
	    catch(Exception e){
	    	System.err.println("Error in opening \"" + locFileName + "\"!");
	    }
	}
    
    
    /**
	 * Convert a set of points and locations (from a ShowImage data file) to
	 * PathPoints.
	 */
    public void convertPointsToPathPoints()
    {
    	Point inPoint;
    	Location atlocation;
    	PathPoint thisPP;
    	PathPoint prevPP;
    	for(int pathIndex = 0; pathIndex < readPaths.size(); pathIndex++)
    	{
    		prevPP = null;
    		for(int elementIndex = 0;
    		elementIndex < ( (Vector)readPaths.get(pathIndex) ).size(); 
    		elementIndex++)
    		{
    			// get the (x,y) location of the new PathPoint
    			inPoint = pointInReadPaths(pathIndex, elementIndex);
    			
    			// check if there's a location associated with this point
    			atlocation = locationAtPoint(inPoint);
    			
    			// create the PathPoint itself
    			thisPP = new PathPoint(inPoint, new Vector(2), atlocation);
    			pathPoints.add(thisPP);
    			
    			if(prevPP != null){
    				thisPP.addConnectedPoint(prevPP);
    				prevPP.addConnectedPoint(thisPP);
    			}
    			
    			prevPP = thisPP;
    		}

    	}
    }
    
    /**
     * Convert a set of PathPoints to a (highly unoptimized) set of paths
     * that can be read by ShowImage.
     *
     */
    public void convertPathPointsToPoints()
    {
        // loop through all PathPoints
    	for(int pathIndex = 0; pathIndex < pathPoints.size(); pathIndex++)
    	{
    		PathPoint thisPoint = (PathPoint)pathPoints.get(pathIndex);
    		
    		// if this PathPoint is associated with a location, add it to
    		// the outgoing Locations vector
    		if(thisPoint.location != null)
    		{
	    		System.err.println("Adding location: " + thisPoint.location.cord +
	    				", " + thisPoint.location.name);
	    		outLocations.add( thisPoint.location );
    		}
    		
    		// if this PathPoint has no connections, just add it as a single
    		// one-point path, and move on.
    		if(thisPoint.numConnectedPoints() == 0)
    		{
    			Vector thisPath = new Vector(2);
    			thisPath.add( thisPoint.point );
    			outPaths.add(thisPath);
    			continue;
    		}
    		
    		// otherwise, loop through each of this PathPoint's connections
    		for(int conPoint = 0; conPoint < thisPoint.numConnectedPoints();
    				conPoint++)
    		{
    			// create a new vector to represent this path
    			// (which consists of only two points)
    			Vector thisPath = new Vector(2);
    			thisPath.add( thisPoint.point );
    			thisPath.add( thisPoint.getConnectedPoint(conPoint) );
    			
    			// add the new vector to the list of paths
    			outPaths.add(thisPath);
    		}
    	}
    }
    
    /**
     * Write ShowImage-style data to disk.
     */
    public void writePoints(){
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
            pathout.writeObject(outPaths);
            pathWriteSuccess = true;
            pathout.close();
        }
	    catch(FileNotFoundException e){
	    	System.err.println(pathNotFound);
	    }
	    catch(IOException e){
	    	System.err.println("Error in writing \"" + pathFileName + "\"!");
	    }
	    
        try{
            // Write out the locations vector
            ObjectOutputStream locout = new ObjectOutputStream(
                    new FileOutputStream(locationOutputFile));
            locout.writeObject(outLocations);
            locationWriteSuccess = true;
            locout.close();
        }
	    catch(FileNotFoundException e){
	    	System.err.println(locNotFound);
	    }
	    catch(IOException e){
	    	System.err.println("Error in writing \"" + locFileName + "\"!");
	    }
	    if(locationWriteSuccess && pathWriteSuccess){
	        // Set status
        }
    }
	
    /**
     * Return a Location (from readLocations) that corresponds with the given
     * Point (from readPaths).
     * @param pointToCompare Point to compare for locations
     * @return the Location associated with pointToCompare, or null if none
     * is found.
     */
    public Location locationAtPoint(Point pointToCompare)
    {
    	for(int locIndex = 0; locIndex < readLocations.size(); locIndex++){
			if ((getReadLocation(locIndex).cord).equals(pointToCompare))
			{
				return(getReadLocation(locIndex));
			}
    	}
    	return(null);
    }
    
    /**
     * Get the location at the given index in the readLocations Vector.
     * @param locIndex Index of Location to return
     * @return the Location at locIndex
     */
    public Location getReadLocation(int locIndex){
    	return((Location)readLocations.get(locIndex));
    }
    
    /**
     * Return the point at a given path index in a given path (specified by an
     * index).  
     * @param pathIndex The index determining the path to choose.
     * @param elementIndex The index determining the element in the path.
     * @return point at a given path index in a given path (specified by an
     * index).  
     */
    public Point pointInReadPaths(int pathIndex, int elementIndex)
    {
    	if(elementIndex <= ((Vector)readPaths.get(pathIndex)).size() - 1)
    	{
        	return((Point)((Vector)readPaths.get(pathIndex)).get(elementIndex));    	
    	}
    	else
    		return(null);

    }
    
    /**
     * Condense all overlapping PathPoints into single PathPoints with
     * appropriate links.
     */
    public void condensePoints ()
    {
    	PathPoint overlap;
    	PathPoint overlapConnection;
    	PathPoint active;
    	// Loop through all PathPoints in Paths Vector
    	for(int activeIndex = 0; activeIndex<pathPoints.size(); activeIndex++){
    		// Set active PathPoint (the one to check for overlaps)
    		active = getPathPoint(activeIndex);
    		
    		// Loop through all path points again to look for intersections
    		for(int overlapIndex = 0; overlapIndex < pathPoints.size();
    			overlapIndex++)
    		{
    			// Skip the active PathPoint (because's it's identical to
    			// itself.
    			if(activeIndex == overlapIndex)
    				continue;
    			
    			// If the active PathPoint intersects with a PathPoint, 
    			// then that PathPoint is an overlap PathPoint
    			if(getPointAtPathPointsIndex(activeIndex).equals(
    					getPointAtPathPointsIndex(overlapIndex)))
    			{
    				// Set overlap PathPoint (the one that is going to be
    				// removed
    				overlap = getPathPoint(overlapIndex);
    				// For all points that are connected to that overlap 
    				// PathPoint
    				for(int conPointIndex = 0; 
    						conPointIndex < overlap.numConnectedPoints();
    						conPointIndex++)
    				{
    					// Set the PathPoint that is connected to the 
    					// overlap PathPoint
    					overlapConnection = 
    						overlap.getConnectedPathPoint(conPointIndex);
    					
    					// Remove the overlap PathPoint from the PathPoint
    					// that is/was connected to the overlap PathPoint
    					overlapConnection.removeConnectedPoint(overlap);
    					
    					// Add the active (the PathPoint we are overwriting
    					// over the overlap PathPoint) to the PathPoint
    					// that was connected to the overlap PathPoint.
    					overlapConnection.addConnectedPoint(active);
    					
    					// Add the connected PathPoint to the active
    					// PathPoint
    					active.addConnectedPoint(overlapConnection);
    				}
    				
    				// Remove the overlapping PathPoint
    				pathPoints.remove(overlap);
    			}
    		}
    	}
    }
    
    /**
     * Find all intersections of connections from PathPoints, and add new,
     * connected PathPoints at these spots.
     */
    public void intersections ()
    {
    	PathPoint ap1;  // Active point 1
    	PathPoint ap2;  // Active point 2
    	PathPoint tp1;  // Test point 1
    	PathPoint tp2;  // Test point 2
    	
    	// The intersect coordinates
    	double intersectX;
    	double intersectYTest;
    	double intersectYActive;
    	double activeSlope;
    	double testSlope;
    	
    	// Loop through all PathPoints in Paths Vector
    	for(int activeIndex1 = 0; activeIndex1<pathPoints.size(); 
    	activeIndex1++)
    	{
    		// store the first point in our active line segment
    		ap1 = getPathPoint(activeIndex1);
    		
			// For all points that are connected to that active point 1 
			// PathPoint
    		
			// now loop through all the points, again
			for(int testIndex1 = 0; testIndex1<pathPoints.size(); 
	    		testIndex1++)
	    	{
			    // store the first point in the test line segment
	    		tp1 = getPathPoint(testIndex1);
	    		//If the test point and active point are the same
	    		// Get a new test point.  
	    		if(tp1.equals(ap1))
	    			continue;
    		
AP1:    		for(int activeIndex2 = 0; 
					activeIndex2 < ap1.numConnectedPoints();
					activeIndex2++)
				{
	
					// the second point in the active line segment
					ap2 = ap1.getConnectedPathPoint(activeIndex2);
					// if the test point as  the second active point,
					// skip it.  If the 2nd active point is the same
					// as the first, skip it.  
					if(tp1.equals(ap2) || ap1.equals(ap2))
						continue;
					
					// test if this line (ap1-ap2) represents a bridge, tunnel,
					// etc.
					if(ap1.noLink(ap2))
						continue;
					
					activeSlope = getSlope(ap1, ap2);
		    		

		    		
					// For all points that are connected to that test point 1 
					// PathPoint
					for(int testIndex2 = 0; 
						testIndex2 < tp1.numConnectedPoints();
						testIndex2++)
					{
					    // store the second point in the test line segment
						tp2 = tp1.getConnectedPathPoint(testIndex2);
						
						// if _this_ point is one of the active points, skip it
						// if _this_ point is the same as the first test point
						// skip!
			    		if(tp2.equals(ap1) || tp2.equals(ap2) 
			    				|| tp2.equals(tp1))
			    			continue;
						
			    		
			    		// test if this line (tp1-tp2) represents a bridge, tunnel,
						// etc.
						if(tp1.noLink(tp2))
							continue;
						
			    		// this is The Rectangle Test
			    		if(!rectangleTest(ap1, ap2, tp1, tp2))
			    			continue;
			    		
						System.err.print(
								"ap1: (" + ap1.point.x + ", " + ap1.point.y + ") ");
						
						System.err.println(
								";  ap2: (" + ap2.point.x + ", " + ap2.point.y + ") ");
						
						System.err.print(
								"tp1: (" + tp1.point.x + ", " + tp1.point.y + ") ");
						
						System.err.println(
								";  tp2: (" + tp2.point.x + ", " + tp2.point.y + ") ");
						
						System.err.println("Active slope: " + activeSlope);
			    		
						testSlope = getSlope(tp1, tp2);

						System.err.println("Test slope: " + testSlope);
						
						if(testSlope == activeSlope)
							continue;

						// calculate the possible X intersect coordinate
						intersectX =
							((  (activeSlope*(ap1.point.x) - ap1.point.y)
								- (testSlope*(tp1.point.x) - tp1.point.y))/
							   (activeSlope - testSlope));
						
						// calculate the two possible Y intersect coordinates
						intersectYTest = ((testSlope)
						        * (intersectX - tp1.point.x) + tp1.point.y);
						intersectYActive = ((activeSlope)
						        * (intersectX - ap1.point.x) + ap1.point.y);
						
						// if the two Y intersect coordinates are the same,
						// it's a real intersection
						if(Math.abs(intersectYActive-intersectYTest) < 0.00001)
						{
							System.err.println("Points are equal!");
							System.err.println("(" + intersectX + ", " 
									+ intersectYTest + ")");
							
							// (this is actually supposed to be "P-sub-i", not
                            // the Greek letter Pi.)
							// create a PathPoint in the location of the
							// potential intercept point
							PathPoint pi = new PathPoint(
							        new Point( (int)intersectX,
							                (int)intersectYTest),
							        new Vector(2), null );
							
							System.err.println(
									"pi: (" + pi.point.x + ", " + pi.point.y + ") ");
							
							// ensure that the potential point is actually on
							// both lines
							if(!pointInSegments(ap1, ap2, tp1, tp2, pi))
								continue;
							
							// congratulations, you've found an intercept!
							
							// add the new point to the main points Vector
							pathPoints.add(pi);
							
							// and integrate the new point with its neighbors
							twoWayIntersectReplace(ap1, ap2, pi);
							twoWayIntersectReplace(tp1, tp2, pi);
							
							System.err.println("pi, after: " + pi);
							
							System.err.println("ap1, after: " + ap1);
							System.err.println("ap2, after: " + ap2);
							System.err.println("tp1, after: " + tp1);
							System.err.println("tp2, after: " + tp2);
							
							// since we've found an intercept, abort this
							// entire test line, and move on to the next 
							// 'active point 2'
							continue AP1;
						}
						
					}
		    	}

			}
    	}
    }
    
    /**
     * Test if a given point is in the rectangle defined by two lines.
     * (If it is, it's _possible_ that the line is the intersect point of
     * the two lines. This function is used as one of the screening tests for
     * that.)
     * @param ap1 first point in first test line
     * @param ap2 second point in first test line
     * @param tp1 first point in the second test line
     * @param tp2 second point in the second test line
     * @param pi the point to test
     * @return true if pi is in the common area of the rectangles defined by 
     * the lines ap1-ap2 and tp1-tp2, false otherwise.
     */    
    public boolean pointInSegments(PathPoint ap1, PathPoint ap2, PathPoint tp1,
    		PathPoint tp2, PathPoint pi)
    {
    	Rectangle rec1 = createRectangle(ap1.point,ap2.point);
    	// if the point isn't in the first rectangle, it can't be in both
    	if(!rec1.contains(pi.point))
    		return(false);
    	
    	Rectangle rec2 = createRectangle(tp1.point, tp2.point);
    	// if the point isn't in the second rectangle, it can't be in both
    	if(!rec2.contains(pi.point))
    		return(false);
    	
    	// by this point, we've tested both rectangles
    	return(true);
    }
    
    /**
     * Test if the rectangles defined by the line segments ap1-ap2 and
     * tp1-tp2 intersect. If they don't, the two lines cannot possibly
     * intersect (but just because the rectangles intersect doesn't mean
     * the lines do).
     * @param ap1 first point in first test line
     * @param ap2 second point in first test line
     * @param tp1 first point in the second test line
     * @param tp2 second point in the second test line
     * @return
     */
    public boolean rectangleTest(PathPoint ap1, PathPoint ap2, PathPoint tp1,
    		PathPoint tp2)
    {
    	Rectangle rec1 = createRectangle(ap1.point,ap2.point);
    	Rectangle rec2 = createRectangle(tp1.point, tp2.point);
    	return(rec1.intersects(rec2));
    }
    
    /**
     * Create a Rectangle object defined by the two given points. Order
     * does not matter.
     * @param p1 A point defining one corner of the Rectangle
     * @param p2 A point defining the other corner of the Rectangle
     * @return A rectangle with corners at the given points.
     */
    public Rectangle createRectangle(Point p1, Point p2)
    {
    	int x = Math.min(p1.x, p2.x);
    	int y = Math.min(p1.y, p2.y);
    	int height = Math.abs(p2.y - p1.y);
    	int width = Math.abs(p2.x - p1.x);
    	return(new Rectangle( x, y, width + 1, height + 1));
    }

    /**
     * Call intersectReplace on both combinations of p1 and p2, to effect a 
     * full integration of pi into the line segment connecting p1 and p2.
     * 
     * @see intersectReplace
     * 
     * @param p1 One end of a line segment
     * @param p2 The other end of a line segment
     * @param pi A point on the line between p1 and p2
     */
    public void twoWayIntersectReplace(PathPoint p1, PathPoint p2,
            PathPoint pi)
    {
        intersectReplace(p1, p2, pi);
        intersectReplace(p2, p1, pi);
    }
    
    /**
     * Integrate one point (pi) into the line segment between two other points
     * (p1 and p2). This only connects the paths going between pi and p1. To
     * full integrate a node, this function must be called twice, swapping p1
     * and p2 on the second call.
     * 
     * @see twoWayIntersectReplace
     *
     * @param p1 One end of a line segment
     * @param p2 The other end of a line segment
     * @param pi A point on the line between p1 and p2
     */
    private void intersectReplace(PathPoint p1, PathPoint p2, PathPoint pi)
    {
    	//remove p2 from p1
    	p1.removeConnectedPoint(p2);
    	//add pi to p1
    	p1.addConnectedPoint(pi);
    	//add p1 to pi
    	pi.addConnectedPoint(p1);
    }
    
    /**
     * Get the slope between two points.
     * @param p1 The first point
     * @param p2 The second point
     * @return the slope between p1 and p2, as a double
     */
    public static double getSlope(PathPoint p1, PathPoint p2)
    {
    	if(p2.point.x == p1.point.x)
    	{
    		//throw new RuntimeException("Non real slope");
    	}
    	return(((double)(p2.point.y - p1.point.y))/
    			((double)(p2.point.x - p1.point.x)));
    }

    
    
    /**
     * Return the Point contained in the PathPoint at the given index in
     * the pathPoints vector.
     * @param index the index to check
     * @return the Point in the PathPoint at the index
     */
    public Point getPointAtPathPointsIndex(int index)
    {
    	return( ( (PathPoint)pathPoints.get( index ) ).point );
    }
    
    /**
     * Return the PathPoint at the given index in the pathPoints vector.
     * @param index the index to check
     * @return the PathPoint at the index
     */
    public PathPoint getPathPoint(int index)
    {
    	return((PathPoint)pathPoints.get( index ) );
    }
    
    
    
    
    public void convertPathPointsToGraphPoints()
    {
    	GraphPoint gp;
    	PathPoint curPP;
    	PathPoint graphPP;
    	Edge tempEdge;
    	
    	PathPoint prevPP, whereFrom;
    	
    	graphPoints = new Vector();
    	
    	// create a GraphPoint for every "significant" PathPoint
    	for(int ppIndex = 0; ppIndex < pathPoints.size(); ppIndex++)
    	{
    		if(getPathPoint(ppIndex).isGraphPoint())
    		{
    			// the constructor does the linking between the PathPoint
    			// and the new GraphPoint
    			gp = new GraphPoint(getPathPoint(ppIndex));
    			graphPoints.add(gp);
    		}
    	}
    	
    	for(int ppIndex = 0; ppIndex < pathPoints.size(); ppIndex++)
    	{
    		if(!getPathPoint(ppIndex).isGraphPoint())
    			continue;
    		
    		graphPP = getPathPoint(ppIndex);
    		
    		for(int conIndex = 0; conIndex < graphPP.numConnectedPoints();
    			conIndex++)
    		{
				tempEdge = new Edge();
				tempEdge.path.add(graphPP.point);
				tempEdge.endpt1 = graphPP.getGraphPoint();
				graphPP.getGraphPoint().edges.add(tempEdge);

    			curPP = getPathPoint(conIndex);
    			tempEdge.path.add(curPP.point);
				tempEdge.weight += graphPP.getWeight(curPP);
    			
				whereFrom = graphPP;
				prevPP = curPP;
				
				while( (curPP = curPP.pointTraversal(whereFrom)) != null )
				{
					tempEdge.path.add(curPP);
					tempEdge.weight += prevPP.getWeight(curPP);
					
					whereFrom = prevPP;
					prevPP = curPP;
				}
				
				tempEdge.endpt1 = prevPP.getGraphPoint();
				
    		}
    		
    	}
    	for(int graphPI = 0; graphPI < graphPoints.size(); graphPI++)
    	{
    		 System.out.println(((GraphPoint)graphPoints.get(graphPI)).toString());
    	}
    }
    
    
    
    
}

/**
 * A Point that contains a list of the PathPoints it is connected to, as well 
 * as an optional pointer to an associated Location.
 */
class PathPoint
{
	// Point where the PathPoint is located at
	Point point;
	// Points that are connected to the PathPoint
	Vector connectedPoints;
	// Any location associated with the point
	Location location;
	
	private GraphPoint graphPoint = null;

	/**
	 * @return Returns the graphPoint.
	 */
	public GraphPoint getGraphPoint() {
		return graphPoint;
	}
	/**
	 * @param graphPoint The graphPoint to set.
	 */
	public void setGraphPoint(GraphPoint graphPoint) {
		this.graphPoint = graphPoint;
	}
	
	/**
	 * Tests to see if two points have the same location.
	 * @param p
	 * @return
	 */
	public boolean equals(PathPoint p)
	{
		return( ( p.point ).equals( point ) );
	}
	
	
	/**
	 * Generic constructor
	 * @param inpoint
	 * @param inConnectPoints
	 * @param inLoc
	 */
	public PathPoint(Point inPoint, Vector inConnectPoints, Location inLoc){
		point = inPoint;
		connectedPoints = inConnectPoints;
		location =  inLoc;
	}
	
	/**
	 * Add the given point to this point's connectedPoints vector.
	 * @param p Point to add
	 */
	public void addConnectedPoint(PathPoint p)
	{
		if(p != null)
			connectedPoints.add(p);
	}
	
	/**
	 * Remove the given point from this point's connectedPoints vector.
	 * @param p Point to remove
	 * @return true if the point was was found and could be removed, false
	 * otherwise.
	 */
	public boolean removeConnectedPoint(PathPoint p)
	{
		return connectedPoints.remove(p);
	}
	
	/**
	 * Get the number of connected points
	 * @return number of connected points
	 */
	public int numConnectedPoints(){
		return(connectedPoints.size());
	}
	/**
	 * Get the point at given index in Vector of connected points
	 * @param index Index to get point from
	 * @return point at given index
	 */
	public Point getConnectedPoint(int index){
		return((Point)getConnectedPathPoint(index).point);
	}
	
	/**
	 * Get the PathPoint at given index in Vector of connected points.
	 * @param index Index to get PathPoint from
	 * @return point at given index
	 */
	public PathPoint getConnectedPathPoint(int index){
		return((PathPoint)connectedPoints.get(index));
	}
	
	public boolean noLink(PathPoint other)
	{
		return (
					location != null
				&&	other.location != null
				&&	location.name.equals("<nolink>")
				&&	other.location.name.equals("<nolink>")
				);
	}
	
	public boolean isGraphPoint()
	{
		if(numConnectedPoints() > 2 || numConnectedPoints() == 1)
			return(true);
		if(location != null && !location.name.equals("<nolink>"))
			return(true);
		return(false);
	}
	
	public PathPoint pointTraversal(PathPoint prevPoint)
	{
		if(isGraphPoint())
		{
			return(null);
		}
		if(prevPoint == getConnectedPathPoint(0))
			return(getConnectedPathPoint(1));
		else
			return(getConnectedPathPoint(0));
	}
	
	public double getWeight(PathPoint p)
	{
		return(point.distance(p.point));
	}
	
	/**
	 * Print this PathPoint's x/y coordinates, the name of its associated
	 * Location (if applicable), and the coordinates of any connections it has.
	 */
	public String toString(){
		String outStr = "(" + point.x + ", " + point.y + ") ";
		if(location != null)
			outStr += " [" + location.name + "]";
		for(int i = 0; i < numConnectedPoints(); i++){
			outStr += "\n\t --> (" + getConnectedPoint(i).x + ", " +
				getConnectedPoint(i).y + ")";
		}
		outStr += "\n";
		
		return outStr;
	}
}


class GraphPoint
{
	Point point;
	Vector edges;
	Location locLabel;
	
	public GraphPoint(PathPoint pp)
	{
		point = new Point(pp.point);
		pp.setGraphPoint(this);
		
		locLabel = pp.location;
		edges = new Vector();
	}
	
	public Edge getEdge(int index)
	{
		return( (Edge)edges.get( index ) );
	}
	public String getLocationName()
	{
		if(locLabel != null)
			return(locLabel.name + "\n");
		else
			return("");
	}
	public String toString()
	{
		String outStr = "GraphPoint @ (" + point.x + ", " + point.y + ")\n";
		outStr += getLocationName();
		outStr +=  edges.toString();
		return(outStr);	
	}
}

class Edge
{
	GraphPoint endpt1;
	GraphPoint endpt2;
	
	Vector path;  // Vector Points
	double weight = 0;
	
	public Edge()
	{
		path = new Vector();
		endpt1 = endpt2 = null;
	}
	
	public String toString()
	{
		String outStr = "End Point 1: " + endpt1 + "\nEnd Point 2: " +
		endpt2 + "\n";
		for(int i = 0; i < path.size(); i++)
		{
			outStr += "   -->" + getPointInPath(i).toString() + "\n";
		}
		outStr += "Total weight: " + weight + "\n";
		
		return(outStr);
	}
	
	public Point getPointInPath(int i)
	{
		return((Point)path.get(i));
	}
	
	public double getWeight()
	{
		return(weight);
	}
	
}