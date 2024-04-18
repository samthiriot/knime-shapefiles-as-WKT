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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.write.write_to_geotiff;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.media.jai.RasterFactory;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
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
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.FileUtil;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


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
public class WriteWKTToGeoTIFFNodeModel extends NodeModel {

	private static final NodeLogger logger = NodeLogger.getLogger(WriteWKTToGeoTIFFNodeModel.class);

    private final SettingsModelString m_file = new SettingsModelString("filename", null);

    private final SettingsModelString m_colX = new SettingsModelString("colx", null);
    private final SettingsModelString m_colY = new SettingsModelString("coly", null);

    private final SettingsModelString m_crs = new SettingsModelString("CRS", "EPSG:4326");


    private final SettingsModelDouble m_upperLeftLat = new SettingsModelDouble("upper left lat", 0.0);
    private final SettingsModelDouble m_upperLeftLon = new SettingsModelDouble("upper left lon", 0.0);
    
    private final SettingsModelDouble m_bottomRightLat = new SettingsModelDouble("bottom right lat", 0.0);
    private final SettingsModelDouble m_bottomRightLon = new SettingsModelDouble("bottom right lon", 0.0);
    
    private final SettingsModelString m_compression = new SettingsModelString("m_compression", "DEFLATE");

    protected static LinkedHashSet<String> COMPRESSION_ALGOS = new LinkedHashSet<String>(Arrays.asList(
    		"no compression", 
    		"LZW", "DEFLATE", "PACKBITS"
    		));
    
	/**
	 * Constructor for the node model.
	 */
	protected WriteWKTToGeoTIFFNodeModel() {
		
        super(1, 0);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		
		DataTableSpec specs = inSpecs[0];

		Set<String> intColumnsUnused = extractIntColumns(specs);
	
    	if (m_colX.getStringValue() != null)
    		intColumnsUnused.remove(m_colX.getStringValue());
    	if (m_colY.getStringValue() != null)
    		intColumnsUnused.remove(m_colY.getStringValue());
    	
    	// try to define default parameter value
    	Iterator<String> itColName = intColumnsUnused.iterator();
    	if (m_colY.getStringValue() == null && itColName.hasNext()) {
    		m_colY.setStringValue(itColName.next());
    		itColName.remove();
    	}
    	if (m_colX.getStringValue() == null && itColName.hasNext()) {
    		m_colX.setStringValue(itColName.next());
    		itColName.remove();
    	}
    	
    	// fail if columns are not defined
    	if (m_colX.getStringValue() == null || m_colY.getStringValue() == null)
    		throw new InvalidSettingsException("please select columns for coordinates");
    		
    	// fail if columns are unknown
    	if (specs.getColumnSpec(m_colX.getStringValue()) == null)
    		throw new InvalidSettingsException("no column "+m_colX.getStringValue());
    	if (specs.getColumnSpec(m_colY.getStringValue()) == null)
    		throw new InvalidSettingsException("no column "+m_colY.getStringValue());
    	
    	// ... or the same!
    	if (m_colX.getStringValue().equals(m_colY.getStringValue()))
    		throw new InvalidSettingsException("please select differrent columns for X and Y");

    	// fail if no file
    	if (m_file.getStringValue() == null)
    		throw new IllegalArgumentException("No filename was provided");

    	// check the parameters include a filename
		try {
			FileUtil.toURL(m_file.getStringValue());
		} catch (InvalidPathException | MalformedURLException e2) {
			e2.printStackTrace();
			throw new InvalidSettingsException("unable to open URL "+m_file.getStringValue()+": "+e2.getMessage());
		}
    	
		// CRS
    	try {
			CRS.decode(m_crs.getStringValue()); 
		} catch (FactoryException e) {
			throw new InvalidSettingsException("unable to find the Coordinate Reference System "+m_crs.getStringValue());
		}      
		
    	// compression
    	if (!COMPRESSION_ALGOS.contains(m_compression.getStringValue()))
			throw new InvalidSettingsException("unknown compression algorithm "+m_compression.getStringValue());

		// fail if no data 
		Set<String> numericColumnsUnused = extractNumericColumns(specs);
		numericColumnsUnused.remove(m_colX.getStringValue());
		numericColumnsUnused.remove(m_colY.getStringValue());
		
		if (numericColumnsUnused.isEmpty())
			throw new InvalidSettingsException("there in no data column");
		
        return new DataTableSpec[]{};
        
	}


