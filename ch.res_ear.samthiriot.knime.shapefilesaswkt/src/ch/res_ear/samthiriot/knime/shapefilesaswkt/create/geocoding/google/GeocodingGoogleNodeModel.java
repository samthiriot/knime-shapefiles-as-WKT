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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.create.geocoding.google;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;


/**
 * This is an example implementation of the node model of the
 * "SpatialPropertySurface" node.
 * 
 * This example node performs simple number formatting
 * ({@link String#format(String, Object...)}) using a user defined format string
 * on all double columns of its input table.
 *
 * @author Samuel Thiriot
 */
public class GeocodingGoogleNodeModel extends NodeModel {
    
	
	private final SettingsModelString m_colname_address = new SettingsModelString(
			"colname_address", 
			"address"
			);
	private final SettingsModelString m_api_key = new SettingsModelString(
			"api_key", 
			null
			);
	
	/**
	 * Constructor for the node model.
	 */
	protected GeocodingGoogleNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		if (m_api_key.getStringValue() == null)
			throw new InvalidSettingsException("you should define an API key");
		
		final DataTableSpec spec = inSpecs[0];
		if (spec == null)
			throw new InvalidSettingsException("no table as input");
			
		if (spec.containsName(SpatialUtils.GEOMETRY_COLUMN_NAME))
			throw new InvalidSettingsException("the input table already contains a column named the_geom");
		
		if (!spec.containsName(m_colname_address.getStringValue()))
			throw new InvalidSettingsException("unknown column "+m_colname_address.getStringValue()+" in the table");

