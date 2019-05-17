package ch.res_ear.samthiriot.knime.shapefilesAsWKT;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import org.geotools.data.DataStore;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.BasicFeatureTypes;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.FileUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

// see http://docs.geotools.org/latest/userguide/tutorial/feature/csv2shp.html

/**
 * Utilities to manipulate spatial data within KNIME.
 * 
 * @author Samuel Thiriot (EIFER)
 *
 */
public class SpatialUtils {

	public static final String GEOMETRY_COLUMN_NAME = "the_geom";
	
	public static final String PROPERTY_CRS_CODE = "crs code";
	public static final String PROPERTY_CRS_WKT = "crs WKT";
	
	public static final String ATTRIBUTE_NAME_INCREMENTAL_ID = "inc_id";

	public static String getDefaultCRSString() {
		return "EPSG:4326";
	}
	
	public static String getStringForCRS(CoordinateReferenceSystem crs) {
		try {
		    ReferenceIdentifier id = crs.getIdentifiers().iterator().next();
		    return id.getCodeSpace()+":"+id.getCode();
		} catch (NoSuchElementException e) {
			return crs.getName().getCodeSpace()+":"+crs.getName().getCode();
		}

	}
	
	public static CoordinateReferenceSystem getCRSforString(String s) {
		if (s == null)
			throw new IllegalArgumentException("No CRS provided");
		
		try {
			return CRS.decode(s);
		} catch (FactoryException e1) {
			e1.printStackTrace();
			throw new IllegalArgumentException("unable to decode CRS from string: "+s);
		} catch (NullPointerException e2) {
			throw new IllegalArgumentException("This string does not contains any CRS: "+s);
		}
	}
	
	/**
	 * Takes a column of a sample supposed to be in WKT format, 
	 * and tries to detect using the 50 first lines 
	 * which geometry type it is: Point, Polyline, etc.
	 * 
	 * @param sample
	 * @param colNameGeom
	 * @return
	 */
	public static Class<?> detectGeometryClassFromData(
							BufferedDataTable sample,
							String colNameGeom) 
							throws IllegalArgumentException {
			
		final int SAMPLE = 50;
		
		final int idxColGeom = sample.getDataTableSpec().findColumnIndex(colNameGeom);

        GeometryFactory geomFactory = JTSFactoryFinder.getGeometryFactory( null );
        WKTReader reader = new WKTReader(geomFactory);
        
		List<Geometry> foundGeometries = new ArrayList<Geometry>(SAMPLE);
		
    	Iterator<DataRow> itRows = sample.iterator();
    	while (itRows.hasNext()) {
        	DataRow currentRow = itRows.next();
        	
        	DataCell cellGeom = currentRow.getCell(idxColGeom);
        	
        	if (cellGeom.isMissing()) {
        		continue;
        	}
        
        	Geometry g;
			try {
				g = reader.read(cellGeom.toString());
				foundGeometries.add(g);
			} catch (ParseException e) {
				e.printStackTrace();
				// ignore it
			}
        	
        	
        	if (foundGeometries.size() >= SAMPLE) {
        		break;
        	}
        	
    	}
    	
    	if (foundGeometries.isEmpty()) {
    		throw new IllegalArgumentException("no geometry found in column "+colNameGeom);
    	}
        
    	// check if all the geometries are the same
    	Class<?> classFirst = foundGeometries.get(0).getClass();
    	if (foundGeometries.stream().anyMatch( g -> !g.getClass().equals(classFirst))) {
    		throw new IllegalArgumentException("not all the geometry types are the same");
    	}
    	
    	return classFirst;
	}
	
	
	public static DataStore createDataStore() {
        return SpatialUtils.createTmpDataStore(true);
	}

	
	public static SimpleFeatureType createGeotoolsType(
				BufferedDataTable sample,
				String colNameGeom,
				String featureName,
				CoordinateReferenceSystem crs,
				boolean addIncrementalId
				)
				throws IllegalArgumentException {
		
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(featureName);
        builder.setCRS(crs); 
        
        Class<?> geomClassToBeStored = detectGeometryClassFromData(sample, colNameGeom);
        
        // add attributes in order
        builder.add(
        		SpatialUtils.GEOMETRY_COLUMN_NAME, 
        		geomClassToBeStored
        		);
        builder.add("rowid", String.class);
        
        if (addIncrementalId)
        	builder.add(ATTRIBUTE_NAME_INCREMENTAL_ID, Integer.class);
        
        // build the type
        final SimpleFeatureType type = builder.buildFeatureType();

        return type;
	}
	
