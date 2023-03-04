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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_kml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Collection;

import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.referencing.CRS;
import org.geotools.xsd.Parser;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
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
        
		// parse the content using KML
		//org.geotools.kml.v22.KMLConfiguration configuration = new org.geotools.kml.v22.KMLConfiguration();
        Parser parser = new Parser(new org.geotools.kml.v22.KMLConfiguration());
        SimpleFeature f;
		try {
			f = (SimpleFeature) parser.parse( inputStream );
		} catch (Exception e2) { // IOException | SAXException | ParserException...
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

        exec.setMessage("loading the KML structure");
       
    	SimpleFeature f = decodeFileFromKML();
    	    	
    	// CRS is always WGS84 for KML
		CoordinateReferenceSystem crs;
		try {
			crs = CRS.decode("EPSG:4326"); // WGS84 
		} catch (FactoryException e) {
			throw new RuntimeException("unable to find the Coordinate Reference System EPSG:4326. This error should not happen. Please report this bug for solving.");
		}        
		
		DataTableSpec tableSpec = FeaturesDecodingUtils.createDataTableSpec(f, getLogger(), crs);
        
        final BufferedDataContainer container = exec.createDataContainer(tableSpec);

        final DataCell missing = new MissingCell("was null in KML");
        
        exec.setMessage("reading entries from KML");
        // read the file
        Collection<?> placemarks = (Collection<?>) f.getAttribute("Feature");
        int line = 0;
        
        if (placemarks != null) {
	        double total = placemarks.size();
	        for (Object p: placemarks) {
	        	SimpleFeatureImpl feature = (SimpleFeatureImpl)p;
	        	
	        	ArrayList<DataCell> cells = new ArrayList<DataCell>(2+f.getProperties().size());
	        	
	        	// add id
	        	//cells.add(StringCellFactory.create(feature.getID()));
	        	
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

	        		DataCell resultCell = FeaturesDecodingUtils.getDataCellForProperty(
							property,
							feature
							);
	        		
	        		if (resultCell == null)
	        			cells.add(missing);
	        		else
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
        

        // add flow variables for the CRS
    	pushFlowVariableString("CRS_code", SpatialUtils.getStringForCRS(crs));
        pushFlowVariableString("CRS_WKT", crs.toWKT());
        
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
    	
        return new DataTableSpec[]{ null }; // createDataTableSpec(decodeFileFromKML())
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