	private Set<String> extractNumericColumns(DataTableSpec specs) {
		Set<String> intColumnsUnused = new LinkedHashSet<String>();
		Iterator<DataColumnSpec> itCol = specs.iterator();
		while (itCol.hasNext()) {
			DataColumnSpec colSpec = itCol.next();
			if (colSpec.getType().isCompatible(DoubleValue.class))
				intColumnsUnused.add(colSpec.getName());
		}
		return intColumnsUnused;
	}
	
	private Set<String> extractIntColumns(DataTableSpec specs) {
		Set<String> intColumnsUnused = new LinkedHashSet<String>();
		Iterator<DataColumnSpec> itCol = specs.iterator();
		while (itCol.hasNext()) {
			DataColumnSpec colSpec = itCol.next();
			if (colSpec.getType().isCompatible(IntValue.class))
				intColumnsUnused.add(colSpec.getName());
		}
		return intColumnsUnused;
	}
	
	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		final BufferedDataTable inputPopulation = inData[0];
    	
		// prepare the file
    	URL url;
		try {
			url = FileUtil.toURL(m_file.getStringValue());
		} catch (InvalidPathException | MalformedURLException e2) {
			e2.printStackTrace();
			throw new InvalidSettingsException("unable to open URL "+m_file.getStringValue()+": "+e2.getMessage());
		}
        
    	File file = FileUtil.getFileFromURL(url);
   
    	// read the CRS from parameters
    	CoordinateReferenceSystem crs = null; 
		try {
			crs = CRS.decode(m_crs.getStringValue()); 
		} catch (FactoryException e) {
			throw new RuntimeException("unable to find the Coordinate Reference System "+m_crs.getStringValue()+". This error should not happen. Please report this bug for solving.");
		}      
		
    	// detect colums
    	final int idxColX = inputPopulation.getDataTableSpec().findColumnIndex(m_colX.getStringValue());
    	final int idxColY = inputPopulation.getDataTableSpec().findColumnIndex(m_colY.getStringValue());
		Set<String> numericColumnsUnused = extractNumericColumns(inputPopulation.getDataTableSpec());
		numericColumnsUnused.remove(m_colX.getStringValue());
		numericColumnsUnused.remove(m_colY.getStringValue());
		
    	// what are we going to extract?
    	int[] bandsColIdx = new int[numericColumnsUnused.size()];
    	Iterator<String> itCol = numericColumnsUnused.iterator();
    	int idx = 0;
    	int type = DataBuffer.TYPE_INT;
    	while (itCol.hasNext()) {
    		String colName = itCol.next();
    		bandsColIdx[idx++] = inputPopulation.getDataTableSpec().findColumnIndex(colName);
    		
    		// if any column is not int, then store everything as double
    		if (!inputPopulation.getDataTableSpec().getColumnSpec(colName).getType().isCompatible(IntValue.class))
    			type = DataBuffer.TYPE_DOUBLE;
    	}
    	logger.info("will create "+numericColumnsUnused.size()+" "+(type == DataBuffer.TYPE_INT ? "integer" : "double")+" band"+(numericColumnsUnused.size()>1?"s":"")+" named "+String.join(", ", numericColumnsUnused));
    	
    	// detect the envelope
    	exec.setMessage("detecting the grid size");
    	int minX = Integer.MAX_VALUE;
    	int minY = Integer.MAX_VALUE;
    	int maxX = Integer.MIN_VALUE;
    	int maxY = Integer.MIN_VALUE;

    	// detect min-max values
    	double maxValue = - Double.MAX_VALUE;
    	double minValue = Double.MAX_VALUE;
    	boolean containsNull = false;
    	
    	int currentRow = 0;
    	CloseableRowIterator itRow = inputPopulation.iterator();
    	while (itRow.hasNext()) {
			DataRow row = itRow.next();
			
			Integer x = ((IntValue)row.getCell(idxColX)).getIntValue();
			if (x < minX)
				minX = x;
			if (x > maxX)
				maxX = x;
			
			Integer y = ((IntValue)row.getCell(idxColY)).getIntValue();
			if (y < minY)
				minY = y;
			if (y > maxY)
				maxY = y;
			
			for (int b=0; b<bandsColIdx.length; b++) {
				try {
					double cellValue = ((DoubleValue)row.getCell(bandsColIdx[b])).getDoubleValue();   
					if (cellValue > maxValue)
						maxValue = cellValue;
					if (cellValue < minValue)
						minValue = cellValue;
				} catch(ClassCastException e) {
					containsNull = true;
				}
			}
			
			if (currentRow % 100 == 0) {
				exec.setProgress(0.3*(double)currentRow / inputPopulation.size(), "detecting the grid size");
				exec.checkCanceled();
			}
			currentRow++;
		}
    	itRow.close();
		exec.setProgress(0.3, "detecting the grid size");
		exec.checkCanceled();

