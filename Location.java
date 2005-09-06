import java.awt.Point;
import java.util.Vector;

/**
 * Location class.  Simply an object contatining a point (Point cord) and 
 * a description of the point (String name)
 **/
class Location implements ComponentElement
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