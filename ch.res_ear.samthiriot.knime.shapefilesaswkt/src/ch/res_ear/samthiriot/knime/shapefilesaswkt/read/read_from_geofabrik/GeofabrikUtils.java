package ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_geofabrik;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.geotools.data.FeatureReader;
import org.geotools.data.FileDataStore;
import org.geotools.data.geojson.GeoJSONDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GeofabrikUtils {
	
	public static final String URL_INDEX_GEOFABRIK = "https://download.geofabrik.de/index-v1.json";
	// 
	
// https://download.geofabrik.de/index-v1-nogeom.json
	
	public static File readGeofabrikIndexIntoFile() {
		
		
	    try {
	    	
	    	// load the file first 
		    URL url = new URL(URL_INDEX_GEOFABRIK);
		    InputStream inputStream = url.openStream();
		    
		    File f = new File("/tmp/test.json");
		    if (f.exists())
		    	return f;
		    
		    FileWriter fileWriter = new FileWriter(f);
		    
			IOUtils.copy(inputStream, fileWriter, StandardCharsets.UTF_8);
			
			return f;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("unable to read the file");
		}
	    
	}
	
	private static Map<String,String> cachedName2Url = null;
	
	public static Map<String,String> fetchListOfDataExtracts() {

		if (cachedName2Url != null)
			return cachedName2Url;
		
		GeoJSONDataStoreFactory factory = new GeoJSONDataStoreFactory();

		FileDataStore store = null;
		SimpleFeatureIterator it = null;
	    try {
	    	
	    	/*Map<String,?> params = new HashMap<>();
	    	params.put(GeoJSONDataStoreFactory.URL_PARAM.key, new URL(URL_INDEX_GEOFABRIK));
*/
	    	
	    	File f = readGeofabrikIndexIntoFile();
	    	
			store = factory.createDataStore(f);

	    	SimpleFeatureCollection features = store.getFeatureSource(store.getTypeNames()[0]).getFeatures();
			
		    it = features.features();
		    
		    Map<String,String> name2url = new HashMap<>();
		    
		    Map<String,String> id2parent = new HashMap<>();
		    Map<String,String> id2name = new HashMap();
		    
		    // decode features
		    while (it.hasNext()) {
		        SimpleFeature ft = it.next();
		        String id = ft.getAttribute("id").toString();
		        String name = ft.getAttribute("name").toString();
		        
		        id2name.put(id, name);
		        
		        // find parent (optional)
		        String parent = (String) ft.getAttribute("parent");
		        if (parent != null) {
		        	id2parent.put(id, parent);
		        	//System.out.println(id+" has for parent "+parent);
		        } else {
		        	//System.out.println(id+" has no parent");
		        }

		        // find url
		        ObjectNode urls = (ObjectNode)ft.getAttribute("urls");
		        JsonNode nodeShp = urls.findValue("shp");
		        // skip index without shp
		        if (nodeShp == null)
		        	continue;
		        
		        String shp = nodeShp.asText();
		        
		        name2url.put(id, shp);
		        
		        /*
		        System.out.println(ft);
		        System.out.println(id);
		        System.out.println(name);
		        System.out.println(shp);

		        System.out.println();
		        */
		    }
		    
		    // find parents
		    Map<String,String> hierarchy2url = new TreeMap<>();
		    for (String id: name2url.keySet()) {
		    	
		    	final String url = name2url.get(id);
		    	
		    	String newId = id2name.get(id);
		    	
		    	String parent = id2parent.get(id);
	    		while (parent != null) {
	    			newId = id2name.get(parent) +"/" + newId;
	    			id = parent;
	    			parent = id2parent.get(id);
		    	} 
		    	
	    		hierarchy2url.put(newId, url);
		    }
		    
		    cachedName2Url = Collections.unmodifiableMap(hierarchy2url);
		    return cachedName2Url;
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new RuntimeException("The index URL seems corrupted: "+URL_INDEX_GEOFABRIK, e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("unable to access the list of data from "+URL_INDEX_GEOFABRIK, e);
		} finally {
			if (it != null)
				it.close();
			
			if (store != null)
				store.dispose();
			
		} 
	    
	    
	}
}
