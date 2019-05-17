package ch.res_ear.samthiriot.knime.shapefilesAsWKT.writeToDB;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelPassword;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.knime.shapefilesAsWKT.DataTableToGeotoolsMapper;
import ch.res_ear.samthiriot.knime.shapefilesAsWKT.NodeWarningWriter;
import ch.res_ear.samthiriot.knime.shapefilesAsWKT.SpatialUtils;


/**
 * This is the model implementation of WriteWKTAsShapefile.
 * Stores the WKT data as a shapefile.
 *
 * @author Samuel Thiriot
 */
public class WriteWKTIntoDBNodeModel extends NodeModel {
    
	final static String ENCRYPTION_KEY = "KnimeWKT";

	protected SettingsModelString m_dbtype = new SettingsModelString("dbtype", "postgis");
	protected SettingsModelString m_host = new SettingsModelString("host", "127.0.0.1");
	protected SettingsModelIntegerBounded m_port = new SettingsModelIntegerBounded("port", 5432, 1, 65535);
	protected SettingsModelString m_schema = new SettingsModelString("schema", "public");
	protected SettingsModelString m_database = new SettingsModelString("database", "database");
	protected SettingsModelString m_user = new SettingsModelString("user", "postgres");
	protected SettingsModelString m_password = new SettingsModelPassword("password", ENCRYPTION_KEY, "postgres");
	protected SettingsModelString m_layer = new SettingsModelString("layer", "my_geometries");

    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(WriteWKTIntoDBNodeModel.class);
    
    /**
     * Count of entities to write at once
     */
    final static int BUFFER = 5000;
	
	

    /**
     * Constructor for the node model.
     */
    protected WriteWKTIntoDBNodeModel() {
    
        super(1, 0);
    }
    

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
	

	protected String getSchemaName(DataStore datastore) throws InvalidSettingsException {

		final String layer = m_layer.getStringValue();
		
		Set<String> typeNames = new HashSet<>();
		try {
			typeNames.addAll(Arrays.asList(datastore.getTypeNames()));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("error when trying to read the layers: "+e.getMessage(), e);
		}
		
		//if (!typeNames.contains(layer))
		//	throw new InvalidSettingsException("There is no layer named \""+layer+"\" in this datastore");
		
		return layer;
	}


	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		
		final String layer = m_layer.getStringValue();
	
		if (layer == null)
			throw new InvalidSettingsException("please select one layer to read");
	
		if (inSpecs.length < 1)
			throw new InvalidSettingsException("missing input table");
			
		if (!SpatialUtils.hasGeometry(inSpecs[0]))
			throw new InvalidSettingsException("the input table contains no WKT geometry");
		
