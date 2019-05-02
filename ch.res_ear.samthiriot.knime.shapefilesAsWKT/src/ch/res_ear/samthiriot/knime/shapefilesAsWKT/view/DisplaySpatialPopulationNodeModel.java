package ch.res_ear.samthiriot.knime.shapefilesAsWKT.view;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
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
import org.knime.core.node.defaultnodesettings.SettingsModelColor;
import org.knime.core.node.port.PortType;
import org.knime.core.util.FileUtil;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.knime.shapefilesAsWKT.SpatialUtils;



/**
 * This is the model implementation of DisplaySpatialPopulation.
 * View the spatial population on a map.
 *
 * @author Samuel Thiriot
 */
public class DisplaySpatialPopulationNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger(DisplaySpatialPopulationNodeModel.class);
        
    protected DataStore datastore1 = null;
    protected File tmpFile1 = null;
    
    protected DataStore datastore2 = null;
    protected File tmpFile2 = null;

    protected SettingsModelColor m_color1 = new SettingsModelColor("color1", Color.GRAY);
    protected SettingsModelColor m_color2 = new SettingsModelColor("color2", Color.BLUE);
    
    
    /**
     * Constructor for the node model.
     */
    protected DisplaySpatialPopulationNodeModel() {

    	super(
    			new PortType[] { BufferedDataTable.TYPE, BufferedDataTable.TYPE_OPTIONAL },
    			new PortType[] { }
    			);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	datastore1 = null;
    	datastore2 = null;
    			
    	final BufferedDataTable inputPopulation1 = inData[0];
    	
    	final BufferedDataTable inputPopulation2 = inData[1];

    	long total1 = inputPopulation1.size();
    	long total2 = inputPopulation2 != null ? inputPopulation2.size() : 0;
    	double totaltotal = total1+total2;
    			
    	// create progress monitors
    	ExecutionMonitor progress1 = exec.createSubProgress(total1/totaltotal);
    	ExecutionMonitor progress2 = exec.createSubProgress(total2/totaltotal);
    	

    	if (!SpatialUtils.hasGeometry(inputPopulation1.getDataTableSpec()))
    		throw new IllegalArgumentException("the input table 1 contains no spatial data (no column named "+SpatialUtils.GEOMETRY_COLUMN_NAME+")");
    	
    	if (!SpatialUtils.hasCRS(inputPopulation1.getDataTableSpec()))
    		throw new IllegalArgumentException("the input table 1 contains spatial data but no Coordinate Reference System");
    	
    	if (inputPopulation2 != null) {
	    	if (!SpatialUtils.hasGeometry(inputPopulation2.getDataTableSpec()))
	    		throw new IllegalArgumentException("the input table 2 contains no spatial data (no column named "+SpatialUtils.GEOMETRY_COLUMN_NAME+")");
	    	
	    	if (!SpatialUtils.hasCRS(inputPopulation2.getDataTableSpec()))
	    		throw new IllegalArgumentException("the input table 2 contains spatial data but no Coordinate Reference System");
	    	
    	}
    	
    	// decode population 1
    	CoordinateReferenceSystem crsOrig1 = SpatialUtils.decodeCRS(inputPopulation1.getSpec());
    	tmpFile1 = FileUtil.createTempFile("shapefile", ".shp");

    	// copy the input population into a datastore
        datastore1 = SpatialUtils.createDataStore(tmpFile1, false);
        Runnable runnableSpatialize1 = SpatialUtils.decodeAsFeaturesRunnable(
        		inputPopulation1, 
        		SpatialUtils.GEOMETRY_COLUMN_NAME, 
        		progress1, 
        		datastore1, 
        		"entities1", 
        		crsOrig1
        		);
    	exec.setMessage("storing entities");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(runnableSpatialize1);
        
        
        if (inputPopulation2 != null) {
        	CoordinateReferenceSystem crsOrig2 = SpatialUtils.decodeCRS(inputPopulation2.getSpec());
        	tmpFile2 = FileUtil.createTempFile("shapefile", ".shp");
            datastore2 = SpatialUtils.createDataStore(tmpFile2, false);

	        Runnable runnableSpatialize2 = SpatialUtils.decodeAsFeaturesRunnable(
	        		inputPopulation2, 
	        		SpatialUtils.GEOMETRY_COLUMN_NAME, 
	        		progress2, 
	        		datastore2, 
	        		"entities2", 
	        		crsOrig2
	        		);
	        executor.execute(runnableSpatialize2);

        }
        
        executor.shutdown();
        try {
        	// wait forever
        	executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
        	throw new RuntimeException(e);
        }
    	
        return new BufferedDataTable[]{};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
      
    	disposeDatastores();
    }

    protected void disposeDatastores() {
    	 
    	if (datastore1 != null)
            datastore1.dispose();

    	if (datastore2 != null)
            datastore2.dispose();
    }
    
    
    
    @Override
	protected void onDispose() {
    	disposeDatastores();
    	super.onDispose();
	}


	/**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        return new DataTableSpec[]{ };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_color1.saveSettingsTo(settings);
    	m_color2.saveSettingsTo(settings);
    	
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    	m_color1.loadSettingsFrom(settings);
    	m_color2.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_color1.validateSettings(settings);
    	m_color2.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
    	{
	    	File bckp = new File(internDir, "datastore1");
	    	if (!bckp.exists() || !bckp.canRead() || !bckp.isFile())
	    		return;
	    	
	    	System.out.println("restoring datastore from "+bckp);
	
	    	tmpFile1 = FileUtil.createTempFile("shapefile", ".shp");
	    	
	    	FileUtil.copy(
	    			bckp,
	    			tmpFile1,
	    			exec
	    			);
	    	
	        datastore1 = SpatialUtils.createDataStore(tmpFile1, false);
    	}
      	{
	    	File bckp = new File(internDir, "datastore2");
	    	if (!bckp.exists() || !bckp.canRead() || !bckp.isFile())
	    		return;
	    	
	    	System.out.println("restoring datastore from "+bckp);
	
	    	tmpFile2 = FileUtil.createTempFile("shapefile", ".shp");
	    	
	    	FileUtil.copy(
	    			bckp,
	    			tmpFile2,
	    			exec
	    			);
	    	
	        datastore2 = SpatialUtils.createDataStore(tmpFile2, false);
    	}
    	
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
    	if (datastore1 != null) {

	    	File bckp = new File(internDir, "datastore1");
	    	FileUtil.copy(
	    			tmpFile1, 
	    			bckp, 
	    			exec.createSilentSubProgress(0.5)
	    			);
    	}
    	if (datastore2 != null) {

	    	File bckp = new File(internDir, "datastore2");
	    	FileUtil.copy(
	    			tmpFile2, 
	    			bckp, 
	    			exec.createSilentSubProgress(0.5)
	    			);
    	}
    	
    }

}

