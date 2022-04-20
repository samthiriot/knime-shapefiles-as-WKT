/*******************************************************************************
 * Copyright (c) 2019 EIfER[1] (European Institute for Energy Research).
 * This program and the accompanying materials
 * are made available under the terms of the GNU GENERAL PUBLIC LICENSE
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Contributors:
 *     Samuel Thiriot - original version and contributions
 *******************************************************************************/
/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 *
 */
package ch.res_ear.samthiriot.knime.shapefilesaswkt;

import java.io.File;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.geotools.util.factory.Hints;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.BundleContext;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.preferences.PreferenceConstants;
import ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_geofabrik.GeofabrikUtils;

/**
 * This is the eclipse bundle activator.
 * Note: KNIME node developers probably won't have to do anything in here, 
 * as this class is only needed by the eclipse platform/plugin mechanism.
 * If you want to move/rename this file, make sure to change the plugin.xml
 * file in the project root directory accordingly.
 *
 * @author Samuel Thiriot (EIFER)
 */
public class ShapefileAsWKTNodePlugin extends Plugin {
    // The shared instance.
    private static ShapefileAsWKTNodePlugin plugin;

    public static final String KEY_PREFERENCE_STORE = "ch.res_ear.samthiriot.knime.shapefilesaswkt.preferences";
    
    /**
     * The constructor.
     */
    public ShapefileAsWKTNodePlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     * 
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be started
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        try {
        	Hints.putSystemDefault(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        } catch (RuntimeException e) {
        	NodeLogger
        		.getLogger(ShapefileAsWKTNodePlugin.class)
        		.warn(
        				"unable to define hints for the geotools library: "+e.getMessage(), 
        				e);
        }
        
        // update the list of geofabrik sources
        GeofabrikUtils.obtainListOfDataExtracts();
    }

    /**
     * This method is called when the plug-in is stopped.
     * 
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be stopped
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
     * Returns the shared instance.
     * 
     * @return Singleton instance of the Plugin
     */
    public static ShapefileAsWKTNodePlugin getDefault() {
        return plugin;
    }

	public IPreferenceStore getPreferenceStore() {
		//Preferences preferences = InstanceScope.INSTANCE.getNode(ShapefileAsWKTNodePlugin.KEY_PREFERENCE_STORE);
		
		IPreferenceStore prefs = new ScopedPreferenceStore(
				InstanceScope.INSTANCE, 
				KEY_PREFERENCE_STORE
				);
		{
			File f = new File(System.getProperty("java.io.tmpdir"));
			File f2 = new File(f, "spatial data cache");
			f2.mkdirs();
			
			prefs.setDefault(PreferenceConstants.P_DIRECTORY_CACHE, f2.getAbsolutePath());
		}
		return prefs;
	}

}

