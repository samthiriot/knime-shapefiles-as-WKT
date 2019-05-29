package ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_gml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.wfs.GML;
import org.geotools.wfs.GML.Version;
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
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.FileUtil;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.SAXException;

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
public class ReadGMLAsWKTNodeModel extends NodeModel {

	/**
	 * Count of features to decode from the GML file to detect the format
	 */
	private static final int SAMPLE_LINES_GML = 10;
	
    private final SettingsModelString m_file = new SettingsModelString("filename", null);

    protected final SettingsModelBoolean m_skipStandardColumns = new SettingsModelBoolean("skip_standard", true);

    /**
     * There are properties which are automatically added by geotools; 
     * its better to ignore them.
     */
    private static final Set<String> IGNORED_PROPERTIES = new HashSet<>(Arrays.asList(
															    		"Feature", 
																		"LookAt",
																		"Style", 
																		"Region",
																		"description",
																		"boundedBy",
																		"name"
    																				));
    
    
	/**
	 * Constructor for the node model.
	 */
	protected ReadGMLAsWKTNodeModel() {
        super(0, 1);
	}
	
	/**
	 * Opens the file, and creates an iterator; return this iterator.
	 * Please remind closing it.
	 * @return
	 * @throws InvalidSettingsException
	 */
	protected SimpleFeatureIterator getFeaturesIterator() throws InvalidSettingsException {
	
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
		
		/*
		String filenameSchema = null;
        try {
			filenameSchema = FilenameUtils.removeExtension(
					new File(filename.toURI()).getCanonicalPath()
					) +".xsd";
		} catch (IOException | URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RuntimeException("error when trying to search for the schema file");
		}
        File schemaFile = new File(filenameSchema);
        if (schemaFile.exists() && schemaFile.isFile() && schemaFile.canRead()) {
        	// TODO load 
    	   final QName featureName = new QName(typeName.getNamespaceURI(), typeName.getLocalPart());

           String namespaceURI = featureName.getNamespaceURI();
           String uri = schemaLocation.toExternalForm();

           Configuration wfsConfiguration =
                   new org.geotools.gml3.ApplicationSchemaConfiguration(namespaceURI, uri);

           FeatureType parsed = GTXML.parseFeatureType(wfsConfiguration, featureName, crs);
           // safely cast down to SimpleFeatureType
           SimpleFeatureType schema = DataUtilities.simple(parsed);
        }
        */
		
		/*
		GML gml = new GML(Version.WFS1_0);
		gml.setCoordinateReferenceSystem( DefaultGeographicCRS.WGS84 );

		Name typeName = new NameImpl("http://www.openplans.org/topp", "states");
		SimpleFeatureType featureType = gml.decodeSimpleFeatureType(schemaLocation, typeName );
		*/
		
		GML gml = new GML(Version.GML3); // Version.GML3
		gml.setLegacy(true);
		
		SimpleFeatureIterator iter = null;
		try {
			iter = gml.decodeFeatureIterator(inputStream);
		} catch (IOException | ParserConfigurationException | SAXException e) {
			throw new InvalidSettingsException("unable to decode the file as GML: "+e.getMessage(), e);
		}
		
		return iter;
	}
	/**
	 * Decodes the first features from the GML, 
	 * in order to detect what features are available there.
	 * 
	 * @throws InvalidSettingsException
	 */
	protected DataTableSpec decodeSpecsFromGML()
					throws InvalidSettingsException {

		final boolean skipStandardColumns = m_skipStandardColumns.getBooleanValue();
		
		SimpleFeatureIterator iter = getFeaturesIterator();
		
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

	    CoordinateReferenceSystem crs = null;
        
		int done = 0;
		while( iter.hasNext() ){
		    SimpleFeature feature = iter.next();
		    
		    CoordinateReferenceSystem currentCRS = feature.getType().getCoordinateReferenceSystem();
		    if (currentCRS != null) {
			    if (crs == null) {
			    	// use this CRS as the current CRS
			    	crs = currentCRS;
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
					getLogger().info("detected Coordinate Reference System "+crs);
			    } else if (!crs.equals(currentCRS)) {
			    	throw new InvalidSettingsException("invalid GML file: found several different Coordinate Reference System for different features");
			    }
		    }
		    
        	for (Property property: feature.getProperties()) {
        		final String name = property.getName().toString();
        		if (skipStandardColumns && IGNORED_PROPERTIES.contains(name)) {
        			getLogger().info("will skip column "+name+" which is assumed to be automatically created but useless");
        			continue;
        		}
        		
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
		    if (done++ >= SAMPLE_LINES_GML)
		    	break;
		}
		iter.close();
		
        return new DataTableSpec(
        		"GML entities",
        		name2spec.values().toArray(new DataColumnSpec[name2spec.size()])
        		);
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
   
    	// create the data table specs
        final DataTableSpec tableSpec = decodeSpecsFromGML();
        
        // the container of read entities
        final BufferedDataContainer container = exec.createDataContainer(tableSpec);

        final DataCell missing = new MissingCell("was undefined in GML");
        
	    CoordinateReferenceSystem crs = null;

	    //PrecisionModel precisionGeom = new PrecisionModel(PrecisionModel.FLOATING);
	    
        SimpleFeatureIterator iter = getFeaturesIterator();
        int line = 0;
        try {
    		while( iter.hasNext() ) {
    		    SimpleFeature feature = iter.next();
   		    
	        	ArrayList<DataCell> cells = new ArrayList<DataCell>(tableSpec.getNumColumns());

    		    // each feature has its own CRS; let's check it is oK
    		    CoordinateReferenceSystem currentCRS = feature.getType().getCoordinateReferenceSystem();
    		    if (crs == null) {
    		    	// use this CRS as the current CRS
    		    	crs = currentCRS;
    		    	getLogger().info("detected as Coordinate Reference System: "+currentCRS);
    		    } else if (!crs.equals(currentCRS)) {
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
    		    	Property property = feature.getProperty(name);
    		    	
    		    	Geometry geom = (Geometry) feature.getDefaultGeometry();
    		    	//Geometry geomPrecise = GeometryPrecisionReducer.reduce(geom, precisionGeom);
    		    		
    		    	DataCell cell = null;
    		    	
    		    	if ("id".equals(name)) 
    		        	cell = StringCellFactory.create(feature.getID());
    		    	else if (SpatialUtils.GEOMETRY_COLUMN_NAME.equals(name)) 
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
        return new BufferedDataTable[]{ out };
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
    	DataTableSpec specs = decodeSpecsFromGML();
    	
        return new DataTableSpec[]{ specs };
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        
    	m_file.saveSettingsTo(settings);
    	m_skipStandardColumns.saveSettingsTo(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        m_file.loadSettingsFrom(settings);
        m_skipStandardColumns.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    	m_file.validateSettings(settings);
    	m_skipStandardColumns.validateSettings(settings);
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

