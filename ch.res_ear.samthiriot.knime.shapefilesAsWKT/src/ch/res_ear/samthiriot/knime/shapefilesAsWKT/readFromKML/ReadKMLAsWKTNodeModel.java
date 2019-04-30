package ch.res_ear.samthiriot.knime.shapefilesAsWKT.readFromKML;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.referencing.CRS;
import org.geotools.xsd.Parser;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.SAXException;

import ch.res_ear.samthiriot.knime.shapefilesAsWKT.SpatialUtils;


/**
 * This is an example implementation of the node model of the
 * "ReadKMLAsWKT" node.
 * 
 * This example node performs simple number formatting
 * ({@link String#format(String, Object...)}) using a user defined format string
 * on all double columns of its input table.
 *
 * @author Samuel Thiriot
 */
public class ReadKMLAsWKTNodeModel extends NodeModel {
    
    /**
	 * The logger is used to print info/warning/error messages to the KNIME console
	 * and to the KNIME log file. Retrieve it via 'NodeLogger.getLogger' providing
	 * the class of this node model.
	 */
	private static final NodeLogger logger = NodeLogger.getLogger(ReadKMLAsWKTNodeModel.class);


    private final SettingsModelString m_file = new SettingsModelString("filename", null);

	/**
	 * Constructor for the node model.
	 */
	protected ReadKMLAsWKTNodeModel() {
        super(0, 1);
	}

