package ch.res_ear.samthiriot.knime.shapefilesaswkt.create.geocoding.google;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.NodeLogger;

import com.google.maps.model.GeocodingResult;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.create.geocoding.GeocodingCache;

public class GoogleGeocodingCache extends GeocodingCache<List<GeocodingResult>> {

	private static GoogleGeocodingCache singleton = null;
	
	public static GoogleGeocodingCache getInstance() {
		if (singleton == null)
			singleton = new GoogleGeocodingCache();
		return singleton;
	}
	
	public static final String TABLE_NAME_GOOLE_GEOCODING = "GOOGLE_GEOCODING";

	private GoogleGeocodingCache() {
		
		super("OpenStreetMap", TABLE_NAME_GOOLE_GEOCODING);
		
	}

	@Override
	public void storeInCache(String address, List<GeocodingResult> result) {
		
		List<GeocodingResult> copy = new ArrayList<>(result.size());
		
		// remove what cannot be serialized: google plus code (useless in our case)
		for (int i=0; i<result.size(); i++) {
			GeocodingResult r = result.get(i);
			r.plusCode = null;
			copy.add(r);
		}
		super.storeInCache(address, copy);
	}
	
	

}
