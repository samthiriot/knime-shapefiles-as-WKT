package ch.res_ear.samthiriot.knime.shapefilesAsWKT.readFromDB;

import java.util.Arrays;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelPassword;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is an example implementation of the node dialog of the
 * "ReadWKTFromDatabase" node.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}. In general, one can create an
 * arbitrary complex dialog using Java Swing.
 * 
 * @author Samuel Thiriot
 */
public class ReadWKTFromDatabaseNodeDialog extends DefaultNodeSettingsPane {

	
	/**
	 * New dialog pane for configuring the node. The dialog created here
	 * will show up when double clicking on a node in KNIME Analytics Platform.
	 */
    protected ReadWKTFromDatabaseNodeDialog() {
        super();
        
        // see 
        // see http://docs.geotools.org/stable/userguide/library/jdbc/spatialite.html
        // @see http://docs.geotools.org/stable/userguide/library/jdbc/teradata.html
        // @see http://docs.geotools.org/stable/userguide/library/jdbc/mysql.html
        // @see http://docs.geotools.org/stable/userguide/library/data/geopackage.html
        // @see http://docs.geotools.org/stable/userguide/library/jdbc/db2.html
        // @see http://docs.geotools.org/stable/userguide/library/jdbc/h2.html
        
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
        		new SettingsModelPassword("password", ReadWKTFromDatabaseNodeModel.ENCRYPTION_KEY, "postgres"),
        		"password"
        		);
        this.addDialogComponent(passwordComponent);
        
        ms.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				final String type = ms.getStringValue();
				
				hostComponent.getComponentPanel().setVisible(
						type.equals("postgis") || type.equals("mysql"));
				
				portComponent.getComponentPanel().setVisible(
						type.equals("postgis") || type.equals("mysql"));
				
				schemaComponent.getComponentPanel().setVisible(
						type.equals("postgis"));
				
				passwordComponent.getComponentPanel().setVisible(
						type.equals("postgis") || type.equals("mysql"));
				
			}
		});
        
    }
}

