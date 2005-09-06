import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.*;


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
public class ComponentEditor extends JPanel implements FieldEditor
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
	 * @param parent The parent window of the component
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