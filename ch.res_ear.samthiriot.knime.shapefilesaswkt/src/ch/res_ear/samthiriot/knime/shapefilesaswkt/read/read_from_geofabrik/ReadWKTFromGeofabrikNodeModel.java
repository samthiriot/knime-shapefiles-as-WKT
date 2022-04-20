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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_geofabrik;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;


/**
 * This is an example implementation of the node model of the
 * "ReadWKTFromDatabase" node.
 * 
 * This example node performs simple number formatting
 * ({@link String#format(String, Object...)}) using a user defined format string
 * on all double columns of its input table.
 *
 * @author Samuel Thiriot
 */
public class ReadWKTFromGeofabrikNodeModel extends NodeModel {
    
    private SettingsModelString m_nameToLoad = new SettingsModelString(
    		"name_to_load", 
    		"Europe/Germany/Baden-Württemberg/Regierungsbezirk Karlsruhe");
    
    private SettingsModelString m_layerToLoad = new SettingsModelString(
    		"layer_to_load", 
    		"buildings (Building outlines)");

    // used internally during data production
    private MissingCell missing = new MissingCell("not provided");
	
	protected ReadWKTFromGeofabrikNodeModel() {
		
		super(0, 1);

	}
			

	private DataColumnSpec[] createSpecs() {

    	List<DataColumnSpec> specs = new LinkedList<DataColumnSpec>();


		// based on http://download.geofabrik.de/osm-data-in-gis-formats-free.pdf
		
    	// standard specs
    	
    	specs.add(new DataColumnSpecCreator(
    			"id", 
    			StringCell.TYPE
    			).createSpec());
    	
    	{
    		DataColumnSpecCreator creator = new DataColumnSpecCreator(
        			SpatialUtils.GEOMETRY_COLUMN_NAME, 
        			StringCell.TYPE
        			);

			Map<String,String> properties = new HashMap<String, String>();
			CoordinateReferenceSystem coordinateReferenceSystem = SpatialUtils.getCRSforString("EPSG:4326");
			properties.put(SpatialUtils.PROPERTY_CRS_CODE, SpatialUtils.getStringForCRS(coordinateReferenceSystem));
			properties.put(SpatialUtils.PROPERTY_CRS_WKT, coordinateReferenceSystem.toWKT());
			DataColumnProperties propertiesKWT = new DataColumnProperties(properties);
			creator.setProperties(propertiesKWT);
			specs.add(creator.createSpec());
    	}
    	specs.add(new DataColumnSpecCreator(
    			"osm_id", 
    			StringCell.TYPE
    			).createSpec());
    	specs.add(new DataColumnSpecCreator(
    			"code", 
    			IntCell.TYPE
    			).createSpec());
    	specs.add(new DataColumnSpecCreator(
    			"fclass", 
    			StringCell.TYPE
    			).createSpec());
    	specs.add(new DataColumnSpecCreator(
    			"name", 
    			StringCell.TYPE
    			).createSpec());
    	
    	if (m_layerToLoad.getStringValue().startsWith("places")) {
        	specs.add(new DataColumnSpecCreator(
        			"population", 
        			IntCell.TYPE
        			).createSpec());
    	} else if (m_layerToLoad.getStringValue().startsWith("roads")) {
        	specs.add(new DataColumnSpecCreator(
        			"ref", 
        			StringCell.TYPE
        			).createSpec());
        	specs.add(new DataColumnSpecCreator(
        			"oneway", 
        			StringCell.TYPE
        			).createSpec());
        	specs.add(new DataColumnSpecCreator(
        			"maxspeed", 
        			IntCell.TYPE
        			).createSpec());
        	specs.add(new DataColumnSpecCreator(
        			"layer", 
        			IntCell.TYPE
        			).createSpec());
        	specs.add(new DataColumnSpecCreator(
        			"bridge", 
        			BooleanCell.TYPE
        			).createSpec());
        	specs.add(new DataColumnSpecCreator(
        			"tunnel", 
        			BooleanCell.TYPE
        			).createSpec());
    	} else if (m_layerToLoad.getStringValue().startsWith("railways")) {
        	specs.add(new DataColumnSpecCreator(
        			"layer", 
        			IntCell.TYPE
        			).createSpec());
        	specs.add(new DataColumnSpecCreator(
        			"bridge", 
        			BooleanCell.TYPE
        			).createSpec());
        	specs.add(new DataColumnSpecCreator(
        			"tunnel", 
        			BooleanCell.TYPE
        			).createSpec());
    	} else if (m_layerToLoad.getStringValue().startsWith("waterways")) {
        	specs.add(new DataColumnSpecCreator(
        			"width", 
        			IntCell.TYPE
        			).createSpec());
    	} else if (m_layerToLoad.getStringValue().startsWith("buildings")) {
        	specs.add(new DataColumnSpecCreator(
        			"type", 
        			StringCell.TYPE
        			).createSpec());
    	} 

    	return specs.toArray(new DataColumnSpec[specs.size()]);
	}
	
	
	/**
	 * Used to monitor the progress of a file download
	 * 
	 * @author Samuel Thiriot
	 */
	public static class DownloadCountingOutputStream extends CountingOutputStream {

