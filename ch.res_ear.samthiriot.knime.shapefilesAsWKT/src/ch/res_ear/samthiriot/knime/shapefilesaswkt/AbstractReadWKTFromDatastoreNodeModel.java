package ch.res_ear.samthiriot.knime.shapefilesaswkt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_shapefile.GeotoolsToDataTableMapper;


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
public abstract class AbstractReadWKTFromDatastoreNodeModel extends NodeModel {
    
	
	/**
	 * Constructor for the node model.
	 */
	protected AbstractReadWKTFromDatastoreNodeModel() {
		
		// one standard BufferedDatatable
		super(0, 1);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		return new DataTableSpec[] { null };
	}

	/**
	 * Opens the datastore for reading. Should not create a file nor schema.
	 * @return
	 * @throws InvalidSettingsException
	 */
	protected abstract DataStore openDataStore(ExecutionContext exec) throws InvalidSettingsException;
	
	/**
	 * Define which schema should be open in this datastore, either according to 
	 * user parameters or by selecting automatically one of them.
	 * @param datastore
	 * @return
	 * @throws InvalidSettingsException
	 */
	protected abstract String getSchemaName(DataStore datastore) throws InvalidSettingsException;
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

		final DataStore datastore = openDataStore(exec);

		if (datastore.getTypeNames().length == 0)
			throw new InvalidSettingsException("this database does not contain any schema");
		
		final String schemaName = getSchemaName(datastore);
		
		SimpleFeatureType type;
		try {
			type = datastore.getSchema(schemaName);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to decode the schema "+schemaName+" from the file: "+e, e);
		}
		
		List<AttributeDescriptor> descriptors = new ArrayList<>(type.getAttributeDescriptors());
		
		// create mappers
		Map<AttributeDescriptor,GeotoolsToDataTableMapper> gtAttribute2mapper = 
				descriptors.stream()
							.collect(Collectors.toMap( 
									ad -> ad, 
									ad -> new GeotoolsToDataTableMapper(
											ad, 
											type.getCoordinateReferenceSystem(), 
											getLogger()))
							);
	
		// prepare the output
		DataColumnSpec[] dataColSpecs = descriptors.stream()
				   .map( d -> gtAttribute2mapper.get(d).getKnimeColumnSpec() )
				   .toArray(DataColumnSpec[]::new);
        DataTableSpec outputSpec = new DataTableSpec(dataColSpecs);
        
        
        final BufferedDataContainer container = exec.createDataContainer(outputSpec);


        // work for true
		int total = datastore.getFeatureSource(schemaName).getFeatures().size();
		
		SimpleFeatureIterator itFeature = datastore
												.getFeatureSource(schemaName)
				 								.getFeatures()
				 								.features();
		int rowIdx = 0;
		while (itFeature.hasNext()) {
			SimpleFeature feature = itFeature.next();
			
			int i=0;
			DataCell[] cells = new DataCell[dataColSpecs.length];
			for (AttributeDescriptor gtAtt: descriptors) {
				
				Object gtVal = feature.getAttribute(gtAtt.getName());
				GeotoolsToDataTableMapper mapper = gtAttribute2mapper.get(gtAtt);
				
	    		
				cells[i++] = mapper.convert(gtVal);
			}

			container.addRowToTable(
        			new DefaultRow(
	        			new RowKey("Row " + rowIdx), 
	        			cells
	        			)
        			);
			if (rowIdx % 10 == 0) { 
	            // check if the execution monitor was canceled
	            exec.checkCanceled();
	            exec.setProgress(
	            		(double)rowIdx / total, 
	            		"reading row " + rowIdx);
        	}
    		rowIdx++;
		}
		
		itFeature.close();
		datastore.dispose();
		
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{ out };
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

