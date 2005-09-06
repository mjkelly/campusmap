/**
 * Interface: Used for the ComponentEditor class
 * Note: Indexes in get and set MUST match
 *       The number of values must match the # of descriptions
 */
public interface ComponentElement
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