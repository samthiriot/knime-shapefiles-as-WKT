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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_geojson;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.DialogComponentReferenceSystem;

/**
 * Dialog to configure this node.
 * 
 * @author Samuel Thiriot
 */
public class ReadGeoJSONAsWKTNodeDialog extends DefaultNodeSettingsPane {


    protected ReadGeoJSONAsWKTNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString("filename", null),
        		"GeoJSON",
        		".json|.geojson", ".json", ".geojson"
        		));
                   

        addDialogComponent(new DialogComponentReferenceSystem(
        		new SettingsModelString("CRS", "EPSG:4326"),
        		"Coordinate Reference System"
        		));
    }
}