    	final int width = maxX - minX + 1;
    	final int height = maxY - minY + 1;
    	final long pixels = width * height;
    	logger.info("the grid size is ("+minX+","+minY+") ("+maxX+","+maxY+"), that is an image of "+width+"x"+height+" pixels");

    	// controls!
    	if (inputPopulation.size() < pixels) {
    		setWarningMessage("There are less rows that pixels. The image will be only partly defined");
    		containsNull = true;
    	} else if (inputPopulation.size() > pixels)
    		throw new RuntimeException("There are more rows that pixels. Please ensure you selected the right columns for the coordinates.");
    	
		// adapt the precise data type according to max values.
    	// the goal is to minimize the space used for storage
    	int typePrecise = type;
    	double maxAbsValue = Math.max( Math.abs(minValue), Math.abs(maxValue) );
    	Number missingValue = null;
    	
    	final String strMinMax = " (min: "+minValue+", max:"+maxValue+")";
    	
		switch (type) {
		case DataBuffer.TYPE_INT:
			if ( 
				(!containsNull && minValue >= 0 && maxValue <= 255) ||
				(containsNull && (
						(minValue > 0 && maxValue <= 255) ||
						(minValue >= 0 && maxValue < 255))
						)
				) { // should be able to encode the data, or data + mask in case a mask is needed
				typePrecise = DataBuffer.TYPE_BYTE;
				logger.info("data will be stored as byte" + strMinMax);
				if (containsNull) {
					// we need to identify a value to use as a mask, that is not used as a value
					if (minValue > 0)
						// use the lowest encodable value
						missingValue = 0;
					else if (maxValue < 255)
						missingValue = 255;
				}
				
			} else if ( 
					(!containsNull && minValue >= 0 && maxValue <= Short.MAX_VALUE * 2) ||
					(containsNull && (
							(minValue > 0 && maxValue <= Short.MAX_VALUE * 2)
							|| (minValue >= 0 && maxValue < Short.MAX_VALUE * 2)
							))
					) {
				typePrecise = DataBuffer.TYPE_USHORT;	
				logger.info("data will be stored as unsigned short" + strMinMax);
				if (minValue > 0)
					missingValue = 0;
				else if (maxValue < Short.MAX_VALUE*2)
					missingValue = Short.MAX_VALUE*2;
			} else if ( 
					(!containsNull && minValue >= Short.MIN_VALUE && maxValue <= Short.MAX_VALUE) ||
					(containsNull && (
							(minValue > Short.MIN_VALUE && maxValue <= Short.MAX_VALUE)
							|| (minValue >= Short.MIN_VALUE && maxValue < Short.MAX_VALUE)
							))
					) {
				typePrecise = DataBuffer.TYPE_SHORT;
				logger.info("data will be stored as short" + strMinMax);
				if (minValue > Short.MIN_VALUE)
					missingValue = Short.MIN_VALUE;
				else if (maxValue < Short.MAX_VALUE)
					missingValue = Short.MAX_VALUE;
				
			} else  {
				typePrecise = DataBuffer.TYPE_INT;
				logger.info("data will be stored as integer" + strMinMax);
				if (minValue > Integer.MIN_VALUE)
					missingValue = Integer.MIN_VALUE;
				else if (maxValue < Integer.MAX_VALUE)
					missingValue = Integer.MAX_VALUE;
			} 
			break;
		case DataBuffer.TYPE_DOUBLE:
			if ( 
					(!containsNull && maxAbsValue <= Float.MAX_VALUE) ||
					(containsNull && maxAbsValue < Float.MAX_VALUE)
					) {
				typePrecise = DataBuffer.TYPE_FLOAT;
				logger.info("data will be stored as float" + strMinMax);
				
				if (minValue * 0.9999 > -Float.MAX_VALUE)
					missingValue = minValue * 0.9999;
				else if (maxValue * 1.001 < Float.MAX_VALUE)
					missingValue = maxValue * 1.001;
			} 
			else  {
				typePrecise = DataBuffer.TYPE_DOUBLE;				
				logger.info("data will be stored as double" + strMinMax);
				if (minValue * 0.9999 > -Double.MAX_VALUE)
					missingValue = minValue * 0.9999;
				else if (maxValue * 1.001 < Double.MAX_VALUE)
					missingValue = maxValue * 1.001;
			} 
			break;
		default:
			throw new RuntimeException("unsupported data type");
		}
	
