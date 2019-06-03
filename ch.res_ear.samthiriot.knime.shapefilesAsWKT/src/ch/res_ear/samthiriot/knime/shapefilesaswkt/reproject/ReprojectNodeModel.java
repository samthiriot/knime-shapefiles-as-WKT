/*******************************************************************************
 * Copyright (c) 2019 EIfER[1] (European Institute for Energy Research).
 * This program and the accompanying materials
 * are made available under the terms of the GNU GENERAL PUBLIC LICENSE
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Contributors:
 *     Samuel Thiriot - original version and contributions
 *******************************************************************************/
package ch.res_ear.samthiriot.knime.shapefilesaswkt.reproject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataContainer;
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
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;


/**
 * This is the model implementation of Reproject.
 * Reprojects a spatialized population
 *
 * @author Samuel Thiriot
 */
public class ReprojectNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(ReprojectNodeModel.class);
        
    
    public static final String MODEL_KEY_CRS = "crs";

    private final SettingsModelString m_crs = new SettingsModelString(MODEL_KEY_CRS, SpatialUtils.getDefaultCRSString());


    /**
     * Constructor for the node model.
     */
    protected ReprojectNodeModel() {
    
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	// define the length of the several steps
    	ExecutionMonitor execProgressSpatialiseInputs = exec.createSubProgress(0.20);  
    	ExecutionMonitor execProgressReproject = exec.createSubProgress(0.80);  

    	final BufferedDataTable inputPopulation = inData[0];
    	
    	if (!SpatialUtils.hasGeometry(inputPopulation.getDataTableSpec()))
    		throw new InvalidSettingsException("the input table contains no spatial data (no column named "+SpatialUtils.GEOMETRY_COLUMN_NAME+")");
    	
    	if (!SpatialUtils.hasCRS(inputPopulation.getDataTableSpec()))
    		throw new InvalidSettingsException("the input table contains spatial data but no Coordinate Reference System");
    	
    	int idxColumnGeom = inputPopulation.getSpec().findColumnIndex(SpatialUtils.GEOMETRY_COLUMN_NAME);
    	
    	CoordinateReferenceSystem crsOrig = SpatialUtils.decodeCRS(inputPopulation.getSpec());
    	
    	CoordinateReferenceSystem crsTarget = SpatialUtils.getCRSforString(m_crs.getStringValue());
    	
    	// copy the input population into a datastore
    	exec.setMessage("spatializing inputs");
        DataStore datastore = SpatialUtils.createDataStore();
        Runnable runnableSpatialize = SpatialUtils.decodeAsFeaturesRunnable(
        		inputPopulation, 
        		SpatialUtils.GEOMETRY_COLUMN_NAME, 
        		execProgressSpatialiseInputs, 
        		datastore, 
        		"entities", 
        		crsOrig,
        		false,
        		null
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
        
    	exec.setMessage("reprojecting");
        
    	
    	DataColumnSpec[] novelsSpecs = new DataColumnSpec[inputPopulation.getDataTableSpec().getNumColumns()];
    	for (int i=0; i<idxColumnGeom; i++) {
    		novelsSpecs[i] = inputPopulation.getDataTableSpec().getColumnSpec(i);
    	}
    	// TODO inputPopulation.getDataTableSpec().getColumnSpec(idxColumnGeom).getProperties().
		Map<String,String> properties = new HashMap<String, String>();
		properties.put(SpatialUtils.PROPERTY_CRS_CODE, SpatialUtils.getStringForCRS(crsTarget));
		properties.put(SpatialUtils.PROPERTY_CRS_WKT, crsTarget.toWKT());
    	DataColumnSpecCreator creator = new DataColumnSpecCreator(
    			inputPopulation.getDataTableSpec().getColumnSpec(idxColumnGeom).getName(), 
    			StringCell.TYPE
    			);
    	creator.setProperties(new DataColumnProperties(properties));
    	novelsSpecs[idxColumnGeom] = creator.createSpec();
    	for (int i=idxColumnGeom+1; i<novelsSpecs.length; i++) {
    		novelsSpecs[i] = inputPopulation.getDataTableSpec().getColumnSpec(i);
    	}
    	
    	DataTableSpec novelSpec = new DataTableSpec("spatial entities", novelsSpecs);
        BufferedDataContainer container = exec.createDataContainer(novelSpec);
        Iterator<DataRow> itRow = inputPopulation.iterator();
        
        // TODO use http://docs.geotools.org/latest/tutorials/geometry/geometrycrs.html
        
        SimpleFeatureCollection features = datastore.getFeatureSource(datastore.getNames().get(0)).getFeatures();

        ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(
        		features, crsTarget);
        SimpleFeatureIterator itEntities = rfc.features();
        long currentIdx = 0;
        long total = inputPopulation.size();
        while (itEntities.hasNext()) {
        	if (!itRow.hasNext()) throw new RuntimeException("program error: wrong size");
        	
        	DataRow row = itRow.next();
        	
        	SimpleFeature parentFeature = itEntities.next();
        	Geometry geom = (Geometry)parentFeature.getDefaultGeometry();
        	
        	
        	DataCell[] novelCells = new DataCell[row.getNumCells()];
        	for (int i=0; i<idxColumnGeom; i++) {
        		novelCells[i] = row.getCell(i);
        	}
        	novelCells[idxColumnGeom] = StringCellFactory.create(geom.toString());
        	for (int i=idxColumnGeom+1; i<row.getNumCells(); i++) {
        		novelCells[i] = row.getCell(i);
        	}
            container.addRowToTable(new DefaultRow(row.getKey(), novelCells));

        	if (currentIdx % 10 == 0) {
        		execProgressReproject.setProgress((double)currentIdx/total, ""+currentIdx+"/"+total);
        		execProgressReproject.checkCanceled();
        	}
        	
        	currentIdx++;
        }
        itEntities.close();
    	
        datastore.dispose();
        
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
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
        

    	int idxColumnGeom = inSpecs[0].findColumnIndex(SpatialUtils.GEOMETRY_COLUMN_NAME);

    	//CoordinateReferenceSystem crsOrig = SpatialUtils.decodeCRSFromColumnSpec(inSpecs[0].getColumnSpec(idxColumnGeom));
    	CoordinateReferenceSystem crsTarget = SpatialUtils.getCRSforString(m_crs.getStringValue());
    	
    	
    	DataColumnSpec[] novelsSpecs = new DataColumnSpec[inSpecs[0].getNumColumns()];
    	for (int i=0; i<idxColumnGeom; i++) {
    		novelsSpecs[i] = inSpecs[0].getColumnSpec(i);
    	}
		Map<String,String> properties = new HashMap<String, String>(inSpecs[0].getProperties());
		properties.put(SpatialUtils.PROPERTY_CRS_CODE, SpatialUtils.getStringForCRS(crsTarget));
		properties.put(SpatialUtils.PROPERTY_CRS_WKT, crsTarget.toWKT());
    	DataColumnSpecCreator creator = new DataColumnSpecCreator(
    			inSpecs[0].getColumnSpec(idxColumnGeom).getName(), 
    			StringCell.TYPE
    			);
    	creator.setProperties(new DataColumnProperties(properties));
    	novelsSpecs[idxColumnGeom] = creator.createSpec();
    	for (int i=idxColumnGeom+1; i<novelsSpecs.length; i++) {
    		novelsSpecs[i] = inSpecs[0].getColumnSpec(i);
    	}
    	
        return new DataTableSpec[]{ new DataTableSpec("recoded", novelsSpecs) };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        
    	m_crs.saveSettingsTo(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            

    	m_crs.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_crs.validateSettings(settings);

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

