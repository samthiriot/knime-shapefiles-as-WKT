package ch.res_ear.samthiriot.knime.shapefilesaswkt.preferences;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.ShapefileAsWKTNodePlugin;
import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class SpatialPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public SpatialPreferencePage() {
		super(GRID);
		setPreferenceStore(ShapefileAsWKTNodePlugin.getDefault().getPreferenceStore());
		setDescription("Settings for the spatial processing features");
	}
	

	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		
		addField(new DirectoryFieldEditor(
				PreferenceConstants.P_DIRECTORY_CACHE, 
				"&Directory for cache storage", 
				getFieldEditorParent()));
		
		//DialogComponentButton clearCacheButton = new DialogComponentButton("clear cache");
		
	    Button clearCacheButton = new Button(getFieldEditorParent(), SWT.NONE);
	    clearCacheButton.setText("Clear cache");
	    clearCacheButton.addSelectionListener(new SelectionAdapter() {
            @Override 
            public void widgetSelected(final SelectionEvent e) {
            	clearCacheButton.setEnabled(false);
            	File cacheDir = SpatialUtils.getFileForCache();
            	try {
					FileUtils.deleteDirectory(cacheDir);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            	cacheDir.mkdirs();
            	clearCacheButton.setEnabled(true);
            }
		});
		

		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		setPreferenceStore(ShapefileAsWKTNodePlugin.getDefault().getPreferenceStore());
	}
	
}