	public static SimpleFeatureStore createFeatureStore(
			BufferedDataTable sample,
			DataStore datastore,
			SimpleFeatureType type,
			String featureName) throws IOException {
        
		
		try {
			datastore.getSchema(type.getName());
		} catch (IOException e) {
			datastore.createSchema(type);	
		}
		
        //System.out.println(datastore.getNames().get(0));
        
        // datastore.getSchema(featureName)
		SimpleFeatureSource featureSource = datastore.getFeatureSource(datastore.getNames().get(0));
        if (!(featureSource instanceof SimpleFeatureStore)) {
            throw new IllegalStateException("Modification not supported");
        }
        SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
        
        return featureStore;
        
	}

	public static class RowAndGeometry {
		
		public final Geometry geometry;
		public final DataRow row;
		
		public RowAndGeometry(Geometry geometry, DataRow row) {
			super();
			this.geometry = geometry;
			this.row = row;
		}
		
		
	}
	
	public interface IRowAndGeometryConsumer {
	    void accept(RowAndGeometry rowAndGeom) 
	    		throws CanceledExecutionException, InvalidSettingsException;
	}
	
	/**
	 * Decodes every cell of the geometry column of the sample, 
	 * and passes it to the consumer.
	 * 
	 * @param sample
	 * @param rowConsumer
	 * @throws CanceledExecutionException 
	 */
	public static void applyToEachGeometry(
						BufferedDataTable sample, 
						IRowAndGeometryConsumer geometryConsumer
						) throws CanceledExecutionException, InvalidSettingsException {

		GeometryFactory geomFactory = JTSFactoryFinder.getGeometryFactory( null );
		WKTReader reader = new WKTReader(geomFactory);
		
		final int idxColGeom = sample.getDataTableSpec().findColumnIndex(GEOMETRY_COLUMN_NAME);
		
		CloseableRowIterator itRow = sample.iterator();
		try {
	    	while (itRow.hasNext()) {
	    		final DataRow row = itRow.next();
	    		final DataCell cellGeom = row.getCell(idxColGeom);

            	if (cellGeom.isMissing()) {
            		//System.out.println("ignoring line "+currentRow.getKey()+" which has no geometry");
            		continue; // ignore data with missing elements
            	}
            	
            	// add geometry
            	try {
    				Geometry geom = reader.read(cellGeom.toString());
    				geometryConsumer.accept(
    						new RowAndGeometry(geom, row));

    			} catch (ParseException e) {
    				e.printStackTrace();
    				throw new IllegalArgumentException(
    						"Invalid WKT geometry on row "+
    						row.getKey()+":"+
    						e.getMessage(), 
    						e
    						);    			
    			}
            	
	    	}
		} finally {
			itRow.close();
		}
		
	}

	private static class AddRowsRunnable implements Runnable {

		private final BufferedDataTable sample; 
		
		final static int BUFFER = 50000;
		
		private List<SimpleFeature> toStore = new ArrayList<>(BUFFER);
		private final int idxColGeom; 
		private final WKTReader reader;
		private final SimpleFeatureBuilder featureBuilder;
		private final SimpleFeatureStore featureStore;
		private final SimpleFeatureType type;
		private final ExecutionMonitor execProgress;
		private final boolean addIncrementalId;
		
		public AddRowsRunnable(
				BufferedDataTable sample, 
				int idxColGeom,
				SimpleFeatureStore featureStore,
				SimpleFeatureType type,
				ExecutionMonitor execProgress,
				boolean addIncrementalId
				) {
			this.sample = sample;
			this.idxColGeom = idxColGeom;
			this.featureStore = featureStore;
			this.type = type;
			this.execProgress = execProgress;
	        this.featureBuilder = new SimpleFeatureBuilder(type);
	        this.addIncrementalId = addIncrementalId;
	        
			GeometryFactory geomFactory = JTSFactoryFinder.getGeometryFactory( null );
	        reader = new WKTReader(geomFactory);
		}
		