		private final ExecutionContext exec;
		private final double total;
		private double done = 0;
		
	    public DownloadCountingOutputStream(
	    		OutputStream out, 
	    		ExecutionContext exec,
	    		int total
	    		) {
	        super(out);
	        this.exec = exec;
	        this.total = total;
	    }

	    @Override
	    protected void afterWrite(int n) throws IOException {
	        super.afterWrite(n);

	        try {
				exec.checkCanceled();
			} catch (CanceledExecutionException e) {
				throw new RuntimeException("execution canceled");
			}
	        done += n;
	        //System.out.println(done+" / "+total+" "+(done/total));
	        exec.setProgress(done/total);
	    }

	}
	
	/**
	 * Provided a value returned by a getAttribute call on a geotools feature,
	 * returns a Knime DataCell Value: missing, or String Cell.
	 * @param stringValue
	 * @return
	 */
	protected DataCell getStringOrMissing(Object stringValue) {
		if (stringValue == null)
			return missing;
		String str = stringValue.toString();
		if (str.isEmpty())
			return missing;
		return StringCellFactory.create(str);
	}
	
	/**
	 * Provided a value returned by a getAttribute call on a geotools feature,
	 * returns a Knime DataCell Value: missing, or String Cell.
	 * @param intValue
	 * @return
	 */
	protected DataCell getIntOrMissing(Object intValue) {
		if (intValue == null)
			return missing;

		return IntCellFactory.create(intValue.toString());
	}
	
	/**
	 * Provided a value returned by a getAttribute call on a geotools feature,
	 * returns a Knime DataCell Value: missing, or String Cell. 
	 * If the value is 0, is assumed missing.
	 * @param intValue
	 * @return
	 */
	protected DataCell getIntOrMissingIfZero(Object intValue) {
		if (intValue == null)
			return missing;

		String str = intValue.toString();
		
		if (str.equals("0"))
			return missing;
		
		return IntCellFactory.create(str);
	}
	
	/**
	 * Provided a value returned by a getAttribute call on a geotools feature,
	 * returns a Knime Boolean or Missing Value: 
	 * @param intValue
	 * @return
	 */
	protected DataCell getBoolOrMissing(Object val) {
		if (val == null)
			return missing;

		String str = val.toString();
		if (str.equals("T"))
			return BooleanCellFactory.create(true);
		else if (str.equals("F"))
			return BooleanCellFactory.create(false);
		else
			return missing;
	}
	
	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
		
		// download the file
		String nameToDownload = m_nameToLoad.getStringValue();
		
		exec.checkCanceled();
		exec.setMessage("finding URL...");

		String urlToDownload = GeofabrikUtils.fetchListOfDataExtracts().get(nameToDownload);
		URL urlToDownload2 = new URL(urlToDownload);
		
