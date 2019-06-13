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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.create.pointfrom2d;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.DialogComponentReferenceSystem;
import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;

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
public class CreatePointFrom2DNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New dialog pane for configuring the node. The dialog created here
	 * will show up when double clicking on a node in KNIME Analytics Platform.
	 */
    @SuppressWarnings("unchecked")
	protected CreatePointFrom2DNodeDialog() {
        super();
        
        createNewGroup("Coordinates");
     	addDialogComponent(new DialogComponentColumnNameSelection(
     			new SettingsModelString(
    					"colname_x", 
    					"X"), 
     			"column for x", 
     			0,
     			true,
     			DoubleValue.class
     			));
     	

     	addDialogComponent(new DialogComponentColumnNameSelection(
     			new SettingsModelString(
    					"colname_y", 
    					"Y"), 
     			"column for y", 
     			0,
     			true,
     			DoubleValue.class
     			));
     	
     	addDialogComponent(new DialogComponentBoolean(
     			new SettingsModelBoolean(
     					"delete_xy", 
     					Boolean.TRUE), 
     			"delete coordinate columns"
     			));
     	
        createNewGroup("Coordinate Reference System");

     	addDialogComponent(new DialogComponentReferenceSystem(
        		new SettingsModelString(
        				"crs", 
        				SpatialUtils.getDefaultCRSString()
        				),
        		"Coordinate Reference System"
        		));
    }
}

