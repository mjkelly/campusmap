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
    	pathOp.convertPathPointsToPoints();
    	pathOp.writePoints();
    }
    
    
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
    			/*
    			// Set the inPoint
    			inPoint = pointInReadPaths(pathIndex, elementIndex);

    			// Set the next
    			next = pointInReadPaths(pathIndex, elementIndex + 1);
    			
    			// Get an intersecting location
    			atlocation = locationAtPoint(inPoint);
    			
    			//Constructor call
    			pathPoints.add(new PathPoint(inPoint, prev, next, atlocation));
    			
    			System.err.println("Added PathPoint: " + );
    			
    			// Set previous for next iteration
    			prev = inPoint;
    			*/
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
    	for(int pathIndex = 0; pathIndex < pathPoints.size(); pathIndex++)
    	{
    		PathPoint thisPoint = (PathPoint)pathPoints.get(pathIndex);
    		
    		if(thisPoint.location != null)
    		{
	    		System.err.println("Adding location: " + thisPoint.location.cord +
	    				", " + thisPoint.location.name);
	    		outLocations.add( thisPoint.location );
    		}
    		if(thisPoint.numConnectedPoints() == 0)
    		{
    			Vector thisPath = new Vector(2);
    			thisPath.add( thisPoint.point );
    			outPaths.add(thisPath);
    			continue;
    		}
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
    
    public void intersections ()
    {
    	PathPoint ap1;  // Active point 1
    	PathPoint ap2;  // Active point 2
    	PathPoint tp1;  // Test point 1
    	PathPoint tp2;  // Test point 2
    	// The intersect coordinates
    	double intersectX;
    	long intersectYTest;
    	long intersectYActive;
    	double activeSlope;
    	double testSlope;
    	// Loop through all PathPoints in Paths Vector
AP1:	for(int activeIndex1 = 0; activeIndex1<pathPoints.size(); 
    	activeIndex1++)
    	{
    		
    		ap1 = getPathPoint(activeIndex1);
    		
			// For all points that are connected to that active point 1 
			// PathPoint
    		for(int activeIndex2 = 0; 
			activeIndex2 < ap1.numConnectedPoints();
			activeIndex2++)
			{
				ap2 = ap1.getConnectedPathPoint(activeIndex2);
				activeSlope = getSlope(ap1, ap2);
				
				for(int testIndex1 = 0; testIndex1<pathPoints.size(); 
		    	testIndex1++)
		    	{
		    		tp1 = getPathPoint(testIndex1);
		    		
		    		if(tp1.equals(ap1) || tp1.equals(ap2))
		    			continue;
					// For all points that are connected to that active point 1 
					// PathPoint
					for(int testIndex2 = 0; 
					testIndex2 < tp1.numConnectedPoints();
					testIndex2++)
					{
						tp2 = tp1.getConnectedPathPoint(testIndex2);
						
			    		if(tp2.equals(ap1) || tp2.equals(ap2))
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
						
						
						
						intersectX =
							((  (activeSlope*(ap1.point.x) - ap1.point.y)
								- (testSlope*(tp1.point.x) - tp1.point.y))/
							   (activeSlope - testSlope));
						
						
						intersectYTest = (long)((testSlope)*(intersectX - tp1.point.x)
										+ tp1.point.y);
						intersectYActive = (long)((activeSlope)*(intersectX - ap1.point.x)
										+ ap1.point.y);
						

						if(intersectYActive == intersectYTest)
						{
							System.err.println("Points are equal!");
							System.err.println("(" + intersectX + ", " 
									+ intersectYTest + ")");
							

							
							PathPoint pi = new PathPoint(
											new Point( (int)intersectX,
											(int)intersectYTest),new Vector(2)
											,null );
							
							System.err.println(
									"pi: (" + pi.point.x + ", " + pi.point.y + ") ");
							
							// WE ARE HERE
							
							pathPoints.add(pi);
							intersectReplace(ap1, ap2, pi);
							intersectReplace(ap2, ap1, pi);
							intersectReplace(tp1, tp2, pi);
							intersectReplace(tp2, tp1, pi);
							
							System.err.println("pi, after: " + pi);
							
							System.err.println("ap1, after: " + ap1);
							System.err.println("ap2, after: " + ap2);
							System.err.println("tp1, after: " + tp1);
							System.err.println("tp2, after: " + tp2);
							
							continue AP1;
						}
						
					}
		    	}
				
				
			}
    	}
    }
    
    public boolean pointInSegments()
    {
    	
    }
    public void intersectReplace(PathPoint p1, PathPoint p2, PathPoint pi)
    {
    	//remove p2 from p1
    	p1.removeConnectedPoint(p2);
    	//add pi to p1
    	p1.addConnectedPoint(pi);
    	//add p1 to pi
    	pi.addConnectedPoint(p1);
    }
    
    public static double getSlope(PathPoint p1, PathPoint p2)
    {
    	if(p2.point.x == p1.point.x)
    	{
    		throw new RuntimeException("Non real slope");
    	}
    	return(((double)(p2.point.y - p1.point.y))/
    			((double)(p2.point.x - p1.point.x)));
    }
    
    public Point getPointAtPathPointsIndex(int index)
    {
    	return( ( (PathPoint)pathPoints.get( index ) ).point );
    }
    public PathPoint getPathPoint(int index)
    {
    	return((PathPoint)pathPoints.get( index ) );
    }
}

class PathPoint
{
	// Point where the PathPoint is located at
	Point point;
	// Points that are connected to the PathPoint
	Vector connectedPoints;
	// Any location associated with the point
	Location location;
	

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
	
	public void addConnectedPoint(PathPoint p)
	{
		if(p != null)
			connectedPoints.add(p);
	}
	
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
	 * Get the point at given index in Vector of connect points
	 * @param index Index to get point from
	 * @return point at given index
	 */
	public Point getConnectedPoint(int index){
		return((Point)getConnectedPathPoint(index).point);
	}
	
	public PathPoint getConnectedPathPoint(int index){
		return((PathPoint)connectedPoints.get(index));
	}
	
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