/**
 * This interface defines the fundamental methods that a 
 * FieldEditor must have: The ability to get a variable, 
 * and the ability to set a variable.  The variable
 * to contain is done using an integer id.
 */
public interface FieldEditor
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