	protected SimpleFeature decodeFileFromKML() throws InvalidSettingsException {
		
		 // open the file

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
       
        InputStream inputStream;
		try {
			inputStream = FileUtil.openStreamWithTimeout(filename);
		} catch (IOException e2) {
			e2.printStackTrace();
			throw new IllegalArgumentException("unable to open the URL "+filename+": "+e2.getMessage());
		}
        
        Parser parser = new Parser(new org.geotools.kml.v22.KMLConfiguration());
        SimpleFeature f;
		try {
			f = (SimpleFeature) parser.parse( inputStream );
		} catch (IOException | SAXException | ParserConfigurationException e2) {
			e2.printStackTrace();
			throw new IllegalArgumentException("Invalid file content "+filename+": "+e2.getMessage());

		}
		
		return f;
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

       
    	SimpleFeature f = decodeFileFromKML();
    
        DataTableSpec tableSpec = createDataTableSpec(f);
        
        final BufferedDataContainer container = exec.createDataContainer(tableSpec);

        final DataCell missing = new MissingCell("was null in KML");
        
        // read the file
        Collection<?> placemarks = (Collection) f.getAttribute("Feature");
        int line = 0;
        
        if (placemarks != null) {
	        double total = placemarks.size();
	        for (Object p: placemarks) {
	        	System.out.println(p);
	
	        	SimpleFeatureImpl feature = (SimpleFeatureImpl)p;
	        	
	        	System.out.println("\t"+feature.getID());
	        	System.out.println("\t"+feature.getName());
	        	
	        	System.out.println("\t"+feature.getAttributes());
	        	for (Object attRaw: feature.getAttributes()) {
	        		System.out.println("\t\t"+attRaw);
	        	}
	        	System.out.println("\t"+feature.getDefaultGeometry());
	        	
	        	ArrayList<DataCell> cells = new ArrayList<DataCell>(2+f.getProperties().size());
	        	
	        	// add id
	        	cells.add(StringCellFactory.create(feature.getID()));
	        	
	        	// add geometry
	        	if (feature.getDefaultGeometry() == null) {
	        		logger.warn("ignoring a feature which has no geometry: "+feature);
	        		continue;
	        	}
	        	cells.add(StringCellFactory.create(feature.getDefaultGeometry().toString()));
	        	
	        	//System.out.println(feature.getUserData());
	        	
	        	// add other fields
	        	for (Property property: f.getProperties()) {
	        		final String name = property.getName().toString();
	        		if ("Feature".equals(name) ||  // TODO a set
	            			"LookAt".equals(name) ||
	            			"Style".equals(name) || 
	            			"Region".equals(name)
	            			)
	            			continue;
	        		final Class<?> type = property.getType().getBinding();
	        		final Object value = feature.getProperty(name).getValue();
	        		
	        		DataCell resultCell = null;
	        		if (value == null) {
	        			resultCell = missing;
	        		} else if (type.equals(Integer.class)) {
	        			resultCell = IntCellFactory.create(value.toString());
	        		} else if (type.equals(String.class)) {
	        			resultCell = StringCellFactory.create(value.toString());
	        		} else if (type.equals(Long.class)) {
	        			resultCell = LongCellFactory.create(value.toString());
	        		} else if (type.equals(Double.class) || type.equals(Float.class)) {
	        			resultCell = DoubleCellFactory.create(value.toString());
	        		} else if (type.equals(Boolean.class)) {
	        			resultCell = BooleanCellFactory.create(value.toString());
	        		} else {
	        			resultCell = StringCellFactory.create(value.toString());
	        		} 
	        		cells.add(resultCell);
	        	}
	        	
				container.addRowToTable(
	        			new DefaultRow(
		        			new RowKey("Row_" + line), 
		        			cells
		        			)
	        			);
				
	            exec.checkCanceled();
	
	        	line++;
	        	exec.setProgress(
	        			(double)line/total, 
	        			"reading KML entity "+line
	        			);
	        }
	        
        }
        		
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{ out };
    }

    
    protected DataTableSpec createDataTableSpec(SimpleFeature f) {
    	
    	List<DataColumnSpec> specs = new ArrayList<DataColumnSpec>(f.getProperties().size()+2);
    	
    	// add column with id
    	specs.add(new DataColumnSpecCreator(
    			"id", 
    			StringCell.TYPE
    			).createSpec());
    	
    	// create a column with the geometry
    	{
	    	
			DataColumnSpecCreator creatorGeom = new DataColumnSpecCreator(
	    			SpatialUtils.GEOMETRY_COLUMN_NAME, 
	    			StringCell.TYPE
	    			);
			Map<String,String> properties = new HashMap<String, String>();
			CoordinateReferenceSystem crs;
			try {
				crs = CRS.decode("EPSG:4326"); // WGS84 
			} catch (FactoryException e) {
				throw new RuntimeException("unable to find the Coordinate Reference System EPSG:4326. This error should not happen. Please report this bug for solving.");
			}
			properties.put(SpatialUtils.PROPERTY_CRS_CODE, SpatialUtils.getStringForCRS(crs));
			properties.put(SpatialUtils.PROPERTY_CRS_WKT, crs.toWKT());
			DataColumnProperties propertiesKWT = new DataColumnProperties(properties);
			creatorGeom.setProperties(propertiesKWT);
			specs.add(creatorGeom.createSpec());
    	}
		
    	// create one column per property
    	
    	
    	//System.out.println(f.getAttributes());
    	
    	Set<String> foundNames = new HashSet<String>();
    	for (Property property: f.getProperties()) {
    		String name = property.getName().toString();
    		if ("Feature".equals(name) ||  // TODO a set
    			"LookAt".equals(name) ||
    			"Style".equals(name) || 
    			"Region".equals(name)
    			)
    			continue;
    		System.out.println(name);
    		if (!foundNames.add(name)) {
    			
    			int i = 1;
    			do {
    				i++;
    			} while (foundNames.contains(name+"("+i+")"));
    			
    			logger.warn("there was already a property named \""+name+"; we will rename this one "+name+"("+i+")");
    			name = name + "(" + i + ")";
    		}
    		final Class<?> type = property.getType().getBinding();
    		System.out.println("\t"+type);
    		DataType knimeType = null;
    		if (type.equals(Integer.class)) {
    			knimeType = IntCell.TYPE;
    		} else if (type.equals(String.class)) {
    			knimeType = StringCell.TYPE;
    		} else if (type.equals(Long.class)) {
    			knimeType = LongCell.TYPE;
    		} else if (type.equals(Double.class) || type.equals(Float.class)) {
    			knimeType = DoubleCell.TYPE;
    		} else if (type.equals(Boolean.class)) {
    			knimeType = BooleanCell.TYPE;
    		} else {
    			logger.warn("The type of KML property "+name+" is not supported ("+property.getType()+"); we will convert it to String");
    			knimeType = StringCell.TYPE;
    		} 
    		specs.add(new DataColumnSpecCreator(
	    			name, 
	    			knimeType
	    			).createSpec());
    	}
		
        return new DataTableSpec(
        		"KML entities",
        		specs.toArray(new DataColumnSpec[specs.size()])
        		);
    }
    
	/**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
   
    	// attempts to read the file and create the corresponding specs
    	// will fail if the file is not defined, 
    	// or not valid
    	
        return new DataTableSpec[]{ createDataTableSpec(decodeFileFromKML()) };
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

	@Override
	protected void reset() {
		
	}
}

