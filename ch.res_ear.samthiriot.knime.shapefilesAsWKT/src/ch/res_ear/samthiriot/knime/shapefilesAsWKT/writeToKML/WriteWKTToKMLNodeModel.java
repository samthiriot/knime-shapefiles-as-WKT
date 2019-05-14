package ch.res_ear.samthiriot.knime.shapefilesAsWKT.writeToKML;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.namespace.QName;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.kml.KMLConfiguration;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Encoder;
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

import ch.res_ear.samthiriot.knime.shapefilesAsWKT.DataTableToGeotoolsMapper;
import ch.res_ear.samthiriot.knime.shapefilesAsWKT.NodeWarningWriter;
import ch.res_ear.samthiriot.knime.shapefilesAsWKT.SpatialUtils;
import ch.res_ear.samthiriot.knime.shapefilesAsWKT.writeToShapefile.WriteWKTAsShapefileNodeModel;


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
public class WriteWKTToKMLNodeModel extends NodeModel {

    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(WriteWKTAsShapefileNodeModel.class);
    
    private final SettingsModelString m_file = new SettingsModelString("filename", null);
    private final SettingsModelBoolean m_removeKMLnamespace = new SettingsModelBoolean("removeNamespace", true);
    private final SettingsModelBoolean m_useKML22 = new SettingsModelBoolean("useKMLv22", true);
    
	/**
	 * Constructor for the node model.
	 */
	protected WriteWKTToKMLNodeModel() {
		
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
			throw new IllegalArgumentException("sorry, we can not store more than "+Integer.MAX_VALUE+" with this node.");

    	if (!SpatialUtils.hasGeometry(inputPopulation.getDataTableSpec()))
    		throw new IllegalArgumentException("the input table contains no spatial data (no column named "+SpatialUtils.GEOMETRY_COLUMN_NAME+")");
    	
    	if (!SpatialUtils.hasCRS(inputPopulation.getDataTableSpec()))
    		throw new IllegalArgumentException("the input table contains spatial data but no Coordinate Reference System");
    	    	
    	final boolean paramRemoveKMLnamespace = m_removeKMLnamespace.getBooleanValue();
    	
    	final boolean zipResult = m_file.getStringValue().toLowerCase().endsWith(".kmz");
    	
    	//CoordinateReferenceSystem crsOrig = SpatialUtils.decodeCRS(inputPopulation.getSpec());
    	
    	URL url;
		try {
			
			url = FileUtil.toURL(m_file.getStringValue());
		} catch (InvalidPathException | MalformedURLException e2) {
			e2.printStackTrace();
			throw new InvalidSettingsException("unable to open URL "+m_file.getStringValue()+": "+e2.getMessage());
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
        												.map(colspec -> new DataTableToGeotoolsMapper(warnings, colspec))
        												.collect(Collectors.toList());
        // add those to the builder type
        mappers.forEach(mapper -> mapper.addAttributeForSpec(builder));
        
        // build the type
        final SimpleFeatureType type = builder.buildFeatureType();
    
        // identify the id of the geom column, that we will not use as a standard one
        final int idxColGeom = inputPopulation.getDataTableSpec().findColumnIndex(SpatialUtils.GEOMETRY_COLUMN_NAME);
		
        // prepare classes to create Geometries from WKT
        GeometryFactory geomFactory = JTSFactoryFinder.getGeometryFactory( null );
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
	            SimpleFeature feature = featureBuilder.buildFeature(null);
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

        QName KMLspace = null;
        Configuration configurationEncoder = null;
        if (m_useKML22.getBooleanValue()) {
        	KMLspace = org.geotools.kml.v22.KML.kml;
        	configurationEncoder = new org.geotools.kml.v22.KMLConfiguration();
        } else {
        	KMLspace = org.geotools.kml.KML.kml;
        	configurationEncoder = new KMLConfiguration();
        }
    	
        Encoder encoder = new Encoder(configurationEncoder);
        encoder.setIndenting(true);
        
        exec.setProgress(0.5, "writing entities");
        
        OutputStream os = null;
        try {
        	if (zipResult) {
        		@SuppressWarnings("resource")
				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
        		ZipEntry e = new ZipEntry("doc.kml");
        		zos.putNextEntry(e);
        		os = zos;
        	} else 
        		os = new FileOutputStream(file);
        	
	        if (paramRemoveKMLnamespace) {
	        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	            try {
	              encoder.encode(featureCollection, KMLspace, baos);
	              String out = baos.toString().replaceAll("kml:", "");
	              os.write(out.getBytes());
	            } catch (Exception e) {
	              throw new RuntimeException(e);
	            } 
	        } else {
	        	encoder.encode(featureCollection, KMLspace, os);
	        }
        } finally {
        	if (os != null) {
        		if (zipResult) {
            		((ZipOutputStream)os).closeEntry();
            	}
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
    	
        return new DataTableSpec[]{};
        
	}

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        
    	m_file.saveSettingsTo(settings);
    	m_removeKMLnamespace.saveSettingsTo(settings);
    	m_useKML22.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        
    	m_file.loadSettingsFrom(settings);
    	m_removeKMLnamespace.loadSettingsFrom(settings);
    	m_useKML22.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_file.validateSettings(settings);
    	m_removeKMLnamespace.validateSettings(settings);
    	m_useKML22.validateSettings(settings);
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

