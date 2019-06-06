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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.view;

import java.awt.Color;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColorChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelColor;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "DisplaySpatialPopulation" Node.
 * View the spatial population on a map.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Samuel Thiriot
 */
public class DisplaySpatialPopulationNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring DisplaySpatialPopulation node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected DisplaySpatialPopulationNodeDialog() {
        super();
        
        createNewGroup("top table");
        addDialogComponent(new DialogComponentColorChooser(
        		new SettingsModelColor("color1", Color.GRAY),
        		"default color",
        		true));
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDoubleBounded("opacity1", 0.7, 0.0, 1.0),
        		"opacity",
        		0.1));
        
        createNewGroup("bottom table");
        addDialogComponent(new DialogComponentColorChooser(
        		new SettingsModelColor("color2", Color.BLUE),
        		"default color",
        		true));
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDoubleBounded("opacity2", 0.5, 0.0, 1.0),
        		"opacity",
        		0.1));
        
        createNewGroup("Overlay");
        addDialogComponent(new DialogComponentStringSelection(
        	    new SettingsModelString("url wms", "https://ows.terrestris.de/osm-gray/service"),
        	    "WMS server for overlay", 
        	    "https://ows.terrestris.de/osm/service",
        	    //"http://ows.terrestris.de/osm/service",
        	    "https://ows.terrestris.de/osm-gray/service",
        	    "http://ows.mundialis.de/services/service", 
        	    "http://maps.heigit.org/osm-wms/service"
        	    ));

    }
}

