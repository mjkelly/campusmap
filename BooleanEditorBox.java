import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

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