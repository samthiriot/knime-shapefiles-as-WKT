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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_geofabrik;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.swt.widgets.Display;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is an example implementation of the node dialog of the
 * "ReadWKTFromDatabase" node.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}. In general, one can create an
 * arbitrary complex dialog using Java Swing.
 * 
 * @author Samuel Thiriot
 */
public class ReadWKTFromGeofabrikNodeDialog extends DefaultNodeSettingsPane {

	
	protected final DialogComponentStringSelection dialogZone;
	protected final SettingsModelString m_nameToLoad;
	
	public static final String DEFAULT_ZONE = "Europe/Germany/Baden-Württemberg/Regierungsbezirk Karlsruhe";
	
	/**
	 * New dialog pane for configuring the node. The dialog created here
	 * will show up when double clicking on a node in KNIME Analytics Platform.
	 */
    protected ReadWKTFromGeofabrikNodeDialog() {
        super();
        
        {
	        m_nameToLoad = new SettingsModelString("name_to_load", DEFAULT_ZONE);
	        m_nameToLoad.setEnabled(false);
	        
	        dialogZone = new DialogComponentStringSelection(
	        		m_nameToLoad,
	        		"Zone to load",
	        		Arrays.asList("loading...                                                  ")
	        		);
	        dialogZone.setEnabled(false);
	        dialogZone.setToolTipText("loading...");
	        addDialogComponent(dialogZone);
        }
        
        {
	        SettingsModelString m_layerToLoad = new SettingsModelString(
	        		"layer_to_load", 
	        		"buildings (Building outlines)");
	        
	        Collection<String> layers = new LinkedList<>(Arrays.asList(
	        		"buildings (Building outlines)",
	        		"landuse (Forests, residential areas, industrial areas,...)",
	        		"natural (Natural features)",
	        		"places (Cities, towns, suburbs, villages,...)",
	        		"pofw (Places of worship such as churches, mosques, ...)",
	        		"pois (Points of Interest)",
	        		"railways (Railway, subways, light rail, trams, ...)",
	        		"roads (Roads, tracks, paths, ...)",
	        		"traffic",
	        		"transport (Parking lots, petrol (gas) stations, ...)",
	        		"water (Lakes, ...)",
	        		"waterways (Rivers, canals, streams, ...)"
	        		));
	        addDialogComponent(new DialogComponentStringSelection(
	        		m_layerToLoad,
	        		"layer to load",
	        		layers
	        		));
        }

        // load the list of zone names
       GeofabrikUtils
       		.obtainListOfDataExtracts()
       		.thenAccept(zone2url -> {
       			System.out.println("got list");
       			Collection<String> names = zone2url.keySet();
       			Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						System.out.println("update list");
						dialogZone.replaceListItems(names, DEFAULT_ZONE);
		       			dialogZone.setEnabled(true);
		    	        dialogZone.setToolTipText(null);

					}
				});
       		});
        //Collection<String> names = GeofabrikUtils.fetchListOfDataExtracts().keySet();
        
    }
}

