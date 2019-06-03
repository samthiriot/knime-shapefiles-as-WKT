package ch.res_ear.samthiriot.knime.shapefilesaswkt.write.write_to_kml;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
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
public class WriteWKTToKMLNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New dialog pane for configuring the node. The dialog created here
	 * will show up when double clicking on a node in KNIME Analytics Platform.
	 */
    protected WriteWKTToKMLNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString("filename", null),
        		"KML",
        		JFileChooser.SAVE_DIALOG,
        		false
        		));  
        
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean("removeNamespace", true), 
        		"remove KML namespace")
        		);
        
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean("useKMLv22", true), 
        		"write attributes and use KML specification v2.2")
        		);
    }
}

