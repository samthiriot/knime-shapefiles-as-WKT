package ch.res_ear.samthiriot.knime.shapefilesAsWKT.view;

import java.awt.Color;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColorChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelColor;

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
        
        addDialogComponent(new DialogComponentColorChooser(
        		new SettingsModelColor("color1", Color.GRAY),
        		"color for entities 1 (top table)",
        		true));
        addDialogComponent(new DialogComponentColorChooser(
        		new SettingsModelColor("color2", Color.BLUE),
        		"color for entities 1 (bottom table)",
        		true));

    }
}

