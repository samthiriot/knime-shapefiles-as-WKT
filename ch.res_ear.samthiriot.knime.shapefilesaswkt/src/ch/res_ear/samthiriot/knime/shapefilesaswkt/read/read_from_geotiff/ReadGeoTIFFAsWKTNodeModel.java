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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_geotiff;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RectIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Envelope2D;
import org.geotools.util.factory.Hints;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;
import it.geosolutions.jaiext.iterators.RectIterFactory;


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
public class ReadGeoTIFFAsWKTNodeModel extends NodeModel {
    
    /**
	 * The logger is used to print info/warning/error messages to the KNIME console
	 * and to the KNIME log file. Retrieve it via 'NodeLogger.getLogger' providing
	 * the class of this node model.
	 */
	private static final NodeLogger logger = NodeLogger.getLogger(ReadGeoTIFFAsWKTNodeModel.class);


    private final SettingsModelString m_file = new SettingsModelString("filename", null);

    private final SettingsModelBoolean m_createColumnId = new SettingsModelBoolean("create col id", true);
    private final SettingsModelBoolean m_createColumnCoords = new SettingsModelBoolean("create col coords", true);
    private final SettingsModelBoolean m_createColumnGeom = new SettingsModelBoolean("create col geom", true);


	/**
	 * Constructor for the node model.
	 */
	protected ReadGeoTIFFAsWKTNodeModel() {
        super(0, 1);
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
    	
    	// TODO try to open the file
    	
        return new DataTableSpec[]{ null }; // createDataTableSpec(decodeFileFromKML())
    }