		exec.checkCanceled();
		exec.setMessage("estimating size");
		int total = Integer.parseInt(urlToDownload2.openConnection().getHeaderField("Content-Length"));
		
		exec.checkCanceled();
		exec.setMessage("downloading ("+(total/1024/1024)+" Mb)");
		File destFile = null;
		{
			ExecutionContext execCopy = exec.createSubExecutionContext(0.6);
			File dirShapefiles = new File(GeofabrikUtils.getFileForCache(), "shapefiles");
			dirShapefiles.mkdirs();
			destFile = new File(
					dirShapefiles, 
					nameToDownload.replaceAll("/", "_")+".shp.zip");
			synchronized (GeofabrikUtils.getLockForFile(destFile.getAbsolutePath())) {
				if (!destFile.exists()) {
					FileOutputStream os = new FileOutputStream(destFile);
					DownloadCountingOutputStream cos = new DownloadCountingOutputStream(os, execCopy, total);
					URLConnection connection = urlToDownload2.openConnection();
					connection.setUseCaches(true);
					IOUtils.copy(connection.getInputStream(), cos);
		            cos.close();
				}
			}
			
		}
		
		exec.checkCanceled();
		exec.setProgress(0.6, "unzipping");
		File destDir = Files.createTempDirectory("geofabrik").toFile();
		File fileForShp = null;
		final String paramLayerToRead = m_layerToLoad.getStringValue();
		{
			// find the code we search
			String layerCode = paramLayerToRead;
			int idxSpace = layerCode.indexOf(" ");
			if (idxSpace > 0)
				layerCode = layerCode.substring(0, idxSpace);
			final String layerCodeToSearch = layerCode;
			ZipFile zipFile = null;
			try {
				zipFile = new ZipFile(destFile);
				Collection<ZipEntry> relevantEntries = zipFile
					.stream()
					.filter(entry -> entry.getName().contains(layerCodeToSearch))
					.collect(Collectors.toList());
				
				for (ZipEntry zipEntry: relevantEntries) {
					exec.setMessage("unzipping "+zipEntry.getName());
					InputStream is = zipFile.getInputStream(zipEntry);
					File destFileEntry = new File(destDir, zipEntry.getName());
					FileOutputStream os = new FileOutputStream(destFileEntry);
		            IOUtils.copy(is, os);
		            if (zipEntry.getName().endsWith(".shp"))
		            	fileForShp = destFileEntry;
				}
				
			} catch(ZipException e) {
				destFile.delete();
				throw new RuntimeException("error when opening downloaded data. Please try to reexecute...");
			} finally {
				if (zipFile != null)
					zipFile.close();	
			}
			
		}
		
		if (fileForShp == null)
			throw new RuntimeException("no shp file found for layer "+paramLayerToRead+" for country "+nameToDownload);
		
		// open data using the good old shapefile reader
		exec.checkCanceled();
		exec.setProgress(0.6, "loading");
		DataStore datastore = null;
		{
			
			Map<String,Object> parameters = new HashMap<>();
			try {
				parameters.put("url", fileForShp.toURI().toURL());
			} catch (MalformedURLException e2) {
				throw new RuntimeException("cannot convert the path "+fileForShp+" to an URL", e2);
			}
			try {
		        getLogger().info("opening as a shapefile: "+fileForShp);

				datastore = DataStoreFinder.getDataStore(parameters);
			} catch (IOException e1) {
				e1.printStackTrace();
				throw new InvalidSettingsException("Unable to open the url as a shape file: "+e1.getMessage());
			}
			
			if (datastore == null)
				throw new InvalidSettingsException("unable to open the shapefile from path "+fileForShp);

			// set the charset
			try {
				((ShapefileDataStore)datastore).setCharset(StandardCharsets.UTF_8);
			} catch (ClassCastException e) {
				throw new InvalidSettingsException("unable to define charset for this datastore");
			}
			
		}
		final String schemaName = datastore.getTypeNames()[0];
		
