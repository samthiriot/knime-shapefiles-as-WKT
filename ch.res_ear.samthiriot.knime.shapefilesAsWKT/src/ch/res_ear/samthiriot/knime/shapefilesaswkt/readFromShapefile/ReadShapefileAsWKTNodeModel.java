package ch.res_ear.samthiriot.knime.shapefilesaswkt.readFromShapefile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.AbstractReadWKTFromDatastoreNodeModel;


/**
 * This is the model implementation of ReadShapefileAsKML.
 * Reads spatial features (geometries) from a <a href="https://en.wikipedia.org/wiki/Shapefile">shapefile</a>. Accepts any geometry type: points, lines, or polygons.  * n * nActual computation relies on the <a href="https://geotools.org/">geotools library</a>.
 *
 * @author EIFER
 */
public class ReadShapefileAsWKTNodeModel extends AbstractReadWKTFromDatastoreNodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(ReadShapefileAsWKTNodeModel.class);
        
    private final SettingsModelString m_file = new SettingsModelString("filename", null);
    private final SettingsModelString m_charset = new SettingsModelString("charset", Charset.defaultCharset().name());

    /**
     * Constructor for the node model.
     */
    protected ReadShapefileAsWKTNodeModel() {
    
        super();
    }
    
    @Override
    protected DataStore openDataStore(ExecutionContext exec) throws InvalidSettingsException {

    	
    	// retrieve parameters
        CheckUtils.checkSourceFile(m_file.getStringValue());
        
        if (m_file.getStringValue() == null)
        	throw new InvalidSettingsException("no file defined");
       
        URL filename;
        try {
			filename = FileUtil.toURL(m_file.getStringValue());
			
		} catch (InvalidPathException | MalformedURLException e2) {
			e2.printStackTrace();
			throw new InvalidSettingsException("unable to open URL "+m_file.getStringValue()+": "+e2.getMessage());
		}

        Path filePath = null;
        try {
			filePath = FileUtil.resolveToPath(filename);
		} catch (IOException | URISyntaxException e2) {
			throw new InvalidSettingsException("unable to resolve this URL to a path: "+filename);
		}
        
        String charset = m_charset.getStringValue();


        // open the 
		Map<String,Object> parameters = new HashMap<>();
		try {
			parameters.put("url", filePath.toUri().toURL());
		} catch (MalformedURLException e2) {
			throw new RuntimeException("cannot convert the path "+filePath+" to an URL", e2);
		}
		DataStore datastore;
		try {
	        getLogger().info("opening as a shapefile: "+filePath.toUri());

			datastore = DataStoreFinder.getDataStore(parameters);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new InvalidSettingsException("Unable to open the url as a shape file: "+e1.getMessage());
		}
		
		if (datastore == null)
			throw new InvalidSettingsException("unable to open the shapefile from path "+filename);

		// set the charset
		try {
			((ShapefileDataStore)datastore).setCharset(Charset.forName(charset));
		} catch (ClassCastException e) {
			throw new InvalidSettingsException("unable to define charset for this datastore");
		}
				
		
		return datastore;
    }
        

	@Override
	protected String getSchemaName(DataStore datastore) throws InvalidSettingsException {

		String schemaName = null;
		String[] existing = null;
		try {
			existing = datastore.getTypeNames();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error while searching for a schema inside the shapefile: "+e, e);
		}
		
		if (existing.length > 1)
			getLogger().warn(
					"there are several layers in this data store: "+
							Arrays.toString(existing)+"; will open the first one: "+
							schemaName
							);
		schemaName = existing[0];
		
		return schemaName;
	}
    

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
		final DataStore datastore = openDataStore(null);

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

   
}

