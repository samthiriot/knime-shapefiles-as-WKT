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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.readFromShapefile;

import java.nio.charset.Charset;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import ch.res_ear.samthiriot.knime.dialogs.DialogComponentEncodingSelection;

/**
 * <code>NodeDialog</code> for the "ReadShapefileAsKML" Node.
 * Reads spatial features (geometries) from a <a href="https://en.wikipedia.org/wiki/Shapefile">shapefile</a>. Accepts any geometry type: points, lines, or polygons.  * n * nActual computation relies on the <a href="https://geotools.org/">geotools library</a>.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Samuel Thiriot (EIFER)
 */
public class ReadShapefileAsWKTNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring ReadShapefileAsKML node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected ReadShapefileAsWKTNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString("filename", null),
        		"shapefile",
        		".shp" // TODO other extensions?
        		));
                    
        addDialogComponent(new DialogComponentEncodingSelection(
        		new SettingsModelString("charset", Charset.defaultCharset().name()),
        		false
        		));
        

    }
}

