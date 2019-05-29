package ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_gml;

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
public class ReadGMLAsWKTNodeDialog extends DefaultNodeSettingsPane {


    protected ReadGMLAsWKTNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString("filename", null),
        		"GML",
        		".gml" // TODO other extensions?
        		));

        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean("skip_standard", true),
        		"skip standard columns"
        		));
        
    }
}

