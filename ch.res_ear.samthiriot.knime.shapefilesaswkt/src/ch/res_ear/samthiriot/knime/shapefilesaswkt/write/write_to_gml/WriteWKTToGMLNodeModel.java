package ch.res_ear.samthiriot.knime.shapefilesaswkt.write.write_to_gml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.xmlbeans.impl.common.XMLChar;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.wfs.GML;
import org.geotools.wfs.GML.Version;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.FileUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.DataTableToGeotoolsMapper;
import ch.res_ear.samthiriot.knime.shapefilesaswkt.NodeWarningWriter;
import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;
import ch.res_ear.samthiriot.knime.shapefilesaswkt.write.write_to_shapefile.WriteWKTAsShapefileNodeModel;


/**
 * This is an example implementation of the node model of the
 * "WriteWKTToKML" node.
 * 
 * This example node performs simple number formatting
 * ({@link String#format(String, Object...)}) using a user defined format string
 * on all double columns of its input table.
 *
 * @author Samuel Thiriot
 */
public class WriteWKTToGMLNodeModel extends NodeModel {

    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(WriteWKTAsShapefileNodeModel.class);
    
    private final SettingsModelString m_file = new SettingsModelString("filename", null);
    private final SettingsModelString m_version = new SettingsModelString("version", "GML v3");
    protected final SettingsModelBoolean m_writeSchema = new SettingsModelBoolean("write_schema", true);

	/**
	 * Constructor for the node model.
	 */
	protected WriteWKTToGMLNodeModel() {
		
        super(1, 0);
	}

	
	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		final BufferedDataTable inputPopulation = inData[0];
    	
		if (inputPopulation.size() > Integer.MAX_VALUE)
			throw new IllegalArgumentException(
					"sorry, we can not store more than "+Integer.MAX_VALUE+" with this node.");

    	if (!SpatialUtils.hasGeometry(inputPopulation.getDataTableSpec()))
    		throw new IllegalArgumentException(
    				"the input table contains no spatial data (no column named "+SpatialUtils.GEOMETRY_COLUMN_NAME+")");
    	
    	if (!SpatialUtils.hasCRS(inputPopulation.getDataTableSpec()))
    		throw new IllegalArgumentException(
    				"the input table contains spatial data but no Coordinate Reference System");
    	    	
    	
    	//CoordinateReferenceSystem crsOrig = SpatialUtils.decodeCRS(inputPopulation.getSpec());
    	
    	URL url;
		try {
			url = FileUtil.toURL(m_file.getStringValue());
		} catch (InvalidPathException | MalformedURLException e2) {
			e2.printStackTrace();
			throw new InvalidSettingsException(
					"unable to open URL "+m_file.getStringValue()+": "+e2.getMessage());
		}
        
    	File file = FileUtil.getFileFromURL(url);
        
    	// copy the input population into a datastore
    	exec.setMessage("encoding entities");
        
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("entities");
        
        // mandatory; the target CRS is always the same for KML
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84; 
        builder.setCRS(crs);

        //builder.setCRS(crsOrig); 
        
        // TODO improve: create different files for different geom types (?)
        Class<?> geomClassToBeStored = SpatialUtils.detectGeometryClassFromData(	
        										inputPopulation, 
        										SpatialUtils.GEOMETRY_COLUMN_NAME);
        
        // add attributes in order
        builder.add(
        		SpatialUtils.GEOMETRY_COLUMN_NAME, 
        		geomClassToBeStored
        		);
        
        // create mappers
    	NodeWarningWriter warnings = new NodeWarningWriter(getLogger());

        List<DataTableToGeotoolsMapper> mappers = inputPopulation
        												.getDataTableSpec()
        												.stream()
        												.filter(colspec -> !SpatialUtils.GEOMETRY_COLUMN_NAME.equals((colspec.getName())))
        												.map(colspec -> new GMLDataTableToGeotoolsMapper(warnings, colspec))
        												.collect(Collectors.toList());
        // add those to the builder type
        mappers.forEach(mapper -> mapper.addAttributeForSpec(builder));
        
        // build the type
        final SimpleFeatureType type = builder.buildFeatureType();
    
