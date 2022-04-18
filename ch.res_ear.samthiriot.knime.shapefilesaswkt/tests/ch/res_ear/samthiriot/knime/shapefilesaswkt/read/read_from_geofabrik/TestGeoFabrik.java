package ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_geofabrik;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

class TestGeoFabrik {

	@Test
	void test() {
		
		Map<String,String> name2url = GeofabrikUtils.fetchListOfDataExtracts();
		
		for (String name: name2url.keySet()) 
			System.out.println(name);

		
		//fail("Not yet implemented");
	}

}
