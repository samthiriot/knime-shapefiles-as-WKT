package ch.res_ear.samthiriot.knime.shapefilesaswkt.view;

import java.awt.Color;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColor;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.port.PortType;
import org.knime.core.util.FileUtil;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;



/**
 * This is the model implementation of DisplaySpatialPopulation.
 * View the spatial population on a map.
 *
 * @author Samuel Thiriot
 */
public class DisplaySpatialPopulationNodeModel extends NodeModel {
    
    protected DataStore datastore1 = null;
    protected File tmpFile1 = null;
    
    protected DataStore datastore2 = null;
    protected File tmpFile2 = null;

    protected SettingsModelColor m_color1 = new SettingsModelColor("color1", Color.GRAY);
    protected SettingsModelColor m_color2 = new SettingsModelColor("color2", Color.BLUE);
    
    protected SettingsModelDoubleBounded m_opacity1 = new SettingsModelDoubleBounded("opacity1", 0.5, 0.0, 1.0);
    protected SettingsModelDoubleBounded m_opacity2 = new SettingsModelDoubleBounded("opacity2", 0.7, 0.0, 1.0);
    
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
        		crsOrig1,
        		false,
        		m_color1.getColorValue()
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
	        		crsOrig2,
	        		false,
	        		m_color2.getColorValue()
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
    	m_opacity1.saveSettingsTo(settings);
    	m_opacity2.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    	m_color1.loadSettingsFrom(settings);
    	m_color2.loadSettingsFrom(settings);
    	m_opacity1.loadSettingsFrom(settings);
    	m_opacity2.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_color1.validateSettings(settings);
    	m_color2.validateSettings(settings);
    	m_opacity1.validateSettings(settings);
    	m_opacity2.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
    	// we know the names expected for the files
    	// we just have to reload the datastores from them
    	{
	    	File bckp = new File(internDir, "datastore1");
	    	if (!bckp.exists() || !bckp.canRead() || !bckp.isDirectory()) {
	    		//getLogger().error("cannot restore the state of the view: unable to find the directory "+bckp);
	    		return;
	    	}
	    	
	    	getLogger().debug("restoring datastore from "+bckp);
	
	    	tmpFile1 = new File(bckp, "shapefile.shp");
	    	
	        datastore1 = SpatialUtils.createDataStore(tmpFile1, false);
    	}
      	{
	    	File bckp = new File(internDir, "datastore2");
	    	if (!bckp.exists() || !bckp.canRead() || !bckp.isDirectory()) {
	    		return;
	    	}
	    	
	    	getLogger().debug("restoring datastore from "+bckp);
	
	    	tmpFile2 = new File(bckp, "shapefile.shp");
	    	
	        datastore2 = SpatialUtils.createDataStore(tmpFile2, false);
    	}
    	
    }
    
    protected void copyAllShapefileFiles(
    			File fileShp,
    			File targetDirectory,
    			ExecutionMonitor exec
    			) throws IOException, CanceledExecutionException {
    	
    	
    	targetDirectory.mkdirs();
    	
    	final String filenameBase = FilenameUtils.removeExtension(fileShp.getName());
    	File origDirectory = fileShp.getParentFile();
    	
    	File [] filesToCopy = origDirectory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(filenameBase);
			}
		});
    	
    	double progress = 1.0/filesToCopy.length;
    	for (File fileToCopy: filesToCopy) {
    		String extension = FilenameUtils.getExtension(fileToCopy.getName());
	    	FileUtil.copy(
	    			fileToCopy, 
	    			new File(targetDirectory, "shapefile."+extension), 
	    			exec.createSilentSubProgress(progress)
	    			);
    	}

    	
    	
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
    	// principle 
    	// get the files behind the datastores
    	// copy them and all related files with variations of extensions
    	// (remember a shapefile is stored as truc.shp, along with truc.shx, .prj, etc.)
    	
    	if (datastore1 != null) {
	    	File dir1 = new File(internDir, "datastore1");
	    	copyAllShapefileFiles(tmpFile1, dir1, exec.createSilentSubProgress(0.5));
    	}
    	if (datastore2 != null) {

	    	File dir2 = new File(internDir, "datastore2");
	    	copyAllShapefileFiles(tmpFile2, dir2, exec.createSilentSubProgress(0.5));

    	}
    	
    }

}

