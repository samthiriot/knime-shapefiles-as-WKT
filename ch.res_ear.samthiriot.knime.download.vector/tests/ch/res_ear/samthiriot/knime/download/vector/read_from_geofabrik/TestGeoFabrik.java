package ch.res_ear.samthiriot.knime.download.vector.read_from_geofabrik;

import java.util.Map;

import org.junit.Test;

class TestGeoFabrik {

	@Test
	void test() {
		
		Map<String,String> name2url = GeofabrikUtils.fetchListOfDataExtracts();
		
		for (String name: name2url.keySet()) 
			System.out.println(name);

		
		//fail("Not yet implemented");
	}

}
