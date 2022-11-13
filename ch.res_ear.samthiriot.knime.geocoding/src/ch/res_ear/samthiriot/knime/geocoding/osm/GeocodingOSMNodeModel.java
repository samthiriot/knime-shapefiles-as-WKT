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
package ch.res_ear.samthiriot.knime.geocoding.osm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CloseableRowIterator;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;
import fr.dudie.nominatim.client.JsonNominatimClient;
import fr.dudie.nominatim.client.request.NominatimSearchRequest;
import fr.dudie.nominatim.client.request.paramhelper.PolygonFormat;
import fr.dudie.nominatim.model.Address;


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
public class GeocodingOSMNodeModel extends NodeModel {
    
	// TODO param to order things? 
	
	// TODO time in cache !!! 
	
	private final SettingsModelString m_colname_address = new SettingsModelString(
			"colname_address", 
			"address"
			);
	private final SettingsModelString m_url = new SettingsModelString(
			"url", 
			"https://nominatim.openstreetmap.org/"
			);
	private final SettingsModelString m_email = new SettingsModelString(
			"email", 
			""
			);

	private final SettingsModelBoolean m_geom = new SettingsModelBoolean(
			"full geometries", 
			Boolean.FALSE
			);

	
	/**
	 * Constructor for the node model.
	 */
	protected GeocodingOSMNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        final String email = m_email.getStringValue();
     	if (email == null || email.trim().isEmpty())
	        	setWarningMessage("please provide an email to comply with the terms of service");

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
		newColumnSpecs.add(new DataColumnSpecCreator("type", StringCell.TYPE).createSpec());
		
		// add the place id
		newColumnSpecs.add(new DataColumnSpecCreator("osm id", StringCell.TYPE).createSpec());
		newColumnSpecs.add(new DataColumnSpecCreator("osm type", StringCell.TYPE).createSpec());

		// add the license String
		newColumnSpecs.add(new DataColumnSpecCreator("licence", StringCell.TYPE).createSpec());

		// add the rank
		newColumnSpecs.add(new DataColumnSpecCreator("rank", IntCell.TYPE).createSpec());
		newColumnSpecs.add(new DataColumnSpecCreator("rank interpretation", StringCell.TYPE).createSpec());
		

