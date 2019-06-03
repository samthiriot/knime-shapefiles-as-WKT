package ch.res_ear.samthiriot.knime.shapefilesaswkt.write.write_to_shapefile;

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