	private DataColumnSpec[] createSpecs(GridCoverage2D coverage, RenderedImage raster, CoordinateReferenceSystem crs) {

    	List<DataColumnSpec> specs = new LinkedList<DataColumnSpec>();
    	
    	if (m_createColumnId.getBooleanValue())
	    	specs.add(new DataColumnSpecCreator(
	    			"id", 
	    			LongCell.TYPE
	    			).createSpec());
    	
    	if (m_createColumnCoords.getBooleanValue()) {
	    	specs.add(new DataColumnSpecCreator(
	    			"line", 
	    			IntCell.TYPE
	    			).createSpec());
	    	specs.add(new DataColumnSpecCreator(
	    			"column", 
	    			IntCell.TYPE
	    			).createSpec());
    	}
    	
        final int numBands = raster.getSampleModel().getNumBands();
        final int dataType = raster.getSampleModel().getDataType();
        
        //if (numBands != names.length)
        //	throw new RuntimeException("wrong count of names");
        	
	    switch (dataType) {
	    case DataBuffer.TYPE_BYTE:
	    case DataBuffer.TYPE_INT:
	    case DataBuffer.TYPE_SHORT:
	    case DataBuffer.TYPE_USHORT:
	      	for (int i = 0; i < numBands; i++) {
	        	specs.add(new DataColumnSpecCreator(
	        	        coverage.getSampleDimension(i).getDescription().toString(),
	        			IntCell.TYPE
	        			).createSpec());
	        }
	      	break;
	    case DataBuffer.TYPE_DOUBLE:
	    case DataBuffer.TYPE_FLOAT:
	    	for (int i = 0; i < numBands; i++) {
	        	specs.add(new DataColumnSpecCreator(
	        			coverage.getSampleDimension(i).getDescription().toString(),
	        			DoubleCell.TYPE
	        			).createSpec());
	        	}
	    	break;
	    default:
	    	break;
	    }

	    // geometry column 
	    if (m_createColumnGeom.getBooleanValue()) {
	    	DataColumnSpecCreator creatorGeom = new DataColumnSpecCreator(
	    			SpatialUtils.GEOMETRY_COLUMN_NAME, 
	    			StringCell.TYPE
	    			);
			Map<String,String> properties = new HashMap<String, String>();
			properties.put(SpatialUtils.PROPERTY_CRS_CODE, SpatialUtils.getStringForCRS(crs));
			properties.put(SpatialUtils.PROPERTY_CRS_WKT, crs.toWKT());
			DataColumnProperties propertiesKWT = new DataColumnProperties(properties);
			creatorGeom.setProperties(propertiesKWT);
			
        	specs.add(creatorGeom.createSpec());
        
	    }
	    	
	    
    	return specs.toArray(new DataColumnSpec[specs.size()]);
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	
    	// see https://gis.stackexchange.com/questions/106882/reading-each-pixel-of-each-band-of-multiband-geotiff-with-geotools-java

    	
    	final boolean createId = m_createColumnId.getBooleanValue();
    	final boolean createCoords = m_createColumnCoords.getBooleanValue();
    	final boolean createGeom = m_createColumnGeom.getBooleanValue();

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
       
        exec.setMessage("Opening the file");

        // open the file content
        InputStream inputStream;
		try {
			inputStream = FileUtil.openStreamWithTimeout(filename);
		} catch (IOException e2) {
			e2.printStackTrace();
			throw new IllegalArgumentException("unable to open the URL "+filename+": "+e2.getMessage());
		}

		final Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        GeoTiffReader reader = new GeoTiffReader(inputStream, hints);
        
        GridCoverage2D coverage = reader.read(null);
		CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
        
        final RenderedImage raster = coverage.getRenderedImage();
        final int numBands = raster.getSampleModel().getNumBands();
        final int dataType = raster.getSampleModel().getDataType();
        
        final double[] valuesD = new double[numBands];
        final int[] valuesI = new int[numBands];

        PlanarImage planarImg = PlanarImage.wrapRenderedImage(raster);
        RectIter iterator = RectIterFactory.create(raster, planarImg.getBounds());

        final int total = planarImg.getHeight() * planarImg.getWidth();
        
        final BufferedDataContainer container = exec.createDataContainer(new DataTableSpec(createSpecs(coverage, raster, crs)));

        long line = 0;
        iterator.startLines();
        int y = 0;
        while (!iterator.finishedLines()) {
			iterator.startPixels();
			int x=0;
            
			exec.checkCanceled();
			exec.setProgress(
        			(double)line/total, 
        			"reading pixels of line "+y
        			);
            
			while (!iterator.finishedPixels()) {

				ArrayList<DataCell> cells = new ArrayList<DataCell>(numBands);
				
				if (createId)
					cells.add(LongCellFactory.create(line));
				
				if (createCoords) {
					cells.add(IntCellFactory.create(y));
					cells.add(IntCellFactory.create(x));
				}
				
			    switch (dataType) {
			    case DataBuffer.TYPE_BYTE:
			    case DataBuffer.TYPE_INT:
			    case DataBuffer.TYPE_SHORT:
			    case DataBuffer.TYPE_USHORT:
			    	iterator.getPixel(valuesI);
			      	for (int i = 0; i < numBands; i++) {
			    	  	cells.add(IntCellFactory.create(valuesI[i]));
			      	}
			      	break;
			    case DataBuffer.TYPE_DOUBLE:
			    case DataBuffer.TYPE_FLOAT:
			    	iterator.getPixel(valuesD);
			    	for (int i = 0; i < numBands; i++) {
			    		cells.add(DoubleCellFactory.create(valuesD[i]));
			    	}
			    	break;
			    default:
			    	break;
			    }
			    
			    if (createGeom) {

			    	GridEnvelope2D envelope = new GridEnvelope2D(x, y, 1, 1);
			    	Envelope2D crsEnvelope = coverage.getGridGeometry().gridToWorld(envelope);
			    	org.locationtech.jts.geom.Polygon poly = FeatureUtilities.getPolygon(crsEnvelope, (int)line);

		            cells.add(StringCellFactory.create(poly.toString()));
			    }
	              
			    RasterFactory.createPixelInterleavedSampleModel(DataBuffer.TYPE_INT, planarImg.getWidth(), planarImg.getHeight(), 1);
				
			    container.addRowToTable(
	        			new DefaultRow(
		        			new RowKey("Row_" + line), 
		        			cells
		        			)
	        			);
				
	            exec.checkCanceled();
	
	        	line++;
	        	
	        	if (line % 100 == 1) {
	    			exec.checkCanceled();
	    			exec.setProgress(
	            			(double)line/total);
	        	}
	        	
              iterator.nextPixel();
              x++;
            }

        	
            iterator.nextLine();
            y++;
          }
        exec.setProgress(
    			(double)line/total, 
    			"reading pixels"
    			);

        
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{ out };
    }

    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        
    	m_file.saveSettingsTo(settings);
    	
    	m_createColumnId.saveSettingsTo(settings);
    	m_createColumnCoords.saveSettingsTo(settings);
    	m_createColumnGeom.saveSettingsTo(settings);
    	
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        m_file.loadSettingsFrom(settings);
        
        m_createColumnId.loadSettingsFrom(settings);
        m_createColumnCoords.loadSettingsFrom(settings);
    	m_createColumnGeom.loadSettingsFrom(settings);
    	
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

    	m_file.validateSettings(settings);
    	
    	m_createColumnId.validateSettings(settings);
    	m_createColumnCoords.validateSettings(settings);
    	m_createColumnGeom.validateSettings(settings);
    	

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
