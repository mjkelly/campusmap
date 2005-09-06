import java.awt.Color;
import java.awt.Window;

/**
 * Interface that defines what a EditorBox has in order to connect
 * to the EditorComponent.
 */
interface EditorBox
{
	/**
	 * Submit color
	 */
	final static Color SUBMIT_COLOR = Color.GREEN;
	/**
	 * Error color
	 */
	final static Color ERROR_COLOR  = Color.RED;
	/**
	 * Change color
	 */
	final static Color CHANGE_COLOR = Color.LIGHT_GRAY;
	/**
	 * Links the EditorBox to a specific variable of the ComponentEditor.
	 * @param parent The ComponentEditor to link to
	 * @param id for identification of the variable that can be used
	 * to get and set the variable in calls to parent.
	 */
	public void linkToVariable(FieldEditor parent, int id);
    
    /**
     * Links the EditorBox to the Window that contains it. This is used, e.g.,
     * if the implementing class changes its container size and needs to
     * repack the parent.
     * @param parent the Window that contains the EditorBox (JFrames and
     *      JDialogs are Windows)
     */
    public void linkToParent(Window parent);
    
	/**
	 * This method should save changes to the variable that the editor box
	 * controls.
	 * @return String of errors if any exits, empty string otherwise.
	 */
	public String saveChange();
}