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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.writeToShapefile;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "WriteWKTAsShapefile" Node.
 * Stores the WKT data as a shapefile.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Samuel Thiriot
 */
public class WriteWKTAsShapefileNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring WriteWKTAsShapefile node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected WriteWKTAsShapefileNodeDialog() {
        super();
        

        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString("filename", null),
        		"shapefile",
        		JFileChooser.SAVE_DIALOG,
        		false
        		));  
    }
}