		@Override
		public void run() {
			
			toStore.clear();
			this.execProgress.setProgress(.0);
			
			double total = (double)this.sample.size();
			int current = 0;
			
			//System.out.println(Thread.currentThread().getName()+"  will store total "+this.sample.size());
			CloseableRowIterator itRow = sample.iterator();
			try {
				
				while (itRow.hasNext()) {
	        		DataRow currentRow = itRow.next();
	        		
	        		DataCell cellGeom = currentRow.getCell(idxColGeom);
	        		
	            	if (cellGeom.isMissing()) {
	            		System.out.println("ignoring line "+currentRow.getKey()+" which has no geometry");
	            		continue; // ignore data with missing elements
	            	}
	            	
	            	// add geometry
	            	try {
	    				Geometry geom = reader.read(cellGeom.toString());
	    				featureBuilder.add(geom);
	    			} catch (ParseException e) {
	    				e.printStackTrace();
	    				throw new IllegalArgumentException(
	    						"Invalid WKT geometry on row "+
	    						currentRow.getKey()+":"+
	    						e.getMessage(), 
	    						e);    			
	    			}
	            	
	            	// add row id
	            	final String rowid = currentRow.getKey().getString();
	            	
	                // add incrementalId
	            	SimpleFeature feature = null;
	                if (addIncrementalId) {
	                    feature = featureBuilder.buildFeature(
	                    		rowid, new Object[] { rowid, current }
	                    		);
	                } else {
	                    feature = featureBuilder.buildFeature(
	                    		rowid, new Object[] { rowid }
	                    		);
	                }
	                
	
	                toStore.add(feature);
	                
					if (toStore.size() >= BUFFER) {
		    			//System.out.println(Thread.currentThread().getName()+" Storing buffer "+toStore.size());
						storeBufferedSpatialData();
					}
	 
	        		if (current % 100 == 0) {
		        		this.execProgress.setProgress((double)current/total);
		        		try {
							this.execProgress.checkCanceled();
						} catch (CanceledExecutionException e) {
							return;
						} 
	        		}
	        		
	        		current++;
	
	        	}

	        	// store data remaining in the buffer
	        	storeBufferedSpatialData();
	        	
			} finally {
				itRow.close();
			}
			
    		this.execProgress.setProgress(1.0);

		}
		
		private void storeBufferedSpatialData() {
			
			if (toStore.isEmpty())
				return;
			
        	try {
				featureStore.addFeatures( new ListFeatureCollection( type, toStore) );
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("error when storing features in the store", e);
			}
        	toStore.clear();

		}
		
		
	}


	public static Runnable decodeAsFeaturesRunnable(
			BufferedDataTable sample,
			String colNameGeom,
			ExecutionMonitor execProgress,
			DataStore datastore,
			String featureName,
			CoordinateReferenceSystem crs,
			boolean addIncrementalId
			) throws IOException {
		
		SimpleFeatureType type = createGeotoolsType(sample, colNameGeom, featureName, crs, addIncrementalId);
		SimpleFeatureStore store = createFeatureStore(sample, datastore, type, featureName);

		final int idxColGeom = sample.getDataTableSpec().findColumnIndex(colNameGeom);

        return new AddRowsRunnable(sample, idxColGeom, store, type, execProgress, addIncrementalId);
        		
	}
		
	
	/**
	 * Create a temporary datastore
	 * @return
	 */
	public static DataStore createTmpDataStore(boolean createSpatialIndex) {
        File file;
		try {
			file = FileUtil.createTempFile("datastore", ".shp", true);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException("unable to create a geotools datastore", e1);

		}
		
		return createDataStore(file, createSpatialIndex);
	}
	
	
	public static DataStore createDataStore(File file, boolean createSpatialIndex) {
        
		Map<String,Serializable> map = new HashMap<>();
		try {
			map.put( "url", file.toURI().toURL() );
			map.put("create spatial index", Boolean.valueOf(createSpatialIndex));

		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			throw new RuntimeException("unable to create a geotools datastore", e1);

		}
		DataStore dataStore = null;
		try {
			dataStore = new ShapefileDataStoreFactory().createNewDataStore(map);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("unable to create a geotools datastore", e);
		}
		
		return dataStore;
	}
	
	
	/** 
	 * finds the entities of a datastore contained inside 
	 * a given geometry
	 * @param datastore1
	 * @param geom
	 * @return
	 */
	public static FeatureIterator<SimpleFeature> findEntitiesWithin(
			SimpleFeatureSource source, Geometry geom) {
		
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints() );
		Filter filter = ff.within(ff.property( BasicFeatureTypes.GEOMETRY_ATTRIBUTE_NAME), ff.literal( geom ));
		
