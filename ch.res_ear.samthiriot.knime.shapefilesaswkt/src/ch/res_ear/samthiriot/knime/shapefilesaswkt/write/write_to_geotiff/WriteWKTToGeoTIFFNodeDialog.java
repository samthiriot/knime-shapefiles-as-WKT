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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.write.write_to_geotiff;

import javax.swing.JFileChooser;

import org.knime.core.data.IntValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.DialogComponentReferenceSystem;

/**
 * This is an example implementation of the node dialog of the
 * "WriteWKTToKML" node.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}. In general, one can create an
 * arbitrary complex dialog using Java Swing.
 * 
 * @author Samuel Thiriot
 */
public class WriteWKTToGeoTIFFNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New dialog pane for configuring the node. The dialog created here
	 * will show up when double clicking on a node in KNIME Analytics Platform.
	 */
    @SuppressWarnings("unchecked")
	protected WriteWKTToGeoTIFFNodeDialog() {
        super();
        
        createNewGroup("File");

        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString("filename", null),
        		"GeoTIFF",
        		JFileChooser.SAVE_DIALOG,
        		false
        		));  
        
        createNewGroup("Bata");

        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString("coly", null),
        		"Y coordinate", 
        		0,
        		true,
        		IntValue.class
        		));
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString("colx", null),
        		"X coordinate", 
        		0,
        		true,
        		IntValue.class
        		));
        
        createNewGroup("Geo referencing");
        addDialogComponent(new DialogComponentReferenceSystem(
        		new SettingsModelString("CRS", "EPSG:4326"),
        		"Coordinate Reference System"
        		));
        
        addDialogComponent(new DialogComponentNumberEdit(
        		new SettingsModelDouble("upper left lon", 0.0),
        		"upper left (longitude = x)"
        		));
        addDialogComponent(new DialogComponentNumberEdit(
        		new SettingsModelDouble("upper left lat", 0.0),
        		"upper left (latitude = y)"
        		));
        addDialogComponent(new DialogComponentNumberEdit(
        		new SettingsModelDouble("bottom right lon", 0.0),
        		"bottom right (longitude = x)"
        		));
        addDialogComponent(new DialogComponentNumberEdit(
        		new SettingsModelDouble("bottom right lat", 0.0),
        		"bottom right (latitude = y)"
        		));
        
        createNewGroup("Compression");
        addDialogComponent(new DialogComponentStringSelection(
        		new SettingsModelString("m_compression", "DEFLATE"),
        		"compression method",
        		WriteWKTToGeoTIFFNodeModel.COMPRESSION_ALGOS
        		));
        
    }
}



