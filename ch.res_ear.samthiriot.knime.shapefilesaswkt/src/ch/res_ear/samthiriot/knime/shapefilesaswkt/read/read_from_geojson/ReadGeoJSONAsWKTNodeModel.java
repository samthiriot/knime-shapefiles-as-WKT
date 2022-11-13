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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_geojson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.CRS;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
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
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.FeaturesDecodingUtils;
import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;


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
public class ReadGeoJSONAsWKTNodeModel extends NodeModel {
    
    /**
	 * The logger is used to print info/warning/error messages to the KNIME console
	 * and to the KNIME log file. Retrieve it via 'NodeLogger.getLogger' providing
	 * the class of this node model.
	 */
	private static final NodeLogger logger = NodeLogger.getLogger(ReadGeoJSONAsWKTNodeModel.class);


    private final SettingsModelString m_file = new SettingsModelString("filename", null);
    private final SettingsModelString m_crs = new SettingsModelString("CRS", "EPSG:4326"); // WGS84

    // TODO make that a parameter
    private static int SAMPLE_LINES_JSON = 100;
    
	/**
	 * Constructor for the node model.
	 */
	protected ReadGeoJSONAsWKTNodeModel() {
        super(0, 1);
	}


	/**
	 * Opens the file, and creates an iterator; return this iterator.
	 * Please remind closing it.
	 * @return
	 * @throws InvalidSettingsException
	 * @throws IOException 
	 */
	protected FeatureIterator<SimpleFeature> getFeaturesIterator() throws InvalidSettingsException, IOException {
	

    	// open the file

    	// retrieve parameters
        CheckUtils.checkSourceFile(m_file.getStringValue());
        
        // identify the file containing the KML (possibly with knime:// protocol)
        URL filename;
		try {
			filename = FileUtil.toURL(m_file.getStringValue());
		} catch (InvalidPathException | MalformedURLException e2) {
			e2.printStackTrace();
			throw new InvalidSettingsException("unable to open URL "+m_file.getStringValue()+": "+e2.getMessage());
		}
        
        if (filename == null)
        	throw new InvalidSettingsException("no file defined");
       
        
        // open the file content
        InputStream inputStream;
		try {
			inputStream = FileUtil.openStreamWithTimeout(filename);
		} catch (IOException e2) {
			e2.printStackTrace();
			throw new IllegalArgumentException("unable to open the URL "+filename+": "+e2.getMessage());
		}
	
		
    	FeatureJSON io = new FeatureJSON();
    	// TODO io.readCRS(inputStream)
    	return io.streamFeatureCollection(inputStream);
    	
	}
	
