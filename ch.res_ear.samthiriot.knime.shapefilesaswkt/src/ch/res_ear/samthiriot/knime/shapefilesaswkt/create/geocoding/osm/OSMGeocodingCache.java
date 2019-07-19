package ch.res_ear.samthiriot.knime.shapefilesaswkt.create.geocoding.osm;

import java.util.List;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.create.geocoding.GeocodingCache;
import fr.dudie.nominatim.model.Address;

public class OSMGeocodingCache extends GeocodingCache<List<Address>> {

	private static OSMGeocodingCache singleton = null;
	
	public static OSMGeocodingCache getInstance() {
		if (singleton == null)
			singleton = new OSMGeocodingCache();
		return singleton;
	}
	
	public static final String TABLE_NAME_OSM_GEOCODING = "OSM_GEOCODING";

	private OSMGeocodingCache() {
		
		super("OpenStreetMap", TABLE_NAME_OSM_GEOCODING);
		
	}
	
	

}