        // identify the id of the geom column, that we will not use as a standard one
        final int idxColGeom = inputPopulation.getDataTableSpec().findColumnIndex(SpatialUtils.GEOMETRY_COLUMN_NAME);
		
        // prepare classes to create Geometries from WKT
        GeometryFactory geomFactory = JTSFactoryFinder.getGeometryFactory( null );
        //geomFactory.getPrecisionModel().
        WKTReader reader = new WKTReader(geomFactory);
        
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);

        // the buffer of spatial features to be added soon (it's quicker to add several lines than only one)
		List<SimpleFeature> toStore = new ArrayList<>((int)inputPopulation.size());
		
        CloseableRowIterator itRow = inputPopulation.iterator();
        try {
	        int currentRow = 0;
	        while (itRow.hasNext()) {
	        	final DataRow row = itRow.next();
	        	
	        	// process the geom column
	        	final DataCell cellGeom = row.getCell(idxColGeom);
	        	if (cellGeom.isMissing()) {
	        		// no geometry
	        		continue; // skip lines without geom
	        	}
	        	try {
		
		        	Geometry geom = reader.read(cellGeom.toString());
		        	featureBuilder.add(geom);
	
				} catch (ParseException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
	        	
	        	int colId = 0;
	        	for (int i=0; i<row.getNumCells(); i++) {
	        		
	        		if (i == idxColGeom) {
	        			// skip the column with geom
	        		} else {
	        			// process as a standard column
	        			featureBuilder.add(mappers.get(colId++).getValueNoNull(row.getCell(i)));
	        		}
	        	}
	        	
	        	// build this feature
	            SimpleFeature feature = featureBuilder.buildFeature(row.getKey().toString());
	            // add this feature to the buffer
	            toStore.add(feature);
	            
	            if (currentRow % 10 == 0) {
	        		exec.setProgress(0.4*(double)currentRow / inputPopulation.size(), "encoding entity "+currentRow);
	        		exec.checkCanceled();
	            }
	            currentRow++;
	            
	        }
	
	
	        // store last lines
	        if (!toStore.isEmpty()) {
	        }
	        exec.setProgress(0.4);

        } finally {
        	if (itRow != null)
        		itRow.close();
        	
        }

        // store geometries into a feature list
    	exec.setMessage("storing entities");

        ListFeatureCollection featureCollection = new ListFeatureCollection(type, toStore);

        GML gml = null;
        
        javax.xml.namespace.QName qName;
        org.geotools.xsd.Configuration config;
        
        final String version = m_version.getStringValue(); 
        if ("GML v2".equals(version)) {
            gml = new GML(Version.GML2);
            gml.setLegacy(true);
            //qName = org.geotools.gml2.GML._FeatureCollection;
            //config = new org.geotools.gml2.GMLConfiguration();
            qName = null;
            config = null;
            
        } else if ("GML v3".equals(version)) {
        	//configurationEncoder = new org.geotools.gml3.GMLConfiguration();
        	//xmlspace = new org.geotools.gml3.GML();
        	gml = new GML(Version.WFS1_1);
        	gml.setNamespace("location", "location.xsd");
        	
            qName = org.geotools.gml3.GML.FeatureCollection;
            config = new org.geotools.gml3.GMLConfiguration();
            
            // TODO 32 https://www.javatips.net/api/spatial_statistics_for_geotools_udig-master/uDig/org.locationtech.udig.processingtoolbox/src/org/locationtech/udig/processingtoolbox/tools/FormatTransformer.java
            
        } else {
        	throw new InvalidSettingsException("unknown GML version "+version);
        }
        
        
        org.geotools.xsd.Encoder encoder = null;
        if (config != null) {
        	encoder = new org.geotools.xsd.Encoder(config);
            encoder.setIndenting(true);
            encoder.setIndentSize(2);
        }
        //gml.setBaseURL(file.toURI().toURL());
        gml.setCoordinateReferenceSystem(crs);
        final URL baseurl = file.getParentFile().toURI().toURL();
        
        String filenameSchema = null;
        if (m_writeSchema.getBooleanValue()) {
	        exec.setProgress(0.5, "writing the schema");
	        filenameSchema = FilenameUtils.removeExtension(file.getAbsolutePath())+".xsd";
	        getLogger().info("writing the GML schema into "+filenameSchema);
	        FileOutputStream xsd = new FileOutputStream(filenameSchema);
	        
	        gml.setBaseURL(new URL("http://schemas.opengis.net"));

	        try {
		        gml.encode(xsd, type);
		        
		        // we use the WFS features to write the type. That's a bit weird but the official way does not work.
		        //final javax.xml.namespace.QName typeQName = new javax.xml.namespace.QName(type.getName().getNamespaceURI(), type.getName().getLocalPart());
		        //encoder.setNamespace("location", url.toExternalForm());
		        //final javax.xml.namespace.QName typeQName = new javax.xml.namespace.QName(type.getName().getNamespaceURI(), type.getTypeName());
		        
		        //encoder.encode(type, typeQName, xsd);

	        } finally {
	        	xsd.close();
	        }
        }
        
        // write the entities as GML
        exec.setProgress(0.6, "writing entities");
        getLogger().info("writing the GML features into "+file);
        OutputStream os = null;
        try {
    		os = new FileOutputStream(file);

    		if (config != null) {
    			encoder.setSchemaLocation(baseurl.toExternalForm(), FilenameUtils.getName(filenameSchema));
    			//featureCollection.getSchema().getCoordinateReferenceSystem()
    			encoder.encode(featureCollection, qName, os);
    		} else {
    			if (filenameSchema != null)
    	    		gml.setNamespace(baseurl.toExternalForm(), FilenameUtils.getName(filenameSchema));
    			
    			gml.encode(os, featureCollection);
    			
    		}
            
        } finally {
        	if (os != null) {
        		os.close();
        	}
            
        }

        exec.setProgress(1);
        
        setWarningMessage(warnings.buildWarnings());

        return new BufferedDataTable[]{};
        
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		
		DataTableSpec specs = inSpecs[0];

    	if (m_file.getStringValue() == null)
    		throw new IllegalArgumentException("No filename was provided");

    	// check the input table contains a geometry
    	if (!SpatialUtils.hasGeometry(specs))
    		throw new IllegalArgumentException("the input table contains no spatial data (no column named "+SpatialUtils.GEOMETRY_COLUMN_NAME+")");
    	
    	if (!SpatialUtils.hasCRS(specs))
    		throw new IllegalArgumentException("the input table contains spatial data but no Coordinate Reference System");
    	
    	// check the parameters include a filename
		try {
			FileUtil.toURL(m_file.getStringValue());
		} catch (InvalidPathException | MalformedURLException e2) {
			e2.printStackTrace();
			throw new InvalidSettingsException("unable to open URL "+m_file.getStringValue()+": "+e2.getMessage());
		}
		
		// ensures the input table does not contain column names with invalid names
		Set<String> invalidColNames = new LinkedHashSet<String>();
		for (String colname: specs.getColumnNames()) {
			if (!XMLChar.isValidName(colname)) 
				invalidColNames.add(colname);
		}
		if (!invalidColNames.isEmpty()) {
			if (invalidColNames.size() == 1)
				throw new IllegalArgumentException(
					"The column \""+invalidColNames.iterator().next()+"\" contains special characters which cannot be stored as GML. "
					+ "Please use the column rename node to remove these special characters first"
					);
			else 
				throw new IllegalArgumentException(
					invalidColNames.size()+" columns contains special characters which cannot be stored as GML. "
					+ "Please use the column rename node to rename these columns: "
					+ String.join(", ", invalidColNames)
					);
		}
    	
        return new DataTableSpec[]{};
        
	}

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        
    	m_file.saveSettingsTo(settings);
    	m_version.saveSettingsTo(settings);
    	m_writeSchema.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        
    	m_file.loadSettingsFrom(settings);
    	m_version.loadSettingsFrom(settings);
    	m_writeSchema.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_file.validateSettings(settings);
    	m_version.validateSettings(settings);
    	m_writeSchema.validateSettings(settings);
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
		
		// nothing to do
	}

}

