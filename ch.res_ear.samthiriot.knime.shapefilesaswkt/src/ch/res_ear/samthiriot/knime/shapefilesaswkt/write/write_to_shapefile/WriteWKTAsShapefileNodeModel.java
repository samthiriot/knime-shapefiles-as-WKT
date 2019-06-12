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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.write.write_to_shapefile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.geotools.data.DataStore;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
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
import ch.res_ear.samthiriot.knime.shapefilesaswkt.DataTableToGeotoolsMapperForShapefile;
import ch.res_ear.samthiriot.knime.shapefilesaswkt.NodeWarningWriter;
import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;


/**
 * This is the model implementation of WriteWKTAsShapefile.
 * Stores the WKT data as a shapefile.
 *
 * @author Samuel Thiriot
 */
public class WriteWKTAsShapefileNodeModel extends NodeModel {
    
    /**
     * Count of entities to write at once
     */
    final static int BUFFER = 5000;
	
    final static int MAX_COLUMNS = 255;
	
    private final SettingsModelString m_file = new SettingsModelString("filename", null);


    /**
     * Constructor for the node model.
     */
    protected WriteWKTAsShapefileNodeModel() {
    
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(
			    		final BufferedDataTable[] inData,
			            final ExecutionContext exec) throws Exception {

    	final BufferedDataTable inputPopulation = inData[0];
    	

    	if (!SpatialUtils.hasGeometry(inputPopulation.getDataTableSpec()))
    		throw new IllegalArgumentException("the input table contains no spatial data (no column named "+SpatialUtils.GEOMETRY_COLUMN_NAME+")");
    	
    	if (!SpatialUtils.hasCRS(inputPopulation.getDataTableSpec()))
    		throw new IllegalArgumentException("the input table contains spatial data but no Coordinate Reference System");
    	    	
    	CoordinateReferenceSystem crsOrig = SpatialUtils.decodeCRS(inputPopulation.getSpec());
    	
    	URL url;
		try {
			
			url = FileUtil.toURL(m_file.getStringValue());
		} catch (InvalidPathException | MalformedURLException e2) {
			e2.printStackTrace();
			throw new InvalidSettingsException("unable to open URL "+m_file.getStringValue()+": "+e2.getMessage());
		}
        
    	File file = FileUtil.getFileFromURL(url);
        
    	NodeWarningWriter warnings = new NodeWarningWriter(getLogger());
    	
    	// copy the input population into a datastore
    	exec.setMessage("storing entities");
        DataStore datastore = SpatialUtils.createDataStore(file, true);
        
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("entities");
        builder.setCRS(crsOrig); 
        
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
        if (inputPopulation.getDataTableSpec().getNumColumns() > MAX_COLUMNS+1) {
        	int count = (inputPopulation.getDataTableSpec().getNumColumns() - MAX_COLUMNS - 1);
        	warnings.warn("Only "+MAX_COLUMNS+" columns can be stored in a shapefile format; will ignore the "+
        			count + " last one(s)"
        			);
        }
        Set<String> usedNames = new HashSet<>();
        List<DataTableToGeotoolsMapper> mappers = inputPopulation
        												.getDataTableSpec()
        												.stream()
        												.filter(colspec -> !SpatialUtils.GEOMETRY_COLUMN_NAME.equals((colspec.getName())))
        												.limit(MAX_COLUMNS)
        												.map(colspec -> new DataTableToGeotoolsMapperForShapefile(
        														warnings, 
        														colspec, 
        														usedNames))
        												.collect(Collectors.toList());
        // add those to the builder type
        mappers.forEach(mapper -> mapper.addAttributeForSpec(builder));
        
        
        // build the type
        final SimpleFeatureType type = builder.buildFeatureType();
        // get or create the type in the file store 
		try {
			datastore.getSchema(type.getName());
		} catch (IOException e) {
			datastore.createSchema(type);	
		}
		// retrieve it 
		SimpleFeatureSource featureSource = datastore.getFeatureSource(datastore.getNames().get(0));
        if (!(featureSource instanceof SimpleFeatureStore)) {
            throw new IllegalStateException("Modification not supported");
        }
        SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

        // identify the id of the geom column, that we will not use as a standard one
        final int idxColGeom = inputPopulation.getDataTableSpec().findColumnIndex(SpatialUtils.GEOMETRY_COLUMN_NAME);
		
        // prepare classes to create Geometries from WKT
        GeometryFactory geomFactory = JTSFactoryFinder.getGeometryFactory( null );
        WKTReader reader = new WKTReader(geomFactory);
        
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);

        // the buffer of spatial features to be added soon (it's quicker to add several lines than only one)
		List<SimpleFeature> toStore = new ArrayList<>(BUFFER);
		
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
	        	for (int i=0; i<row.getNumCells() && colId < mappers.size(); i++) {
	        		
	        		if (i == idxColGeom) {
	        			// skip the column with geom
	        		} else {
	        			// process as a standard column
	        			featureBuilder.add(mappers.get(colId++).getValue(row.getCell(i)));
	        		}
	        	}
	        	
	        	// build this feature
	            SimpleFeature feature = featureBuilder.buildFeature(row.getKey().getString());
	            // add this feature to the buffer
	            toStore.add(feature);
	            if (toStore.size() >= BUFFER) {
	        		exec.checkCanceled();
	            	featureStore.addFeatures( new ListFeatureCollection( type, toStore));
	            	toStore.clear();
	            }
	            
	            if (currentRow % 10 == 0) {
	        		exec.setProgress((double)currentRow / inputPopulation.size(), "processing row "+currentRow);
	        		exec.checkCanceled();
	            }
	            currentRow++;
	            
	        }
	
	
	        // store last lines
	        if (!toStore.isEmpty()) {
	        	featureStore.addFeatures( new ListFeatureCollection( type, toStore));
	        }
	        exec.setProgress(1.0);

        } finally {
        	if (itRow != null)
        		itRow.close();
        	
            // close datastore
            datastore.dispose();
        	
        }
        
        setWarningMessage(warnings.buildWarnings());

        return new BufferedDataTable[]{};
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	
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


}

