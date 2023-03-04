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
package ch.res_ear.samthiriot.knime.shapefilesaswkt;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
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
import org.geotools.data.shapefile.ShapefileDataStore;
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
import org.knime.core.data.property.ColorAttr;
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

import ch.res_ear.samthiriot.knime.shapefilesaswkt.preferences.PreferenceConstants;

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
		
		if (s == null || s.equalsIgnoreCase("null"))
			throw new IllegalArgumentException("No CRS provided");
			
		try {
			return CRS.decode(s);
		} catch (FactoryException e1) {
			e1.printStackTrace();
			throw new IllegalArgumentException("unable to decode CRS from string: "+s);
		} catch (NullPointerException e2) {
			throw new IllegalArgumentException("This string does not contains any CRS: "+s);
		} catch (RuntimeException e3) {
			e3.printStackTrace();
			throw new IllegalArgumentException("Error when decoding CRS from string "+s+": "+e3.getMessage(), e3);
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
        return SpatialUtils.createTmpDataStore(true, Charset.defaultCharset().name());
	}

	
	public static SimpleFeatureType createGeotoolsType(
				BufferedDataTable sample,
				String colNameGeom,
				String featureName,
				CoordinateReferenceSystem crs,
				boolean addIncrementalId,
				boolean addColor
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
        
        if (addColor)
        	builder.add("color", String.class);

        // TODO add color?
        //sample.getDataTableSpec().
        //sample.getDataTableSpec().getRowColor(row)
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

	public static class RowsAndGeometrys {
		
		public final Geometry geometry1;
		public final DataRow row1;
		public final Geometry geometry2;
		public final DataRow row2;
		
		public RowsAndGeometrys(Geometry geometry1, DataRow row1, Geometry geometry2, DataRow row2) {
			super();
			this.geometry1 = geometry1;
			this.row1 = row1;
			this.geometry2 = geometry2;
			this.row2 = row2;
		}
		
	}
	
	public interface IRowsAndGeometrysConsumer {
	    void accept(RowsAndGeometrys rowsAndGeoms) 
	    		throws CanceledExecutionException, InvalidSettingsException;
	}
	

	/**
	 * Decodes every cell of the geometry column of the two samples, 
	 * and passes them to the consumer.
	 * 
	 * @param sample
	 * @param rowConsumer
	 * @throws CanceledExecutionException 
	 */
	public static void applyToEachGeometry(
						BufferedDataTable sample1, 
						BufferedDataTable sample2, 
						IRowsAndGeometrysConsumer geometriesConsumer
						) throws CanceledExecutionException, InvalidSettingsException {

		if (sample1.size() != sample2.size())
			throw new InvalidSettingsException("the two input tables should have the same size");
		
		GeometryFactory geomFactory = JTSFactoryFinder.getGeometryFactory( null );
		WKTReader reader = new WKTReader(geomFactory);
		
		final int idxColGeom1 = sample1.getDataTableSpec().findColumnIndex(GEOMETRY_COLUMN_NAME);
		final int idxColGeom2 = sample2.getDataTableSpec().findColumnIndex(GEOMETRY_COLUMN_NAME);

		CloseableRowIterator itRow1 = sample1.iterator();
		CloseableRowIterator itRow2 = sample2.iterator();

		try {
	    	while (itRow1.hasNext()) {
	    		
	    		if (!itRow2.hasNext())
	    			throw new RuntimeException("there are no more as many entities in the two tables o_O");
	    		
	    		final DataRow row1 = itRow1.next();
	    		final DataCell cellGeom1 = row1.getCell(idxColGeom1);

	    		final DataRow row2 = itRow2.next();
	    		final DataCell cellGeom2 = row2.getCell(idxColGeom2);

	    		if (!(cellGeom1.isMissing() || cellGeom2.isMissing())) {
            		
                	// add geometry
            		Geometry geom1 = null;
                	try {
        				geom1 = reader.read(cellGeom1.toString());
        			} catch (ParseException e) {
        				e.printStackTrace();
        				throw new IllegalArgumentException(
        						"Invalid WKT geometry on row "+
        						row1.getKey()+":"+
        						e.getMessage(), 
        						e
        						);    			
        			}
                	
    				Geometry geom2 = null;
    				try {
        				geom2 = reader.read(cellGeom2.toString());
        			} catch (ParseException e) {
        				e.printStackTrace();
        				throw new IllegalArgumentException(
        						"Invalid WKT geometry on row "+
        						row2.getKey()+":"+
        						e.getMessage(), 
        						e
        					);
        			}
    				geometriesConsumer.accept(new RowsAndGeometrys(geom1, row1, geom2, row2));

            	}
            	

	    	}
		} finally {
			itRow1.close();
			itRow2.close();
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
		private final Color defaultColor;
		
		public AddRowsRunnable(
				BufferedDataTable sample, 
				int idxColGeom,
				SimpleFeatureStore featureStore,
				SimpleFeatureType type,
				ExecutionMonitor execProgress,
				boolean addIncrementalId, 
				Color defaultColor
				) {
			this.sample = sample;
			this.idxColGeom = idxColGeom;
			this.featureStore = featureStore;
			this.type = type;
			this.execProgress = execProgress;
	        this.featureBuilder = new SimpleFeatureBuilder(type);
	        this.addIncrementalId = addIncrementalId;
	        this.defaultColor = defaultColor;
	        
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
	        		
	        		LinkedList<Object> cells = new LinkedList<>();
	        		
	        		DataCell cellGeom = currentRow.getCell(idxColGeom);
	        		
	            	if (cellGeom.isMissing()) {
	            		//System.out.println("ignoring line "+currentRow.getKey()+" which has no geometry");
	            		// TODO add an option in order to keep them?
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
	            	cells.add(rowid);
	            	
	                // add incrementalId
	            	SimpleFeature feature = null;
	                if (addIncrementalId) {
	                	cells.add(current);
	                }
	                
	                // color?
	                if (defaultColor != null) {
	    				ColorAttr colorAttr = sample.getDataTableSpec().getRowColor(currentRow);
	            		boolean hasColor = !colorAttr.equals(ColorAttr.DEFAULT);
		        		if (hasColor) {
		        			Color rowColor = sample.getDataTableSpec().getRowColor(currentRow).getColor();
		        			cells.add("#"+Integer.toHexString(rowColor.getRGB()).substring(2));
		        		} else {
		        			cells.add(null);
		        			//cells.add("#"+Integer.toHexString(defaultColor.getRGB()).substring(2));
		        		}
	                }
	        			  
	        		feature = featureBuilder.buildFeature(
                    		rowid, cells.toArray(new Object[cells.size()])
                    		);
	
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

	/**
	 * Returns a Runnable which decodes a KNIME data table 
	 * and stores it as GeoTools features. 
	 * Adds a string attribute for color.
	 */
	public static Runnable decodeAsFeaturesRunnable(
			BufferedDataTable sample,
			String colNameGeom,
			ExecutionMonitor execProgress,
			DataStore datastore,
			String featureName,
			CoordinateReferenceSystem crs,
			boolean addIncrementalId,
			Color defaultColor
			) throws IOException {
		
		SimpleFeatureType type = createGeotoolsType(sample, colNameGeom, featureName, crs, addIncrementalId, defaultColor != null);
		SimpleFeatureStore store = createFeatureStore(sample, datastore, type, featureName);

		final int idxColGeom = sample.getDataTableSpec().findColumnIndex(colNameGeom);

        return new AddRowsRunnable(sample, idxColGeom, store, type, execProgress, addIncrementalId, defaultColor);
        		
	}
	
	/**
	 * Decodes the data in the sample data table as geotools features,
	 * in the provided datastore. 
	 * 
	 * @param sample
	 * @param colNameGeom
	 * @param execProgress
	 * @param datastore
	 * @param featureName
	 * @param crs
	 * @throws IOException
	 */
	public static void decodeAsFeatures(
			BufferedDataTable sample,
			String colNameGeom,
			ExecutionMonitor execProgress,
			DataStore datastore,
			String featureName,
			CoordinateReferenceSystem crs
			) throws IOException {

		SimpleFeatureType type = createGeotoolsType(sample, colNameGeom, featureName, crs, false, false);
		SimpleFeatureStore store = createFeatureStore(sample, datastore, type, featureName);

		final int idxColGeom = sample.getDataTableSpec().findColumnIndex(colNameGeom);

		AddRowsRunnable runnable = new AddRowsRunnable(sample, idxColGeom, store, type, execProgress, false, null);

		runnable.run();
			
	}

	/**
	 * Create a temporary datastore
	 * @return
	 */
	public static DataStore createTmpDataStore(boolean createSpatialIndex, String charset) {
        File file;
		try {
			file = FileUtil.createTempFile("datastore", ".shp", true);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException("unable to create a geotools datastore", e1);

		}
		return createDataStore(file, createSpatialIndex, charset);
	}
	
	public static DataStore createTmpDataStore(boolean createSpatialIndex) {
		return createTmpDataStore(createSpatialIndex, Charset.defaultCharset().name());
	}
	
	public static DataStore createDataStore(File file, boolean createSpatialIndex, String charset) {
        
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
		
		// set the charset
		try {
			((ShapefileDataStore)dataStore).setCharset(Charset.forName(charset));
		} catch (ClassCastException e) {
			throw new RuntimeException("unable to define charset for this datastore");
		}
		
		return dataStore;
	}
	
	public static DataStore createDataStore(File file, boolean createSpatialIndex) {
		return createDataStore(file, createSpatialIndex, Charset.defaultCharset().name());
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

			//System.out.println("searching around "+bufferDistance);
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
				//System.out.println("at distance "+bufferDistance+", found "+closestPoints.size()+" neighboors");
				break;
			}
			
		}
		
		// return the closest
		if (closestPoints.size() == 1) {
			return closestPoints.get(0);
		} else if (closestPoints.size() > 1) {
			// or one random
			Random random = new Random(); // TODO?!
			//System.err.println("selecting one random neighboor among "+closestPoints.size());
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
	
	public static File getFileForCache() {
		String filepath = ShapefileAsWKTNodePlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_DIRECTORY_CACHE);
		File f = new File(filepath);
		f.mkdirs();
		return f;
	}
	

}
