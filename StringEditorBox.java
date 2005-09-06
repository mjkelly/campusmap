import java.awt.Color;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTextArea;

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