package ch.res_ear.samthiriot.knime.shapefilesaswkt.write.write_to_db;

import java.util.Arrays;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelPassword;
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
public class WriteWKTIntoDBNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring WriteWKTAsShapefile node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected WriteWKTIntoDBNodeDialog() {
        super();
        
        SettingsModelString ms = new SettingsModelString("dbtype", "postgis"); 
        DialogComponentStringSelection dbTypeComponent = new DialogComponentStringSelection(
        		ms, 
        		"database type", 
        		Arrays.asList("postgis", 
        				"h2", 
        				"mysql",
        				"geopkg"
        				)
        		);
        ms.setEnabled(false);
        this.addDialogComponent(dbTypeComponent);

        DialogComponentString hostComponent = new DialogComponentString(
        		new SettingsModelString("host", "127.0.0.1"),
        		"hostname"
        		);
        this.addDialogComponent(hostComponent);
        
        DialogComponentNumber portComponent = new DialogComponentNumber(
        		new SettingsModelIntegerBounded("port", 5432, 1, 65535), 
        		"port",
        		1
        		); 
        this.addDialogComponent(portComponent);
        
        DialogComponentString schemaComponent = new DialogComponentString(
        		new SettingsModelString("schema", "public"),
        		"schema"
        		); 
        this.addDialogComponent(schemaComponent);
        
        this.addDialogComponent(new DialogComponentString(
        		new SettingsModelString("database", "database"),
        		"database"
        		));
        this.addDialogComponent(new DialogComponentString(
        		new SettingsModelString("user", "postgres"),
        		"user"
        		));
        
        DialogComponentPasswordField passwordComponent = new DialogComponentPasswordField(
        		new SettingsModelPassword("password", WriteWKTIntoDBNodeModel.ENCRYPTION_KEY, "postgres"),
        		"password"
        		);
        this.addDialogComponent(passwordComponent);
        
        DialogComponentString layerComponent = new DialogComponentString(
        		new SettingsModelString("layer", "my_geometries"),
        		"layer to create"
        		);
        this.addDialogComponent(layerComponent);
        

        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean("check_written", true),
        		"check the results after writing"
        		));
        
        ms.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				final String type = ms.getStringValue();
				
				hostComponent.getModel().setEnabled(
						type.equals("postgis") || type.equals("mysql"));
				
				portComponent.getModel().setEnabled(
						type.equals("postgis") || type.equals("mysql"));
				
				schemaComponent.getModel().setEnabled(
						type.equals("postgis"));
				
				passwordComponent.getModel().setEnabled(
						type.equals("postgis") || type.equals("mysql"));
				
				/*layerComponent.getModel().setEnabled(
						type.equals("postgis")  || type.equals("mysql"))
						);*/
			}
		});
        
    }
}

