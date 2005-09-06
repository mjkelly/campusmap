import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * This class is both an editorBox and a field editor!
 * It is an EditorBox for a Vector<String> object, while being a FieldEditor for 
 * all of the Strings in the object.
 */
class StringVectorEditorBox extends JPanel implements EditorBox, FieldEditor
{
	final static long serialVersionUID = 6; //removes warning
	private Vector<StringEditorBox> stringEditors = 
		new Vector<StringEditorBox>();
	private FieldEditor parent = null;
	private GridBagConstraints constraint;
	private GridBagLayout layout;
	int vectorId;
    
    private Window windowParent = null;
	/**
	 * Default constructor for StringVectorEditorBox
	 */
	public StringVectorEditorBox()
	{
		constraint =  new GridBagConstraints();
		layout = new GridBagLayout();
		setLayout(layout);
	}

	public void linkToVariable(FieldEditor parent, int id)
	{
		this.parent = parent;
		this.vectorId = id;

		//draw stuff
		regenerate();
	}
    
    public void linkToParent(Window parent){
        this.windowParent = parent;
    }

	/**
	 * Refreshes the box fully
	 */
	public void regenerate()
	{
		this.removeAll();
		stringEditors.clear();
		// Add button
		JButton addButton = new JButton("Add");
		add(addButton);
		
		Vector<String> initialVec = getParentVector();
		for(int i = 0; i < initialVec.size(); i++)
		{
			StringEditorBox box = new StringEditorBox(30);
			box.linkToVariable(this, i);
			stringEditors.add(i, box);
			
            JButton removeButton = new JButton("Remove");
            
			add(box);
			constraint.gridwidth = GridBagConstraints.RELATIVE;
			layout.setConstraints(box, constraint);
			add(removeButton);
			constraint.gridwidth = GridBagConstraints.REMAINDER;
			layout.setConstraints(removeButton, constraint);
			
			final StringEditorBox passedBox = box;
			removeButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e)
				{
					removeField(stringEditors.indexOf(passedBox));
					regenerate();
				}
			});
			repaint();
			
		}

		layout.setConstraints(addButton, constraint);
		addButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				addField();
				regenerate();
			}
		});
        
		for(Component c : getComponents())
		{
			//System.err.println(c.toString());
			c.setVisible(true);
			c.repaint();
		}
		this.setVisible(true);
		//repaint();
        setSize(400,400);
        
        // since we might have changed the size of the window,
        // repack our containing JFrame
        if(windowParent != null)
            windowParent.pack();
	}
	
	/**
	 * Adds a new Box, returns the box to allow it to be added
	 * to the display.
	 * @return The new box.
	 */
	public StringEditorBox addField()
	{
		// Get parent's vector of strings
		Vector<String> tempVect = getParentVector();
		// Add new element
		tempVect.add("<New>");
		// Create a new StringEditor box to use for the new field
		StringEditorBox newBox = new StringEditorBox();
		// Link up the variable (that we created earlier)
		newBox.linkToVariable(this, stringEditors.size());
		// Add the box to the stringEditor Boxes
		stringEditors.add(newBox);
		repaint();
		return(newBox);
	}

	/**
	 * Removes an element from "stored" vector at the passed index
	 * @param i The index of the element to remove.
	 */
	public void removeField(int i)
	{
		Vector<String> tempVect = getParentVector();
		tempVect.remove(i);
		stringEditors.remove(i);
		repaint();
	}

	/**
	 * Make all of the StringEditorBoxes save
	 * @return The empty string...always
	 */
	public String saveChange()
	{
		for(StringEditorBox box: stringEditors)
		{
			box.saveChange();
		}
		return "";
	}

	/**
	 * Gets the variable value of a variable identified by the specified index
	 * @param id The identifier for the variable
	 * @return The variable value as an object
	 */
	public Object getVariableValue(int id) {
		Vector<String> tempVect = getParentVector();
		return(tempVect.get(id));
	}
	
	/**
	 * Returns the parent's vector (Assumes that the vectorId was 
	 * correctly assigned).
	 * @return The Vector&lt;String&gt; that is associated with this
	 * object's ID.  
	 */
	public Vector<String> getParentVector()
	{
		return (Vector<String>)parent.getVariableValue(vectorId);
	}

	/**
	 * Set the value of a variable (choosing the variable using the passed id
	 * @param newVal The value to change to
	 * @param id The value to change to.
	 * @return error string if an error occured, else returns the empty string
	 */
	public String setVariableValue(Object newVal, int id) {
		Vector<String> tempVect = (Vector<String>)parent.getVariableValue(vectorId);
		tempVect.set(id,(String)newVal);
		return null;
	}
}