		// define what value we might use as missing data
		if (containsNull) {
			if (missingValue == null) 
				throw new RuntimeException("unable to find a value for the mask");
			logger.info("there might be missing data in the file, proposing as masked value: "+missingValue);
		}
		
    	// first create the image from data
    	exec.setProgress(0.3, "creation of the grid data");

    	// prepare the future data
        WritableRaster raster = null;
        try {
        	raster = RasterFactory.createBandedRaster(
        		typePrecise,
        		width, height,
        		bandsColIdx.length, null);
        } catch (RuntimeException e) {
        	throw new RuntimeException("error when initializing a banded raster", e);
        }
        
        // set the default values (if necessary)
        try {
	        if (inputPopulation.size() < pixels) {
	        	exec.setMessage("defining default value "+missingValue);
	        	fillRasterWithDefault(exec, minX, minY, maxX, maxY, bandsColIdx, type, raster, missingValue);
	        }
	        exec.checkCanceled();
        } catch (RuntimeException e) {
        	throw new RuntimeException("error when initializing default values", e);
        }
        
        // read the data        
        exec.setProgress(0.4, "reading pixels");
    	try {
	        itRow = inputPopulation.iterator();
	    	while (itRow.hasNext()) {
				DataRow row = itRow.next();
				final int x = ((IntValue)row.getCell(idxColX)).getIntValue();
				final int y = ((IntValue)row.getCell(idxColY)).getIntValue();
				
				switch (type) {
				case DataBuffer.TYPE_INT:
					for (int b=0; b<bandsColIdx.length; b++) {
						try {
							raster.setSample(x, y, b, ((IntValue)row.getCell(bandsColIdx[b])).getIntValue());
						} catch(ClassCastException e){
							// unable to decode this, will store missing value
							raster.setSample(x, y, b, missingValue.intValue());
						}
					}
					break;
				case DataBuffer.TYPE_DOUBLE:
					for (int b=0; b<bandsColIdx.length; b++) {
						try {
							raster.setSample(x, y, b, ((DoubleValue)row.getCell(bandsColIdx[b])).getDoubleValue());
						} catch(ClassCastException e){
							// unable to decode this, will store missing value
							raster.setSample(x, y, b, missingValue.doubleValue());
						}
					}
					break;
				default:
					throw new RuntimeException("unsupported data type");
				}
			
				if (currentRow % 100 == 0) {
					exec.setProgress(0.4 + 0.4*(double)currentRow / inputPopulation.size(), "reading pixels");
					exec.checkCanceled();
				}
				currentRow++;
	        }
	    	itRow.close();
    	} catch (RuntimeException e) {
        	throw new RuntimeException("error when reading pixels", e);
    	}
		exec.setProgress(0.8, "reading pixels");
		exec.checkCanceled();

    	exec.setProgress(0.8, "converting to raster");
    	ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(
    			m_upperLeftLon.getDoubleValue(), 
    			m_bottomRightLon.getDoubleValue(), 
    			m_upperLeftLat.getDoubleValue(), 
    			m_bottomRightLat.getDoubleValue(), 
    			crs);
    	
    	GridCoverageFactory gcf = CoverageFactoryFinder.getGridCoverageFactory(null);
    	
    	GridSampleDimension[] dimensions = new GridSampleDimension[bandsColIdx.length];
		for (int b=0; b<bandsColIdx.length; b++) {
			dimensions[b] = new GridSampleDimension(
					inputPopulation.getDataTableSpec().getColumnNames()[bandsColIdx[b]]					
					);
		}
		
        Map<String,Object> properties = new HashMap<String, Object>();
        if (missingValue != null) {
	        CoverageUtilities.setNoDataProperty(properties, missingValue);
			properties.put(CoverageUtilities.NODATA.toString(), missingValue);
        }
        
        // first create the image
		GridCoverage2D gc = null;
		try {
			gc = gcf.create(
    			UUID.randomUUID().toString(),
    			raster,
    			referencedEnvelope
    			);
		} catch (RuntimeException e) {
        	throw new RuntimeException("error when creating the raster", e);
		}
		
		// then render it with the missing value
		RenderedImage img = null;
		GridCoverage2D gc2 = null;
		try {
			img = gc.getRenderedImage();
			gc2 = gcf.create(
					UUID.randomUUID().toString(),
					img,
					referencedEnvelope,
					dimensions,
					null,
					properties
					);
		} catch (RuntimeException e) {
        	throw new RuntimeException("error when creating the raster with properties", e);
		} finally {
			gc.dispose(true);
		}
		
		exec.checkCanceled();