	/**
	 * Decodes the first features from the GML, 
	 * in order to detect what features are available there.
	 * 
	 * @throws InvalidSettingsException
	 * @throws IOException 
	 */
	protected DataTableSpec decodeSpecsFromGeoJSON()
					throws InvalidSettingsException, IOException {

    	FeatureIterator<SimpleFeature> iter = getFeaturesIterator();
		
        // associate each property name with the corresponding column spec 
        Map<String,DataColumnSpec> name2spec = new LinkedHashMap<>();
        // add a column for id
        name2spec.put(
        		"id",
        		new DataColumnSpecCreator(
	    			"id", 
	    			StringCell.TYPE
	    			).createSpec()
        		);

        // CRS is always WGS84 for KML
		CoordinateReferenceSystem crs;
		try {
			crs = CRS.decode(m_crs.getStringValue()); 
		} catch (FactoryException e) {
			throw new RuntimeException("unable to find the Coordinate Reference System "+m_crs.getStringValue()+". This error should not happen. Please report this bug for solving.");
		}      
    	// we can now declare the geometry column
    	DataColumnSpecCreator creatorGeom = new DataColumnSpecCreator(
    			SpatialUtils.GEOMETRY_COLUMN_NAME, 
    			StringCell.TYPE
    			);
		Map<String,String> properties = new HashMap<String, String>();
		properties.put(SpatialUtils.PROPERTY_CRS_CODE, SpatialUtils.getStringForCRS(crs));
		properties.put(SpatialUtils.PROPERTY_CRS_WKT, crs.toWKT());
		DataColumnProperties propertiesKWT = new DataColumnProperties(properties);
		creatorGeom.setProperties(propertiesKWT);
		name2spec.put(SpatialUtils.GEOMETRY_COLUMN_NAME, creatorGeom.createSpec());
		
		int done = 0;
		while( iter.hasNext() ){
		    SimpleFeature feature = iter.next();
		    
		    CoordinateReferenceSystem currentCRS = feature.getType().getCoordinateReferenceSystem();
		    if (currentCRS != null && !crs.equals(currentCRS)) {
			    	throw new InvalidSettingsException("invalid GeoJSON file: found several different Coordinate Reference System for different features");
		    }
		    
        	for (Property property: feature.getProperties()) {
        		final String name = property.getName().toString();
        		
        		// special case: the col name geometry is assumed to contain the geometry
        		if ("geometry".equals(name))
        			continue;
        		
        		DataColumnSpec columnSpec = FeaturesDecodingUtils.getColumnSpecForFeatureProperty(
						property,
						name,
						getLogger());
        		
        		DataColumnSpec previousSpec = name2spec.get(name);
        		if (previousSpec != null) {
        			if (!previousSpec.getType().equals(columnSpec.getType()))
	        			throw new InvalidSettingsException(
	        					"invalid GML file: the property "+name+" has different types "+
	        					previousSpec.getType()+" and "+columnSpec.getType());
        		} else {
        			name2spec.put(name, columnSpec);
        		}
        		
        	}
		    if (done++ >= SAMPLE_LINES_JSON)
		    	break;
		}
		iter.close();
		
        return new DataTableSpec(
        		"GeoJSON entities",
        		name2spec.values().toArray(new DataColumnSpec[name2spec.size()])
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
    	try {
			DataTableSpec specs = decodeSpecsFromGeoJSON();
	        return new DataTableSpec[]{ specs };

		} catch (InvalidSettingsException e) {
			e.printStackTrace();
			throw e;
		} catch (IOException e) {
			throw new RuntimeException("Error when reading data: "+e.getMessage(), e);
		}
    	
        
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
    	// create the data table specs
        final DataTableSpec tableSpec = decodeSpecsFromGeoJSON();
        
        // the container of read entities
        final BufferedDataContainer container = exec.createDataContainer(tableSpec);

        final DataCell missing = new MissingCell("was undefined in GML");
        
	    CoordinateReferenceSystem crs;
	    try {
			crs = CRS.decode(m_crs.getStringValue()); 
		} catch (FactoryException e) {
			throw new RuntimeException("unable to find the Coordinate Reference System "+m_crs.getStringValue()+". This error should not happen. Please report this bug for solving.");
		}      
	    
	    //PrecisionModel precisionGeom = new PrecisionModel(PrecisionModel.FLOATING);
	    
	    FeatureIterator<SimpleFeature> iter = getFeaturesIterator();
        int line = 0;
        try {
        	String lastGeometryType = null;
        	boolean errorGeomType = false;
    		while( iter.hasNext() ) {
    		    SimpleFeature feature = iter.next();
   		    
	        	ArrayList<DataCell> cells = new ArrayList<DataCell>(tableSpec.getNumColumns());

    		    // each feature has its own CRS; let's check it is oK
    		    CoordinateReferenceSystem currentCRS = feature.getType().getCoordinateReferenceSystem();
    		    if (currentCRS != null && !crs.equals(currentCRS)) {
    		    	throw new InvalidSettingsException("invalid GML file: found several different Coordinate Reference System for different features");
    		    }
        	
	        	// skip empty geometries
	        	if (feature.getDefaultGeometry() == null) {
	        		getLogger().warn("ignoring a feature which has no geometry: "+feature);
	        		continue;
	        	}
	        	   
    		    // for each of the expected columns, try to find the corresponding cell
    		    for (int col = 0; col < tableSpec.getNumColumns(); col++) {
    		    	DataColumnSpec colSpec = tableSpec.getColumnSpec(col);
    		    	final String name = colSpec.getName();
    		    	String givenName;
            		// special case: the col name geometry is assumed to contain the geometry
            		if ("geometry".equals(name))
            			givenName = SpatialUtils.GEOMETRY_COLUMN_NAME;
            		else 
            			givenName = name;
            		
    		    	Property property = feature.getProperty(name);
    		    	
    		    	Geometry geom = (Geometry) feature.getDefaultGeometry();
    		    	if (lastGeometryType == null)
    		    		lastGeometryType = geom.getGeometryType();
    		    	else if (!errorGeomType && !lastGeometryType.equals(geom.getGeometryType())) {
    		    		setWarningMessage("There are several geometry types in this table. Some manipulations will not be available, such as shapefile exportation.");
    		    		errorGeomType = true;
    		    	}
    		    	
    		    	DataCell cell = null;
    		    	
    		    	if ("id".equals(givenName)) 
    		        	cell = StringCellFactory.create(feature.getID());
    		    	else if (SpatialUtils.GEOMETRY_COLUMN_NAME.equals(givenName)) 
    		        	cell = StringCellFactory.create(geom.toString());
    		    	else if (property != null) 
    		    		cell = FeaturesDecodingUtils.getDataCellForProperty(property, feature);
    		    	
    		    	if (cell == null)
    		    		cells.add(missing);
    		    	else
    		    		cells.add(cell);
    		    }
    		    
				container.addRowToTable(
	        			new DefaultRow(
		        			new RowKey("Row_" + line), 
		        			cells
		        			)
	        			);
				
				if (line++ % 10 == 0) {
					exec.checkCanceled();
					exec.setMessage("reading GML entity "+line);
				}
    		}
    		
        } finally {
        	if (iter != null)
        		iter.close();
        }
        	
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        
        // add flow variables for the CRS
        pushFlowVariableString("CRS_code", SpatialUtils.getStringForCRS(crs));
        pushFlowVariableString("CRS_WKT", crs.toWKT());
        
        return new BufferedDataTable[]{ out };
        
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        
    	m_file.saveSettingsTo(settings);
    	m_crs.saveSettingsTo(settings);
    	
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        m_file.loadSettingsFrom(settings);
        m_crs.loadSettingsFrom(settings);
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    	m_file.validateSettings(settings);
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

	@Override
	protected void reset() {
		
	}
}