		return new DataTableSpec[] { createOutputSpec(spec) };
	}
	


	/**
	 * Creates the output table spec from the input spec. For each double column in
	 * the input, one String column will be created containing the formatted double
	 * value as String.
	 * 
	 * @param inputTableSpec
	 * @return
	 */
	private DataTableSpec createOutputSpec(DataTableSpec inputTableSpec) {
	
    	final CoordinateReferenceSystem crsTarget = SpatialUtils.getCRSforString("epsg:4326");

		List<DataColumnSpec> newColumnSpecs = new ArrayList<>(inputTableSpec.getNumColumns()+5);
		
		// copy the existing columns
		for (int i = 0; i < inputTableSpec.getNumColumns(); i++) {
			newColumnSpecs.add(inputTableSpec.getColumnSpec(i));
		}
		
		// add column for geom
		Map<String,String> properties = new HashMap<String, String>(inputTableSpec.getProperties());
		properties.put(SpatialUtils.PROPERTY_CRS_CODE, SpatialUtils.getStringForCRS(crsTarget));
		properties.put(SpatialUtils.PROPERTY_CRS_WKT, crsTarget.toWKT());
    	DataColumnSpecCreator creator = new DataColumnSpecCreator(
    			SpatialUtils.GEOMETRY_COLUMN_NAME, 
    			StringCell.TYPE
    			);		
    	creator.setProperties(new DataColumnProperties(properties));
		newColumnSpecs.add(creator.createSpec());
		
		// add the geometry type cell
		newColumnSpecs.add(new DataColumnSpecCreator("geometry type", StringCell.TYPE).createSpec());
		
		// add the boolean cell for partial results
		newColumnSpecs.add(new DataColumnSpecCreator("is partial", BooleanCell.TYPE).createSpec());
		
		// add the place id
		newColumnSpecs.add(new DataColumnSpecCreator("place id", StringCell.TYPE).createSpec());
						
		// add the types
		newColumnSpecs.add(new DataColumnSpecCreator("types", ListCell.getCollectionType(StringCell.TYPE)).createSpec());
		
		return new DataTableSpec(
					newColumnSpecs.toArray(
						new DataColumnSpec[newColumnSpecs.size()]));
	}
		
	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		
		BufferedDataTable inputTable = inData[0];

		final String apiKey = m_api_key.getStringValue();
		final long timeoutSeconds = 60;
		final String colname = m_colname_address.getStringValue();
		
		// prepare the connection 
		GeoApiContext geoApiCtxt = new GeoApiContext.Builder()
				.apiKey(apiKey)
				.connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
			    .build();

		// TODO proxy
				
		final MissingCell missing = new MissingCell("no location found");

		DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec());
		BufferedDataContainer container = exec.createDataContainer(outputSpec);
    	
		GeometryFactory gf = new GeometryFactory();
	        	        
		final int idxColAddress = inputTable.getSpec().findColumnIndex(colname);
		
		int countMultipleResults = 0;
		
		CloseableRowIterator itRow = inputTable.iterator();
		long done = 0;
		final double total = inputTable.size();
		while (itRow.hasNext()) {
			final DataRow row = itRow.next();
			
			if (row.getCell(idxColAddress).isMissing()) {
				// cannot geocode a missing address !
				getLogger().warn("missing address in row "+row.getKey());
				// add a row with empty location
				List<DataCell> cells = new ArrayList<>(outputSpec.getNumColumns());
				// copy the original cells
				for (int i=0; i<row.getNumCells(); i++)
					cells.add(row.getCell(i));
				// add all the cells
				for (int i=0; i<5; i++)
					cells.add(missing);
				container.addRowToTable(
						new DefaultRow(
								row.getKey(), 
								cells));
				continue;
			}
			// geocode this address
			final String address = ((StringValue)row.getCell(idxColAddress)).getStringValue();
		
			exec.checkCanceled();
			exec.setProgress(done++/total, "geocoding "+address);

			List<GeocodingResult> results = GoogleGeocodingCache.getInstance().getOSMGeocodingForAddress(address);
			if (results == null) {
				// not in cache; calling the Google API
				try {
					GeocodingResult[] resultsRaw =  GeocodingApi.geocode(
							geoApiCtxt,
						    address
							).await();
					results = Arrays.asList(resultsRaw);
				} catch (ApiException | InterruptedException | IOException e) {
					e.printStackTrace();
					throw new RuntimeException("error while geocoding: "+e.getLocalizedMessage());
				}
				GoogleGeocodingCache.getInstance().storeInCache(address, results);
			}
			if (results == null || results.isEmpty()) {
				getLogger().warn("unable to find a location for address "+address);
				// add a row with empty location
				List<DataCell> cells = new ArrayList<>(outputSpec.getNumColumns());
				// copy the original cells
				for (int i=0; i<row.getNumCells(); i++)
					cells.add(row.getCell(i));
				// add all the cells
				for (int i=0; i<5; i++)
					cells.add(missing);
				container.addRowToTable(
						new DefaultRow(
								row.getKey(), 
								cells));
				continue;
			}
			if (results.size() > 1) {
				countMultipleResults++;
				if (countMultipleResults == 1)
					setWarningMessage("One address led to multiple locations; one row is created for each location, group the results by address to get only the first result");
				else 
					setWarningMessage(countMultipleResults+" addresses led to multiple locations; one row is created for each location, group the results by address to get only the first result");
			}
			int currentResult = 1;
			for (GeocodingResult result: results) {

				List<DataCell> cells = new ArrayList<>(outputSpec.getNumColumns());
				// copy the original cells
				for (int i=0; i<row.getNumCells(); i++)
					cells.add(row.getCell(i));

				// add the geometry (point) cell
				double latitude = result.geometry.location.lat;
				double longitude = result.geometry.location.lng;
				Point point = gf.createPoint(new Coordinate(longitude,latitude));
				cells.add(StringCellFactory.create(point.toString()));
				
				// add the geometry type cell
				cells.add(StringCellFactory.create(result.geometry.locationType.name()));
				
				// add the boolean cell for partial results
				cells.add(BooleanCellFactory.create(result.partialMatch));
				
				// add the place id
				cells.add(StringCellFactory.create(result.placeId));
								
				// add the types
				List<DataCell> cellsForTypes = Arrays.asList(result.types)
													.stream()
													.map(t -> t.name())
													.map(s -> StringCellFactory.create(s))
													.collect(Collectors.toList());
						
				cells.add(CollectionCellFactory.createListCell(cellsForTypes));
				
				// EPSG 3857
				
				// that's a row
				if (results.size() == 1) {
					// same row id
					container.addRowToTable(
							new DefaultRow(
									row.getKey(), 
									cells));
				} else {
					// forge row id
					container.addRowToTable(
							new DefaultRow(
									new RowKey(row.getKey().getString()+"_"+(currentResult++)), 
									cells));
				}
				
			}
			
			
		}
		itRow.close();
		
		container.close();
		BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { out };
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		
		m_colname_address.saveSettingsTo(settings);
		m_api_key.saveSettingsTo(settings);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
			
		m_colname_address.loadSettingsFrom(settings);
		m_api_key.loadSettingsFrom(settings);


	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		
		m_colname_address.validateSettings(settings);
		m_api_key.validateSettings(settings);

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

