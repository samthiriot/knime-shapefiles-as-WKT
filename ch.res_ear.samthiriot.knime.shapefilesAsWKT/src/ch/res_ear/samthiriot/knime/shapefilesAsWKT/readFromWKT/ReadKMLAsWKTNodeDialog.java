package ch.res_ear.samthiriot.knime.shapefilesAsWKT.readFromWKT;

import java.nio.charset.Charset;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import ch.res_ear.samthiriot.knime.dialogs.DialogComponentEncodingSelection;

/**
 * Dialog to configure this node.
 * 
 * @author Samuel Thiriot
 */
public class ReadKMLAsWKTNodeDialog extends DefaultNodeSettingsPane {


    protected ReadKMLAsWKTNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString("filename", null),
        		"KML",
        		".kml" // TODO other extensions?
        		));
                   

    }
}

