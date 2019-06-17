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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.create.geocoding.osm;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is an example implementation of the node dialog of the
 * "SpatialPropertySurface" node.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}. In general, one can create an
 * arbitrary complex dialog using Java Swing.
 * 
 * @author Samuel Thiriot
 */
public class GeocodingOSMNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New dialog pane for configuring the node. The dialog created here
	 * will show up when double clicking on a node in KNIME Analytics Platform.
	 */
    @SuppressWarnings("unchecked")
	protected GeocodingOSMNodeDialog() {
        super();
        
        createNewGroup("Address");
     	addDialogComponent(new DialogComponentColumnNameSelection(
     			new SettingsModelString(
     					"colname_address", 
     					"address"
     					), 
     			"column for address", 
     			0,
     			true,
     			StringValue.class
     			));
     	
     	createNewGroup("Open Street Map Nominatim service");
     	addDialogComponent(new DialogComponentString(
     			new SettingsModelString(
     					"email", 
     					""
     					), 
     			"Your email",
     			false,
     			60
     			));
     	addDialogComponent(new DialogComponentString(
     			new SettingsModelString(
     					"url", 
     					"https://nominatim.openstreetmap.org/"
     					), 
     			"service URL",
     			true,
     			60
     			));

        createNewGroup("Result");
        addDialogComponent(new DialogComponentBoolean(
     			new SettingsModelBoolean(
     					"full geometries", 
     					Boolean.FALSE
     					), 
     			"Fetch full geometries"
     			));
     	
     
    }
}

