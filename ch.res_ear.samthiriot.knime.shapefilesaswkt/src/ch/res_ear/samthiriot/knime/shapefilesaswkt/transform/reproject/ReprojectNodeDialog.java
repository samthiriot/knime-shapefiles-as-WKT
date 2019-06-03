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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.transform.reproject;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.DialogComponentReferenceSystem;
import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;


/**
 * <code>NodeDialog</code> for the "Reproject" Node.
 * Reprojects a spatialized population
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Samuel Thiriot
 */
public class ReprojectNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring Reproject node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected ReprojectNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentReferenceSystem(
        		new SettingsModelString(ReprojectNodeModel.MODEL_KEY_CRS, SpatialUtils.getDefaultCRSString()),
        		"Coordinate Reference System"
        		));
    }
}