		FeatureIterator<SimpleFeature> fItt;
		try {
			fItt = source.getFeatures(filter).features();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("error while loading entities",e);
		}
		return fItt;
	}
	
	public static FeatureIterator<SimpleFeature> findClosestNeighboorFixBuffer(
			Geometry geom,
			SimpleFeatureSource source,
			int buffer) {
		
		
		Geometry buffered = geom.buffer(buffer);
		
		return findEntitiesWithin(source, buffered);
		
	}

	public static SimpleFeature findClosestNeighboorVariableBuffer(
			Geometry geom,
			SimpleFeatureSource source,
			int maxBuffer) {
		
		List<Integer> distances = new LinkedList<Integer>();
		{
			distances.add(maxBuffer);
			int current = maxBuffer;
			while (current >= 100) {
				current = current-20;
				distances.add(0, current);
			}
			while (current >= 20) {
				current = current - 10;
				distances.add(0, current);
			}
				
		}
		
		double shortestDistance = Double.MAX_VALUE;
		List<SimpleFeature> closestPoints = new LinkedList<SimpleFeature>();

		for (Integer bufferDistance: distances) {

			System.out.println("searching around "+bufferDistance);
			FeatureIterator<SimpleFeature> itNeighboors = findClosestNeighboorFixBuffer(geom, source, bufferDistance);

			// compute distances
			while (itNeighboors.hasNext()) {
				SimpleFeature neighboor = itNeighboors.next();
				double distance = geom.distance((Geometry) neighboor.getAttribute(0));
				if (distance < shortestDistance) {
					shortestDistance = distance;
					closestPoints.clear();
					closestPoints.add(neighboor);
				} else if (distance == shortestDistance) {
					closestPoints.add(neighboor);
				}
			}
			
			itNeighboors.close();
			
			// if we found something, stop searching far
			if (!closestPoints.isEmpty()) {
				System.out.println("at distance "+bufferDistance+", found "+closestPoints.size()+" neighboors");
				break;
			}
			
		}
		
		// return the closest
		if (closestPoints.size() == 1) {
			return closestPoints.get(0);
		} else if (closestPoints.size() > 1) {
			// or one random
			Random random = new Random(); // TODO?!
			System.err.println("selecting one random neighboor among "+closestPoints.size());
			return closestPoints.get(random.nextInt(closestPoints.size()));
		}
		
		// return null if nothing found
		return null;
		
	}
	
	
	private SpatialUtils() {

	}

	public static boolean hasCRS(DataColumnSpec columnSpec) {
		return columnSpec.getProperties().getProperty(PROPERTY_CRS_CODE) != null;
	}
	
	public static CoordinateReferenceSystem decodeCRSFromColumnSpec(DataColumnSpec columnSpec) {

		try {
			return SpatialUtils.getCRSforString(columnSpec.getProperties().getProperty(PROPERTY_CRS_CODE));
		} catch (IllegalArgumentException e) {
			try {
				return CRS.parseWKT(columnSpec.getProperties().getProperty(PROPERTY_CRS_WKT));
			} catch (FactoryException e1) {
				e1.printStackTrace();
				throw new IllegalArgumentException(
						"Unable to decode a coordinate reference system "+
						"from the code \""+columnSpec.getProperties().getProperty(PROPERTY_CRS_CODE)+"\""+
						" nor from WKT "+columnSpec.getProperties().getProperty(PROPERTY_CRS_WKT)); 
			}
		}
	}
	
	public static CoordinateReferenceSystem decodeCRS(DataTableSpec spec) {

		int idx = spec.findColumnIndex(GEOMETRY_COLUMN_NAME);
		if (idx < 0) 
			throw new IllegalArgumentException("No column for containing geometry "+GEOMETRY_COLUMN_NAME);
		
		DataColumnSpec columnSpec = spec.getColumnSpec(idx);
		
		try {
			return SpatialUtils.getCRSforString(columnSpec.getProperties().getProperty(PROPERTY_CRS_CODE));
		} catch (IllegalArgumentException e) {
			try {
				return CRS.parseWKT(columnSpec.getProperties().getProperty(PROPERTY_CRS_WKT));
			} catch (FactoryException e1) {
				e1.printStackTrace();
				throw new IllegalArgumentException(
						"Unable to decode a coordinate reference system "+
						"from the code \""+columnSpec.getProperties().getProperty(PROPERTY_CRS_CODE)+"\""+
						" nor from WKT "+columnSpec.getProperties().getProperty(PROPERTY_CRS_WKT)); 
			}
		}
	}


	public static boolean hasCRS(DataTableSpec dataTableSpec) {
		int idx = dataTableSpec.findColumnIndex(GEOMETRY_COLUMN_NAME);
		if (idx < 0)
			return false;
		return dataTableSpec.getColumnSpec(idx).getProperties().getProperty(PROPERTY_CRS_WKT) != null;
	}

	public static boolean hasGeometry(DataTableSpec dataTableSpec) {
		int idx = dataTableSpec.findColumnIndex(GEOMETRY_COLUMN_NAME);
		return (idx >= 0);
	}
	
	 

}