		return new DataTableSpec(
					newColumnSpecs.toArray(
						new DataColumnSpec[newColumnSpecs.size()]));
	}
		
	/**
	 * @see https://wiki.openstreetmap.org/wiki/Nominatim/Development_overview
	 * @param rank
	 * @return
	 */
	public static String getRankInterpretation(int rank) {
		if (rank <= 2)
			return "Continent, sea";
		else if (rank <= 4)
			return "Country";
		else if (rank <= 8)
			return "State";
		else if (rank <= 10)
			return "Region";
		else if (rank <= 12)
			return "County";
		else if (rank <= 16)
			return "City";
		else if (rank <= 17)
			return "Island, town, moor, waterways	";
		else if (rank <= 18)
			return "Village, hamlet, municipality, district, borough, airport, national park	";
		else if (rank <= 20)
			return "Suburb, croft, subdivision, farm, locality, islet	";
		else if (rank <= 22)
			return "Hall of residence, neighbourhood, housing estate, landuse (polygon only)	";
		else if (rank <= 26)
			return "Airport, street, road	";
		else if (rank <= 27)
			return "Paths, cycleways, service roads, etc.";
		else if (rank <= 28)
			return "House, building";
		else 
			return "Other";
		
	}
	
	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		
		BufferedDataTable inputTable = inData[0];

		final String colname = m_colname_address.getStringValue();
		final Boolean fullGeom = m_geom.getBooleanValue();
		
		// TODO proxy
		
		DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec());
		BufferedDataContainer container = exec.createDataContainer(outputSpec);
    	
		GeometryFactory gf = new GeometryFactory();
	        	        
		final int idxColAddress = inputTable.getSpec().findColumnIndex(colname);
		
		int countMultipleResults = 0;
		
				
		CloseableRowIterator itRow = inputTable.iterator();
		long done = 0;
		final double total = inputTable.size();
		
		if (inputTable.size() > 100 && m_email.getStringValue().trim().isEmpty())
			setWarningMessage("you ask for the geocoding of many addresses; "+
					"please provide a valid email so the managers of the service can contact you "
					+ "if this usage creates issues");
		
		MissingCell missing = new MissingCell("no location found");

		// http
        final SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        final ClientConnectionManager connexionManager = new SingleClientConnManager(null, registry);

        final HttpClient httpClient = new DefaultHttpClient(connexionManager, null);
        final String baseUrl = m_url.getStringValue();
        final String email = m_email.getStringValue();
       
		final JsonNominatimClient nominatimClient = new JsonNominatimClient(
				baseUrl, 
				httpClient, 
				email
				);

		long timestampLastQuery = 0;
		
		while (itRow.hasNext()) {
			
			final DataRow row = itRow.next();
			
			// geocode this address
			final String address = ((StringValue)row.getCell(idxColAddress)).getStringValue();
		
			exec.checkCanceled();
			exec.setProgress(done++/total, "geocoding "+address);

	        List<Address> addresses = OSMGeocodingCache.getInstance().getOSMGeocodingForAddress(address);
	        if (addresses == null) {
	        	// no cache
	       
		        final NominatimSearchRequest r = new NominatimSearchRequest();
		        r.setQuery(address);
		        r.setPolygonFormat(PolygonFormat.GEO_JSON);
		        //r.setPolygonFormat(PolygonFormat.);
	
		        // wait
		        exec.setMessage("(wait) geocoding "+address);
		        while (System.currentTimeMillis() - timestampLastQuery < 1000) {
		        	Thread.sleep(100);
		        	exec.checkCanceled();
		        }
		        
		        exec.setMessage("geocoding "+address);
				try {
					addresses = nominatimClient.search(r);
					timestampLastQuery = System.currentTimeMillis();
				} catch (RuntimeException e) {
					e.printStackTrace();
					throw new RuntimeException("error while geocoding: "+e.getLocalizedMessage());
				}
				if (addresses != null)
					OSMGeocodingCache.getInstance().storeInCache(address, addresses);
				
				exec.checkCanceled();
	        }
	        
			if (addresses == null || addresses.isEmpty()) {
				getLogger().warn("unable to find a location for address "+address);
				// add a row with empty location
				List<DataCell> cells = new ArrayList<>(outputSpec.getNumColumns());
				// copy the original cells
				for (int i=0; i<row.getNumCells(); i++)
					cells.add(row.getCell(i));
				// add all the cells
				for (int i=0; i<7; i++)
					cells.add(missing);
				container.addRowToTable(
						new DefaultRow(
								row.getKey(), 
								cells));
				continue;
			}
			
			if (addresses.size() > 1) {
				countMultipleResults++;
				if (countMultipleResults == 1)
					setWarningMessage("One address led to multiple locations; one row is created for each location, group the results by address to get only the first result");
				else 
					setWarningMessage(countMultipleResults+" addresses led to multiple locations; one row is created for each location, group the results by address to get only the first result");
			}
			int currentResult = 1;
			for (Address addr: addresses) {

				List<DataCell> cells = new ArrayList<>(outputSpec.getNumColumns());
				// copy the original cells
				for (int i=0; i<row.getNumCells(); i++)
					cells.add(row.getCell(i));

				// add the geometry (point) cell
				
				double latitude = addr.getLatitude();
				double longitude = addr.getLongitude();
				Point point = gf.createPoint(new Coordinate(longitude,latitude));
				if (fullGeom && addr.getGeojson() != null)
					cells.add(StringCellFactory.create(addr.getGeojson().toString()));
				else 
					cells.add(StringCellFactory.create(point.toString()));
								
				// add the geometry type cell
				cells.add(StringCellFactory.create(addr.getElementType()));
				
				// add the osm id and type
				cells.add(StringCellFactory.create(addr.getOsmId()));
				cells.add(StringCellFactory.create(addr.getOsmType()));
				
				// add licence
				cells.add(StringCellFactory.create(addr.getLicence()));
				
				// add rank
				cells.add(IntCellFactory.create(addr.getPlaceRank()));
				cells.add(StringCellFactory.create(getRankInterpretation(addr.getPlaceRank())));

				
				// that's a row
				if (addresses.size() == 1) {
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
		m_email.saveSettingsTo(settings);
		m_geom.saveSettingsTo(settings);
		m_url.saveSettingsTo(settings);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
			
		m_colname_address.loadSettingsFrom(settings);
		m_email.loadSettingsFrom(settings);
		m_geom.loadSettingsFrom(settings);
		m_url.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		
		m_colname_address.validateSettings(settings);
		m_email.validateSettings(settings);
		m_geom.validateSettings(settings);
		m_url.validateSettings(settings);

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

