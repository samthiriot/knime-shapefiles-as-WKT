package ch.res_ear.samthiriot.knime.shapefilesaswkt.reproject;

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

