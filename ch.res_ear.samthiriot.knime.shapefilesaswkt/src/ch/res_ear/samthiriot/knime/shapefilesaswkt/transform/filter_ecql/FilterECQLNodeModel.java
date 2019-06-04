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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.transform.filter_ecql;

import java.io.File;
import java.io.IOException;

import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.knime.base.util.flowvariable.FlowVariableProvider;
import org.knime.base.util.flowvariable.FlowVariableResolver;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;


/**
 * This is the model implementation of Reproject.
 * Reprojects a spatialized population
 *
 * @author Samuel Thiriot
 */
public class FilterECQLNodeModel extends NodeModel implements FlowVariableProvider {
    

	private SettingsModelString m_query = new SettingsModelString(
			"query",
			"area(the_geom) < 15"
			);

    /**
     * Constructor for the node model.
     */
    protected FilterECQLNodeModel() {
    
        super(1, 1);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
    	DataTableSpec spec = inSpecs[0];
    	
    	if (!SpatialUtils.hasGeometry(spec)) {
    		throw new InvalidSettingsException("the input table does not contains WKT spatial data");
    	}
    	
    	// ensure the query can be decoded
    	getFilter();

        return new DataTableSpec[]{ spec };
    }

    protected Filter getFilter() throws InvalidSettingsException {

    	final String query = m_query.getStringValue();

        String queryWithVariableValues = FlowVariableResolver.parse(query, this);

    	Filter filter = null;
    	try {
    		filter = ECQL.toFilter(queryWithVariableValues);
    	} catch (CQLException e) {
    		throw new InvalidSettingsException("invalid CQL query: "+e.getMessage(), e);
    	}

    	return filter;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	
    	final BufferedDataTable inputPopulation = inData[0];
    	
    	DataStore datastore = SpatialUtils.createTmpDataStore(false);
    	
    	ExecutionContext execSpatialize = exec.createSubExecutionContext(0.5);
    	ExecutionContext execFilter = exec.createSubExecutionContext(0.5);

    	final CoordinateReferenceSystem crs = SpatialUtils.decodeCRS(inputPopulation.getDataTableSpec());
    	
    	final Filter filter = getFilter();
    	
    	exec.setMessage("spatializing features");
    	SpatialUtils.decodeAsFeatures(
    			inputPopulation, 
    			SpatialUtils.GEOMETRY_COLUMN_NAME, 
    			execSpatialize, 
    			datastore, 
    			"entities", 
    			crs
    			);
    	
    	exec.setMessage("filtering");
        SimpleFeatureCollection features = datastore.getFeatureSource(datastore.getNames().get(0)).getFeatures(
        		filter
        		);

    	BufferedDataContainer container = exec.createDataContainer(inputPopulation.getDataTableSpec());
        CloseableRowIterator itRow = inputPopulation.iterator();

        SimpleFeatureIterator itFeatures = features.features();
        double total = inputPopulation.size();
        int done = 0;
        try {
        	String rowid = null;

	        while (itRow.hasNext()) {
	        	
	        	if (done++ % 10 == 0) {
	        		exec.checkCanceled();
	        		execFilter.setProgress(done/total);
	        	}
	        	
	        	if (rowid == null) {
		        	// we might have less features as they were filtered
		        	if (!itFeatures.hasNext()) {
		        		System.out.println("skipping the last rows");
		        		break;
		        	}
		        	final SimpleFeature feature = itFeatures.next();
		        	rowid = (String) feature.getAttribute("rowid");
		        	
	        	}
	        	
	        	
	        	
	        	final DataRow row = itRow.next();
	        	
	        	if (row.getKey().getString().equals(rowid)) {
	        		container.addRowToTable(row);
	        		rowid = null; // shift next 
	        	} 
	        }
        } finally {
	        if (itRow != null)
	        	itRow.close();
	        
	        if (itFeatures != null)
	        	itFeatures.close();
	        
	        if (datastore != null)
	        	datastore.dispose();

        }
        
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{ out };
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
        
    	m_query.saveSettingsTo(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            

    	m_query.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_query.validateSettings(settings);

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

