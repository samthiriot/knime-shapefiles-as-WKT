package ch.res_ear.samthiriot.knime.shapefilesAsWKT.readFromDB;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelPassword;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.CheckUtils;

import ch.res_ear.samthiriot.knime.shapefilesAsWKT.AbstractReadWKTFromDatastoreNodeModel;
import ch.res_ear.samthiriot.knime.shapefilesAsWKT.SpatialUtils;


/**
 * This is an example implementation of the node model of the
 * "ReadWKTFromDatabase" node.
 * 
 * This example node performs simple number formatting
 * ({@link String#format(String, Object...)}) using a user defined format string
 * on all double columns of its input table.
 *
 * @author Samuel Thiriot
 */
public class ReadWKTFromDatabaseNodeModel extends AbstractReadWKTFromDatastoreNodeModel {
    
	final static String ENCRYPTION_KEY = "KnimeWKT";
			
	protected SettingsModelString m_dbtype = new SettingsModelString("dbtype", "postgis");
	protected SettingsModelString m_host = new SettingsModelString("host", "127.0.0.1");
	protected SettingsModelIntegerBounded m_port = new SettingsModelIntegerBounded("port", 5432, 1, 65535);
	protected SettingsModelString m_schema = new SettingsModelString("schema", "public");
	protected SettingsModelString m_database = new SettingsModelString("database", "database");
	protected SettingsModelString m_user = new SettingsModelString("user", "postgres");
	protected SettingsModelString m_password = new SettingsModelPassword("password", ENCRYPTION_KEY, "postgres");
	protected SettingsModelString m_layer = new SettingsModelString("layer", null);


	/**
	 * Constructor for the node model.
	 */
	protected ReadWKTFromDatabaseNodeModel() {
		
		super();
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		
		final String layer = m_layer.getStringValue();
	
		if (layer == null)
			throw new InvalidSettingsException("please select one layer to read");
	
		if (inSpecs.length < 1)
			throw new InvalidSettingsException("missing input table");
			
		final String dbtype = m_dbtype.getStringValue();
		final String database = m_database.getStringValue();
		
		if (!SpatialUtils.hasGeometry(inSpecs[0]))
			throw new InvalidSettingsException("the input table contains no WKT geometry");
		
		if (dbtype.equals("h2") || dbtype.equals("geopkg") ) {
			CheckUtils.checkSourceFile(database);
			
			// ensure the file exists
			/*
			File f = new File(database);
			if (!f.exists())
				throw new InvalidSettingsException("For the database type "+dbtype+", the database field should contain the path to an existing file");
			if (!f.isFile())
				throw new InvalidSettingsException("For the database type "+dbtype+", the database field should contain the path to a file");
			if (!f.canRead())
				throw new InvalidSettingsException("The file "+database+" cannot be read; please check permissions");
			*/
		}
		
		return super.configure(inSpecs);
	}

	@Override
	protected DataStore openDataStore(ExecutionContext exec) throws InvalidSettingsException {

		// @see http://docs.geotools.org/stable/userguide/library/jdbc/postgis.html
        Map<String, Object> params = new HashMap<>();
        params.put("dbtype", 	m_dbtype.getStringValue());
        params.put("host", 		m_host.getStringValue());
        params.put("port",  	m_port.getIntValue());
        params.put("schema", 	m_schema.getStringValue());
        params.put("database", 	m_database.getStringValue());
        params.put("user", 		m_user.getStringValue());
        params.put("passwd", 	m_password.getStringValue());

        //params.put(PostgisDataStoreFactory.LOOSEBBOX, true );
        //params.put(PostgisDataStoreFactory.PREPARED_STATEMENTS, true );
        DataStore dataStore;
		try {
			final String dbg = "opening database: "+params.get("user")+"@"+params.get("host")+":"+params.get("port");
			if (exec != null) exec.setMessage(dbg);
	        getLogger().info(dbg);
	        dataStore = DataStoreFinder.getDataStore(params);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new InvalidSettingsException("Unable to open the url as a shape file: "+e1.getMessage());
		}

		return dataStore;
	}
	

	
	@Override
	protected String getSchemaName(DataStore datastore) throws InvalidSettingsException {

		final String layer = m_layer.getStringValue();
		
		Set<String> typeNames = new HashSet<>();
		try {
			typeNames.addAll(Arrays.asList(datastore.getTypeNames()));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("error when trying to read the layers: "+e.getMessage(), e);
		}
		
		if (!typeNames.contains(layer))
			throw new InvalidSettingsException("There is no layer named \""+layer+"\" in this datastore");
		
		return layer;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
	

		m_dbtype.saveSettingsTo(settings);
		m_host.saveSettingsTo(settings);
		m_port.saveSettingsTo(settings);
		m_schema.saveSettingsTo(settings);
		m_database.saveSettingsTo(settings);
		m_user.saveSettingsTo(settings);
		m_password.saveSettingsTo(settings);
		m_layer.saveSettingsTo(settings);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		
		m_dbtype.loadSettingsFrom(settings);
		m_host.loadSettingsFrom(settings);
		m_port.loadSettingsFrom(settings);
		m_schema.loadSettingsFrom(settings);
		m_database.loadSettingsFrom(settings);
		m_user.loadSettingsFrom(settings);
		m_password.loadSettingsFrom(settings);
		m_layer.loadSettingsFrom(settings);
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		
		m_dbtype.validateSettings(settings);
		m_host.validateSettings(settings);
		m_port.validateSettings(settings);
		m_schema.validateSettings(settings);
		m_database.validateSettings(settings);
		m_user.validateSettings(settings);
		m_password.validateSettings(settings);
		m_layer.validateSettings(settings);
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		
		// nothing to do
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		
		// nothing to do
	}

	@Override
	protected void reset() {
		
		// nothing to do
	}

}