		// create result table
		DataColumnSpec[] colSpecs = createSpecs();

        DataTableSpec outputSpec = new DataTableSpec(colSpecs);
        final BufferedDataContainer container = exec.createDataContainer(outputSpec);


		int totalFeatures = datastore.getFeatureSource(schemaName).getFeatures().size();
		ExecutionContext execCreate = exec.createSubExecutionContext(0.4);

		SimpleFeatureIterator itFeature = datastore
				.getFeatureSource(schemaName)
					.getFeatures()
					.features();
		int rowIdx = 0;
		
		
		while (itFeature.hasNext()) {
			SimpleFeature feature = itFeature.next();
			
			int i=0;
			DataCell[] cells = new DataCell[colSpecs.length];
			
			cells[i++] = StringCellFactory.create(feature.getID());
			cells[i++] = StringCellFactory.create(feature.getAttribute("the_geom").toString());
			cells[i++] = StringCellFactory.create(feature.getAttribute("osm_id").toString());
			cells[i++] = IntCellFactory.create(feature.getAttribute("code").toString());
			cells[i++] = getStringOrMissing(feature.getAttribute("fclass"));
			cells[i++] = getStringOrMissing(feature.getAttribute("name"));
			
	    	if (paramLayerToRead.startsWith("places")) {
	    		cells[i++] = getIntOrMissing(feature.getAttribute("population"));

	    	} else if (paramLayerToRead.startsWith("roads")) {
				cells[i++] = getStringOrMissing(feature.getAttribute("ref"));
				cells[i++] = getStringOrMissing(feature.getAttribute("oneway").toString());
				cells[i++] = getIntOrMissingIfZero(feature.getAttribute("maxspeed"));
				cells[i++] = IntCellFactory.create(feature.getAttribute("layer").toString());
				cells[i++] = getBoolOrMissing(feature.getAttribute("bridge"));
				cells[i++] = getBoolOrMissing(feature.getAttribute("tunnel"));

	    	} else if (m_layerToLoad.getStringValue().startsWith("railways")) {
	    		cells[i++] = IntCellFactory.create(feature.getAttribute("layer").toString());
				cells[i++] = getBoolOrMissing(feature.getAttribute("bridge").toString());
				cells[i++] = getBoolOrMissing(feature.getAttribute("tunnel").toString());

	    	} else if (m_layerToLoad.getStringValue().startsWith("waterways")) {
				cells[i++] = getIntOrMissingIfZero(feature.getAttribute("width"));
				
	    	} else if (m_layerToLoad.getStringValue().startsWith("buildings")) {

				cells[i++] = getStringOrMissing(feature.getAttribute("type"));
	    	} 
		    	
			container.addRowToTable(
				new DefaultRow(
					new RowKey("Row " + rowIdx), 
					cells
				));
			
			if (rowIdx % 10 == 0) { 
				// check if the execution monitor was canceled
				exec.checkCanceled();
				execCreate.setProgress(
					(double)rowIdx / totalFeatures, 
					"reading row " + rowIdx);
				}
				rowIdx++;
			}
		itFeature.close();

		// clear data
		// TODO
		// if (destFile != null)
			//destFile.delete();
		
		datastore.dispose();
		
		// once we are done, we close the container and return its table
		container.close();
		BufferedDataTable out = container.getTable();
		return new BufferedDataTable[]{ out };

	}


	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) 
			throws InvalidSettingsException {
		
		return new DataTableSpec[]{ new DataTableSpec(createSpecs()) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
	
		m_nameToLoad.saveSettingsTo(settings);
		m_layerToLoad.saveSettingsTo(settings);
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		
		m_nameToLoad.loadSettingsFrom(settings);
		m_layerToLoad.loadSettingsFrom(settings);
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		
		m_nameToLoad.validateSettings(settings);
		m_layerToLoad.validateSettings(settings);
		
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		
		// nothing to do
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		
		// nothing to do
	}

	@Override
	protected void reset() {
		
		// nothing to do
	}

}

