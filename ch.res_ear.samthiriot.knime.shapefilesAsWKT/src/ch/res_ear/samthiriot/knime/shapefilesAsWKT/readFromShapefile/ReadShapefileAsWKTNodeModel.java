package ch.res_ear.samthiriot.knime.shapefilesAsWKT.readFromShapefile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import ch.res_ear.samthiriot.knime.shapefilesAsWKT.SpatialUtils;


/**
 * This is the model implementation of ReadShapefileAsKML.
 * Reads spatial features (geometries) from a <a href="https://en.wikipedia.org/wiki/Shapefile">shapefile</a>. Accepts any geometry type: points, lines, or polygons.  * n * nActual computation relies on the <a href="https://geotools.org/">geotools library</a>.
 *
 * @author EIFER
 */
public class ReadShapefileAsWKTNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(ReadShapefileAsWKTNodeModel.class);
        
    private final SettingsModelString m_file = new SettingsModelString("filename", null);
    private final SettingsModelString m_charset = new SettingsModelString("charset", Charset.defaultCharset().name());
    private final SettingsModelString m_crs = new SettingsModelString("crs", SpatialUtils.getDefaultCRSString());

    /**
     * Constructor for the node model.
     */
    protected ReadShapefileAsWKTNodeModel() {
    
        super(0, 1);
    }
    
    protected DataStore openDataStore() throws InvalidSettingsException {

    	
    	// retrieve parameters
        CheckUtils.checkSourceFile(m_file.getStringValue());
        
        URL filename;
		try {
			filename = FileUtil.toURL(m_file.getStringValue());
		} catch (InvalidPathException | MalformedURLException e2) {
			e2.printStackTrace();
			throw new InvalidSettingsException("unable to open URL "+m_file.getStringValue()+": "+e2.getMessage());
		}
        
        if (filename == null)
        	throw new InvalidSettingsException("no file defined");
       
        String charset = m_charset.getStringValue();


        // open the 
		Map<String,Object> parameters = new HashMap<>();
		parameters.put("url", filename);
		DataStore datastore;
		try {
	        logger.info("opening as a shapefile: "+filename);

			datastore = DataStoreFinder.getDataStore(parameters);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new InvalidSettingsException("Unable to open the url as a shape file: "+e1.getMessage());
		}

		// set the charset
		try {
			((ShapefileDataStore)datastore).setCharset(Charset.forName(charset));
		} catch (ClassCastException e) {
			throw new InvalidSettingsException("unable to define charset for this datastore");
		}
				
		
		return datastore;
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

		final DataStore datastore = openDataStore();

		String schemaName;
		try {
			schemaName = datastore.getTypeNames()[0];
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error while searching for a schema inside the shapefile: "+e, e);
		}
		
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
									ad -> new GeotoolsToDataTableMapper(ad, type.getCoordinateReferenceSystem(), logger))
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	// nothing to reset
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
		final DataStore datastore = openDataStore();

		String schemaName;
		try {
			schemaName = datastore.getTypeNames()[0];
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error while searching for a schema inside the shapefile: "+e, e);
		}
		
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
									ad -> new GeotoolsToDataTableMapper(ad, type.getCoordinateReferenceSystem(), logger))
							);
	
		// prepare the output
		DataColumnSpec[] dataColSpecs = descriptors.stream()
				   .map( d -> gtAttribute2mapper.get(d).getKnimeColumnSpec() )
				   .toArray(DataColumnSpec[]::new);
        DataTableSpec outputSpec = new DataTableSpec(dataColSpecs);
            	
        return new DataTableSpec[]{ outputSpec };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        
    	m_file.saveSettingsTo(settings);
    	m_charset.saveSettingsTo(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        m_file.loadSettingsFrom(settings);
        m_charset.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    	m_file.validateSettings(settings);
    	m_charset.validateSettings(settings);

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

