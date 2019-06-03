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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.transform.compute_ecql;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
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
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;


/**
 * This is the model implementation of Reproject.
 * Reprojects a spatialized population
 *
 * @author Samuel Thiriot
 */
public class ComputeECQLNodeModel extends NodeModel {
    

	private SettingsModelString m_query = new SettingsModelString(
			"query",
			"area(the_geom)"
			);
	
	private SettingsModelString m_type = new SettingsModelString(
			"type",
			"Double"
			);
	
	private SettingsModelString m_colname = new SettingsModelString(
			"colname",
			"surface"
			);

    /**
     * Constructor for the node model.
     */
    protected ComputeECQLNodeModel() {
    
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
    	getExpression();

    	
    	String colname = m_colname.getStringValue();
    	if (spec.getColumnSpec(colname) != null) 
    		throw new InvalidSettingsException("There is already a column named "+colname+" in the input table");
    	
    	
    	DataColumnSpec addedColumn = createColumnSpec();
    	
    	DataTableSpecCreator specCreateor = new DataTableSpecCreator(spec);
    	
    	if (addedColumn != null)
    		specCreateor.addColumns(addedColumn);
    	
        return new DataTableSpec[]{ specCreateor.createSpec() };
    }
    
    /**
     * Returns a column spec for the novel column,
     * or null if the column to be created is the geometry
     * @return
     * @throws InvalidSettingsException
     */
    protected DataColumnSpec createColumnSpec() throws InvalidSettingsException {
    	
    	
    	final String typeName = m_type.getStringValue().toLowerCase();
    	
    	DataType type = null;

    	if (typeName.equalsIgnoreCase("string"))
    		type = StringCell.TYPE;
    	else if (typeName.equalsIgnoreCase("double"))
    		type = DoubleCell.TYPE;
    	else if (typeName.equalsIgnoreCase("integer"))
    		type = IntCell.TYPE;
    	else if (typeName.equalsIgnoreCase("long"))
    		type = LongCell.TYPE;
    	else if (typeName.equalsIgnoreCase("boolean"))
    		type = BooleanCell.TYPE;
    	else if (typeName.equalsIgnoreCase("geometry"))
    		type = null;
    	else
    		throw new InvalidSettingsException("unknown type "+typeName);
    	
    	if (type == null)
    		return null;
    	
    	final String colname = m_colname.getStringValue();
    	
    	return new DataColumnSpecCreator(colname, type).createSpec();
    	
    	
    }

    protected Expression getExpression() throws InvalidSettingsException {

    	final String query = m_query.getStringValue();

    	Expression exp = null;
    	try {
    		exp = ECQL.toExpression(query);
    	} catch (CQLException e) {
    		throw new InvalidSettingsException("invalid CQL expression: "+e.getMessage(), e);
    	}

    	return exp;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	final BufferedDataTable inputPopulation = inData[0];

    	DataColumnSpec addedColumn = createColumnSpec();
    	
    	DataTableSpecCreator specCreateor = new DataTableSpecCreator(inputPopulation.getDataTableSpec());
    	if (addedColumn != null)
    		specCreateor.addColumns(addedColumn);
    	DataTableSpec newSpec = specCreateor.createSpec();
    	
    	Expression exp = getExpression();
    	
    	
    	DataStore datastore = SpatialUtils.createTmpDataStore(false);
    	
    	ExecutionContext execSpatialize = exec.createSubExecutionContext(0.5);
    	ExecutionContext execFilter = exec.createSubExecutionContext(0.5);

    	final CoordinateReferenceSystem crs = SpatialUtils.decodeCRS(inputPopulation.getDataTableSpec());
    	    	
    	exec.setMessage("spatializing features");
    	SpatialUtils.decodeAsFeatures(
    			inputPopulation, 
    			SpatialUtils.GEOMETRY_COLUMN_NAME, 
    			execSpatialize, 
    			datastore, 
    			"entities", 
    			crs
    			);
    	

    	exec.setMessage("computing "+m_colname.getStringValue());
    	
        SimpleFeatureCollection features = datastore.getFeatureSource(datastore.getNames().get(0)).getFeatures();

    	BufferedDataContainer container = exec.createDataContainer(newSpec);
        CloseableRowIterator itRow = inputPopulation.iterator();

        MissingCell missing = new MissingCell("no data");
        
        final int idxGeom = newSpec.findColumnIndex(SpatialUtils.GEOMETRY_COLUMN_NAME);
        
        SimpleFeatureIterator itFeatures = features.features();
        double total = inputPopulation.size();
        int done = 0;
        try {

	        while (itRow.hasNext()) {

	        	if (!itFeatures.hasNext())
	        		throw new RuntimeException("error during the spatialization: less features than lines. This should not happen and reveals a problem in the program.");
	        	final SimpleFeature feature = itFeatures.next();
	        	
	        	final DataRow row = itRow.next();

	        	if (done++ % 10 == 0) {
	        		exec.checkCanceled();
	        		execFilter.setProgress(done/total);
	        	}
	        	

	        	String rowid = (String) feature.getAttribute("rowid");
	        	
	        	if (!row.getKey().getString().equals(rowid)) {
	        		throw new RuntimeException("The features were not returned in the right order. please report a bug..."); 
	        	}
	        	
	        	List<DataCell> cells = null;
	        	Object res = exp.evaluate(feature);

	        	if (addedColumn == null) {
	        		
	        		cells = new ArrayList<>(row.getNumCells());
		        	
	        		for (int col=0; col < idxGeom; col++)
	        			cells.add(row.getCell(col));
	        		
	        		cells.add(StringCellFactory.create(res.toString()));
	        		
	        		for (int col=idxGeom+1; col < row.getNumCells(); col++)
	        			cells.add(row.getCell(col));
	        		
		        	
	        	} else {
		        	
		        	DataCell newCell = null;
		        	try {
			        	if (res == null)
			        		newCell = missing;
			        	else if (addedColumn.getType().equals(StringCell.TYPE)) {
			        		newCell = StringCellFactory.create(res.toString());
			        	} else if (addedColumn.getType().equals(IntCell.TYPE)) {
			        		newCell = IntCellFactory.create(((Number)res).intValue());
			        	} else if (addedColumn.getType().equals(DoubleCell.TYPE)) {
			        		newCell = DoubleCellFactory.create(((Number)res).doubleValue());
			        	} else if (addedColumn.getType().equals(LongCell.TYPE)) {
			        		newCell = LongCellFactory.create(((Number)res).longValue());
			        	} else if (addedColumn.getType().equals(BooleanCell.TYPE)) {
			        		newCell = BooleanCellFactory.create((Boolean)res);
			        	} else
			        		throw new RuntimeException("unknown type "+addedColumn.getType());
			        	
		        	} catch (ClassCastException e) {
		        		throw new InvalidSettingsException("the type you selected is not compliant with the result of the expression; try "+res.getClass().getSimpleName());
		        	}
		        	
		        	cells = new ArrayList<>(row.getNumCells()+1);
		        	cells.addAll(row.stream().collect(Collectors.toList()));
		        	cells.add(newCell);
		        	
	        	}
	        	
	        	
	        	container.addRowToTable(new DefaultRow(row.getKey(), cells));
	        	 
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
    	m_query.saveSettingsTo(settings);
    	m_colname.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            

    	m_query.loadSettingsFrom(settings);
    	m_type.loadSettingsFrom(settings);
    	m_colname.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_query.validateSettings(settings);
    	m_type.validateSettings(settings);
    	m_colname.validateSettings(settings);

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

