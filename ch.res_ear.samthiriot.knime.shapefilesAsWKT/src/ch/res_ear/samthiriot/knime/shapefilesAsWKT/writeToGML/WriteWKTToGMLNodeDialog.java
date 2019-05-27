package ch.res_ear.samthiriot.knime.shapefilesAsWKT.writeToGML;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

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
public class WriteWKTToGMLNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New dialog pane for configuring the node. The dialog created here
	 * will show up when double clicking on a node in KNIME Analytics Platform.
	 */
    protected WriteWKTToGMLNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString("filename", null),
        		"KML",
        		JFileChooser.SAVE_DIALOG,
        		false
        		));  
        SettingsModelString modelVersion = new SettingsModelString("version", "GML v3");
        modelVersion.setEnabled(false);
        addDialogComponent(new DialogComponentStringSelection(
        		modelVersion,
        		"version",
        		//"GML v2",
        		"GML v3"
        		));

        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean("write_schema", true),
        		"write the schema"
        		));
    }
}