    	// write it into a ffile
    	exec.setProgress(0.9, "writing into a GeoTIFF file");
    	final GeoTiffWriteParams wp = new GeoTiffWriteParams();

    	// compression
    	// see https://github.com/geoserver/geoserver/blob/main/src/wcs/src/main/java/org/geoserver/wcs/responses/GeoTIFFCoverageResponseDelegate.java#L267
    	{
    		final String algo = m_compression.getStringValue();
    		if ("no compression".equals(algo)) {
    			// nothing to do
    		} else if ("LZW".equals(algo)) {
    			// nothing to do
    	        wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
    	        wp.setCompressionType("LZW");
    	        wp.setCompressionQuality(0.75F);
    		} else if ("DEFLATE".equals(algo)) {
    			// nothing to do
    	        wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
    	        wp.setCompressionType("Deflate");
    		} else if ("PACKBITS".equals(algo)) {
    			// nothing to do
    	        wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
    	        wp.setCompressionType("PackBits");
    		} else {
    			throw new RuntimeException("Unknown compression algorithm "+algo);
    		}
    	}
        
        final GeoTiffFormat format = new GeoTiffFormat();
        final ParameterValueGroup params = format.getWriteParameters();
        params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);

        if (file.exists())
        	file.delete();
        
        GridCoverageWriter writer = format.getWriter(file);
        try {
        	writer.write(gc2,
        		(GeneralParameterValue[]) params.values().toArray(new GeneralParameterValue[1])
        		);
        } catch (RuntimeException e) {
        	throw new RuntimeException("error when writing the raster into a file", e);
        } finally {
        	writer.dispose();
        	gc.dispose(true);
        	gc2.dispose(true);
        }
        
    	exec.setProgress(1.0, "done");
    	
        return new BufferedDataTable[]{};
        
	}

	private void fillRasterWithDefault(final ExecutionContext exec, int minX, int minY, int maxX, int maxY,
			int[] bandsColIdx, int type, final WritableRaster raster, Number value) throws CanceledExecutionException {
		
		switch (type) {
		
		case DataBuffer.TYPE_INT:
			int valueI = value.intValue();
			logger.info("filling missing data with value "+valueI);
			for (int b=0; b<bandsColIdx.length; b++) {
				for (int x=minX; x<maxX; x++) {
		    		for (int y=minY; y<maxY; y++) {
		    			raster.setSample(x, y, b, valueI);
		    		}
		    	}    
	    		exec.checkCanceled();
	    		// TODO progress		
			}
			break;
		case DataBuffer.TYPE_DOUBLE:
			double valueD = value.doubleValue();
			logger.info("filling missing data with value "+valueD);
			for (int b=0; b<bandsColIdx.length; b++) {
				for (int x=minX; x<maxX; x++) {
		    		for (int y=minY; y<maxY; y++) {
		    			raster.setSample(x, y, b, valueD);
		    		}
		    	}    
	    		exec.checkCanceled();
	    		// TODO progress		
			}
			break;
		default:
			throw new RuntimeException("unsupported data type " + type);
		}
		
	
	}


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        
    	m_file.saveSettingsTo(settings);
    	
    	m_colX.saveSettingsTo(settings);
    	m_colY.saveSettingsTo(settings);
    	
    	m_crs.saveSettingsTo(settings);
    	
    	m_upperLeftLat.saveSettingsTo(settings);
    	m_upperLeftLon.saveSettingsTo(settings);
    	m_bottomRightLat.saveSettingsTo(settings);
    	m_bottomRightLon.saveSettingsTo(settings);
    	
    	m_compression.saveSettingsTo(settings);
    	
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        
    	m_file.loadSettingsFrom(settings);

    	m_colX.loadSettingsFrom(settings);
    	m_colY.loadSettingsFrom(settings);
    	
    	m_crs.loadSettingsFrom(settings);
    	
    	m_upperLeftLat.loadSettingsFrom(settings);
    	m_upperLeftLon.loadSettingsFrom(settings);
    	m_bottomRightLat.loadSettingsFrom(settings);
    	m_bottomRightLon.loadSettingsFrom(settings);
    	
    	m_compression.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_file.validateSettings(settings);
    	
    	m_colX.validateSettings(settings);
    	m_colY.validateSettings(settings);
    	
    	m_crs.validateSettings(settings);
    	
    	m_upperLeftLat.validateSettings(settings);
    	m_upperLeftLon.validateSettings(settings);
    	m_bottomRightLat.validateSettings(settings);
    	m_bottomRightLon.validateSettings(settings);
    	
    	m_compression.validateSettings(settings);

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
