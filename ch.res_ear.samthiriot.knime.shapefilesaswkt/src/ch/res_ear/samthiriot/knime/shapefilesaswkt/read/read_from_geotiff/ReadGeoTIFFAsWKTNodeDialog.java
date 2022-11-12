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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_geotiff;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Dialog to configure this node.
 * 
 * @author Samuel Thiriot
 */
public class ReadGeoTIFFAsWKTNodeDialog extends DefaultNodeSettingsPane {


    protected ReadGeoTIFFAsWKTNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString("filename", null),
        		"GeoTIFF",
        		".tif|.tiff" // TODO other extensions?
        		));
                   
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean("create col id", true),
        		"Create a column for ID"
        		));
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean("create col coords", true),
        		"Create columns with line/column"
        		));
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean("create col geom", true),
        		"Create a column for the geometry"
        		));
    }
}