		return new DataTableSpec[] {};
	}

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(
			    		final BufferedDataTable[] inData,
			            final ExecutionContext exec) throws Exception {

    	final BufferedDataTable inputPopulation = inData[0];
    	

    	if (!SpatialUtils.hasGeometry(inputPopulation.getDataTableSpec()))
    		throw new IllegalArgumentException("the input table contains no spatial data (no column named "+SpatialUtils.GEOMETRY_COLUMN_NAME+")");
    	
    	if (!SpatialUtils.hasCRS(inputPopulation.getDataTableSpec()))
    		throw new IllegalArgumentException("the input table contains spatial data but no Coordinate Reference System");
    	    	
    	CoordinateReferenceSystem crsOrig = SpatialUtils.decodeCRS(inputPopulation.getSpec());
    	
        
    	NodeWarningWriter warnings = new NodeWarningWriter(getLogger());

    	// open the resulting datastore
    	DataStore datastore = openDataStore(exec);

    	// copy the input population into a datastore
    	exec.setMessage("storing entities");
        
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(m_layer.getStringValue());
        builder.setCRS(crsOrig); 
        
        Class<?> geomClassToBeStored = SpatialUtils.detectGeometryClassFromData(	
        										inputPopulation, 
        										SpatialUtils.GEOMETRY_COLUMN_NAME);
        
        // add attributes in order
        builder.add(
        		"geom", //SpatialUtils.GEOMETRY_COLUMN_NAME,
        		geomClassToBeStored
        		);
        
        // create mappers
        List<DataTableToGeotoolsMapper> mappers = inputPopulation
        												.getDataTableSpec()
        												.stream()
        												.filter(colspec -> !SpatialUtils.GEOMETRY_COLUMN_NAME.equals((colspec.getName())))
        												.map(colspec -> new DataTableToGeotoolsMapper(
        														warnings, 
        														colspec))
        												.collect(Collectors.toList());
        // add those to the builder type
        mappers.forEach(mapper -> mapper.addAttributeForSpec(builder));
        
        
        // build the type
        final SimpleFeatureType type = builder.buildFeatureType();
        // get or create the type in the file store 
		try {
			datastore.getSchema(type.getName());
		} catch (IOException e) {
			datastore.createSchema(type);	
		}
		// retrieve it 
		SimpleFeatureSource featureSource = datastore.getFeatureSource(datastore.getNames().get(0));
        if (!(featureSource instanceof SimpleFeatureStore)) {
            throw new IllegalStateException("Modification not supported");
        }
        SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

        // identify the id of the geom column, that we will not use as a standard one
        final int idxColGeom = inputPopulation.getDataTableSpec().findColumnIndex(SpatialUtils.GEOMETRY_COLUMN_NAME);
		
        // prepare classes to create Geometries from WKT
        GeometryFactory geomFactory = JTSFactoryFinder.getGeometryFactory( null );
        WKTReader reader = new WKTReader(geomFactory);
        
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
        
        Transaction transaction = new DefaultTransaction();
        featureStore.setTransaction(transaction);
        
        // the buffer of spatial features to be added soon (it's quicker to add several lines than only one)
		List<SimpleFeature> toStore = new ArrayList<>(BUFFER);
		
        CloseableRowIterator itRow = inputPopulation.iterator();
        try {
	        int currentRow = 0;
	        while (itRow.hasNext()) {
	        	final DataRow row = itRow.next();
	        	
	        	// process the geom column
	        	final DataCell cellGeom = row.getCell(idxColGeom);
	        	if (cellGeom.isMissing()) {
	        		// no geometry
	        		continue; // skip lines without geom
	        	}
	        	try {
		
		        	Geometry geom = reader.read(cellGeom.toString());
		        	featureBuilder.add(geom);
	
				} catch (ParseException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
	        	
	        	int colId = 0;
	        	for (int i=0; i<row.getNumCells(); i++) {
	        		
	        		if (i == idxColGeom) {
	        			// skip the column with geom
	        		} else {
	        			// process as a standard column
	        			featureBuilder.add(mappers.get(colId++).getValue(row.getCell(i)));
	        		}
	        	}
	        	
	        	// build this feature
	            SimpleFeature feature = featureBuilder.buildFeature(row.getKey().getString());
	            // add this feature to the buffer
	            toStore.add(feature);
	            if (toStore.size() >= BUFFER) {
	        		exec.checkCanceled();
	        		getLogger().info("storing "+toStore.size()+" entities");
	            	featureStore.addFeatures( new ListFeatureCollection( type, toStore));
	            	toStore.clear();
	            	
	    	        transaction.commit();
	    	        transaction.close();
	    	        transaction = new DefaultTransaction();
	            }
	            
	            if (currentRow % 10 == 0) {
	        		exec.setProgress((double)currentRow / inputPopulation.size(), "processing row "+currentRow);
	        		exec.checkCanceled();
	            }
	            currentRow++;
	            
	        }
	
	
	        // store last lines
	        if (!toStore.isEmpty()) {
        		getLogger().info("storing "+toStore.size()+" entities");
	        	featureStore.addFeatures( new ListFeatureCollection( type, toStore));
	        }
	        
	    	getLogger().info("commiting changes to database");
	        transaction.commit();

	        exec.setProgress(1.0);
	        

        } finally {
        	if (itRow != null)
        		itRow.close();
        	
        	if (transaction != null) {
                try {
                	transaction.rollback();
                } catch (IOException doubleEeek) {
                    // rollback failed
                }
                transaction.close();
        	}
            // close datastore
            datastore.dispose();
        	
        }
        
        setWarningMessage(warnings.buildWarnings());

        return new BufferedDataTable[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
       
    	// nothing to do
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
    	// nothing to do

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
    	// nothing to do
    }


}

