package ch.res_ear.samthiriot.knime.shapefilesAsWKT.writeToShapefile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.geotools.data.DataStore;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.FileUtil;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.knime.shapefilesAsWKT.SpatialUtils;


/**
 * This is the model implementation of WriteWKTAsShapefile.
 * Stores the WKT data as a shapefile.
 *
 * @author Samuel Thiriot
 */
public class WriteWKTAsShapefileNodeModel extends NodeModel {
    

    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(WriteWKTAsShapefileNodeModel.class);
        
    private final SettingsModelString m_file = new SettingsModelString("filename", null);


    /**
     * Constructor for the node model.
     */
    protected WriteWKTAsShapefileNodeModel() {
    
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	final BufferedDataTable inputPopulation = inData[0];
    	

    	if (!SpatialUtils.hasGeometry(inputPopulation.getDataTableSpec()))
    		throw new IllegalArgumentException("the input table contains no spatial data (no column named "+SpatialUtils.GEOMETRY_COLUMN_NAME+")");
    	
    	if (!SpatialUtils.hasCRS(inputPopulation.getDataTableSpec()))
    		throw new IllegalArgumentException("the input table contains spatial data but no Coordinate Reference System");
    	    	
    	CoordinateReferenceSystem crsOrig = SpatialUtils.decodeCRS(inputPopulation.getSpec());
    	
    	//String filename = m_file.getStringValue();
    	URL url;
		try {
			
			url = FileUtil.toURL(m_file.getStringValue());
		} catch (InvalidPathException | MalformedURLException e2) {
			e2.printStackTrace();
			throw new InvalidSettingsException("unable to open URL "+m_file.getStringValue()+": "+e2.getMessage());
		}
        
    	File file = FileUtil.getFileFromURL(url);
        
    	// copy the input population into a datastore
    	exec.setMessage("storing entities");
        DataStore datastore = SpatialUtils.createDataStore(file, true);
        Runnable runnableSpatialize = SpatialUtils.decodeAsFeaturesRunnable(
        		inputPopulation, 
        		SpatialUtils.GEOMETRY_COLUMN_NAME, 
        		exec, 
        		datastore, 
        		"entities", 
        		crsOrig,
        		false
        		);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(runnableSpatialize);
        executor.shutdown();
        try {
        	// wait forever
        	executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
        	throw new RuntimeException(e);
        }
        datastore.dispose();
    	
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	
    	DataTableSpec specs = inSpecs[0];

    	if (m_file.getStringValue() == null)
    		throw new IllegalArgumentException("No filename was provided");
    	
    	// check the parameters include a filename
    	URL url;
		try {
			url = FileUtil.toURL(m_file.getStringValue());
		} catch (InvalidPathException | MalformedURLException e2) {
			e2.printStackTrace();
			throw new InvalidSettingsException("unable to open URL "+m_file.getStringValue()+": "+e2.getMessage());
		}
        
    	//File file = FileUtil.getFileFromURL(url);
    	//if (!file.canWrite())
    	//	throw new IllegalArgumentException("The destination file is not writable");
    	
    	// check the input table contains a geometry
    	
    	if (!SpatialUtils.hasGeometry(specs))
    		throw new IllegalArgumentException("the input table contains no spatial data (no column named "+SpatialUtils.GEOMETRY_COLUMN_NAME+")");
    	
    	if (!SpatialUtils.hasCRS(specs))
    		throw new IllegalArgumentException("the input table contains spatial data but no Coordinate Reference System");
    	
    	
    	
        return new DataTableSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        
    	m_file.saveSettingsTo(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        
    	m_file.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_file.validateSettings(settings);

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

