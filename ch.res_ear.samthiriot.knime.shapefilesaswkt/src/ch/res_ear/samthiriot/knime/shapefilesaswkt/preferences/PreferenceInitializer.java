package ch.res_ear.samthiriot.knime.shapefilesaswkt.preferences;

import java.io.File;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.ShapefileAsWKTNodePlugin;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		
		IPreferenceStore store = ShapefileAsWKTNodePlugin.getDefault().getPreferenceStore();

		{
			File f = new File(System.getProperty("java.io.tmpdir"));
			File f2 = new File(f, "spatial data cache");
			f2.mkdirs();
			
			store.setDefault(PreferenceConstants.P_DIRECTORY_CACHE, f2.getAbsolutePath());
		}
	}

}
