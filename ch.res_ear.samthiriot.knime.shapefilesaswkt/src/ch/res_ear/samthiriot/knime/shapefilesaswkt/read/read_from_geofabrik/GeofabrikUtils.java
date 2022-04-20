package ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_geofabrik;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.knime.core.node.NodeLogger;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;

public class GeofabrikUtils {
	
	private static final NodeLogger logger = NodeLogger.getLogger(
			ReadWKTFromGeofabrikNodeModel.class);
	
	public static final String URL_INDEX_GEOFABRIK = "https://download.geofabrik.de/index-v1-nogeom.json"; // -nogeom
	// https://download.geofabrik.de/index-v1.json
		
	private static final Object lockDownloadGeofabrikIndex = new Object();
	
	public static File readGeofabrikIndexIntoFile() {
		
		
	    try {
	    	
	    	// load the file first 
		    URL url = new URL(URL_INDEX_GEOFABRIK);
		    URLConnection connection = url.openConnection();
		    connection.setUseCaches(false);
		    InputStream inputStream = connection.getInputStream();
		    
		    File f = new File(GeofabrikUtils.getFileForCache(), "index-v1-nogeom.json"); // -nogeom
		    
		    synchronized (lockDownloadGeofabrikIndex) {
		    	
		    	if (f.exists())
			    	return f;
			    
			    FileWriter fileWriter = new FileWriter(f);
			    
				IOUtils.copy(inputStream, fileWriter, StandardCharsets.UTF_8);
				fileWriter.flush();
				fileWriter.close();
				
				return f;
					
			}
		    
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("unable to read the file");
		}
	    
	}
	
	/**
	 * Cached decoding of the index of geofabrik tools
	 */
	//private static Map<String,String> cachedName2Url = null;
	
	public static class ListOfZonesReader implements Callable<Map<String,String>> {
	    @Override
	    public Map<String,String> call() throws Exception {
	    	return fetchListOfDataExtracts();
	    }
	}
	
	/**
	 * returns a Future with the list of available zones.
	 * @return
	 */
	public static CompletableFuture<Map<String,String>> obtainListOfDataExtracts() {
		
		return CompletableFuture.supplyAsync(
				new Supplier<Map<String,String>>() {
		    @Override
		    public Map<String,String> get() {
		        return fetchListOfDataExtracts();
		    }
		});
	}
	
	public static Map<String,String> fetchListOfDataExtracts() {
    	
		File f = null;

	    try {
	    	
	    	f = readGeofabrikIndexIntoFile();

	        JSONTokener tokener = new JSONTokener(new FileInputStream(f));
	        JSONObject root = new JSONObject(tokener);
	        
	        JSONArray features = root.getJSONArray("features");
	        		    
		    Map<String,String> name2url = new HashMap<>();
		    
		    Map<String,String> id2parent = new HashMap<>();
		    Map<String,String> id2name = new HashMap<>();
		    
		    // decode features
		    for (int i=0; i<features.length(); i++) {
	        	JSONObject feature = (JSONObject)features.get(i);
	        	JSONObject properties = feature.getJSONObject("properties");

	            String id = properties.getString("id");
		        String name = properties.getString("name");
		        
		        //logger.warn("id = "+id+", name = "+name);
		        
		        id2name.put(id, name);
		        
		        // find parent (optional)
		        String parent = null;
		        if (properties.has("parent")) 
		        	parent = properties.getString("parent");
		        if (parent != null) {
		        	id2parent.put(id, parent);
		        	//logger.warn(id+" has for parent "+parent);
		        } else {
		        	//logger.warn(id+" has no parent");
		        }

		        // find url
		        JSONObject urls = properties.getJSONObject("urls");
		        //ObjectNode urls = (ObjectNode)ft.getAttribute("urls");
		        // skip index without shp
		        if (!urls.has("shp"))
		        	continue;
		        
		        String shp = urls.getString("shp");
				        
		        name2url.put(id, shp);
		        
		    }
		    
		    // find parents
		    Map<String,String> hierarchy2url = new TreeMap<>();
		    for (String id: name2url.keySet()) {
		    	
		    	final String url = name2url.get(id);
		    	
		    	String newId = id2name.get(id);
		    	
		    	String parent = id2parent.get(id);
		    			    	
	    		while (parent != null) {
			    	//logger.warn("for id "+newId+" we found parent "+parent);
	    			newId = id2name.get(parent) +"/" + newId;
	    			id = parent;
	    			parent = id2parent.get(id);
		    	} 
		    	
	    		hierarchy2url.put(newId, url);
		    	//logger.warn(newId+" => "+url);
		    }
		    
		    //cachedName2Url = Collections.unmodifiableMap(hierarchy2url);
		    return hierarchy2url;
			
	    } catch (JSONException e1) {
			e1.printStackTrace();
			if (f != null)
				f.delete();
			throw new RuntimeException("error while parsing file "+URL_INDEX_GEOFABRIK, e1);
	    } catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("unable to access the list of data from "+URL_INDEX_GEOFABRIK, e);
		} finally {
			
		}
	    
	}
	
	/*
	public static Map<String,String> fetchListOfDataExtracts() {

		//if (cachedName2Url != null)
		//	return cachedName2Url;
		
		GeoJSONDataStoreFactory factory = new GeoJSONDataStoreFactory();

		FileDataStore store = null;
		SimpleFeatureIterator it = null;
	    try {
	    	
	    	//Map<String,?> params = new HashMap<>();
	    	//params.put(GeoJSONDataStoreFactory.URL_PARAM.key, new URL(URL_INDEX_GEOFABRIK));
	    	
	    	File f = readGeofabrikIndexIntoFile();
	    	
			store = factory.createDataStore(f);

	    	SimpleFeatureCollection features = store.getFeatureSource(store.getTypeNames()[0]).getFeatures();
			
		    it = features.features();
		    
		    Map<String,String> name2url = new HashMap<>();
		    
		    Map<String,String> id2parent = new HashMap<>();
		    Map<String,String> id2name = new HashMap<>();
		    
		    // decode features
		    while (it.hasNext()) {
		        SimpleFeature ft = it.next();

		        logger.warn("processing feature "+ft.toString());
		        
		        String id = ft.getAttribute("id").toString();
		        String name = ft.getAttribute("name").toString();
		        
		        logger.warn("id = "+id+", name = "+name);
		        
		        id2name.put(id, name);
		        
		        
		        // find parent (optional)
		        String parent = (String) ft.getAttribute("parent");
		        if (parent != null) {
		        	id2parent.put(id, parent);
		        	logger.warn(id+" has for parent "+parent);
		        } else {
		        	logger.warn(id+" has no parent");
		        }

		        // find url
		        ObjectNode urls = (ObjectNode)ft.getAttribute("urls");
		        JsonNode nodeShp = urls.findValue("shp");
		        // skip index without shp
		        if (nodeShp == null)
		        	continue;
		        
		        String shp = nodeShp.asText();
		        
		        name2url.put(id, shp);
		        
		    }
		    
		    // find parents
		    Map<String,String> hierarchy2url = new TreeMap<>();
		    for (String id: name2url.keySet()) {
		    	
		    	final String url = name2url.get(id);
		    	
		    	String newId = id2name.get(id);
		    	
		    	String parent = id2parent.get(id);
		    			    	
	    		while (parent != null) {
			    	logger.warn("for id "+newId+" we found parent "+parent);
	    			newId = id2name.get(parent) +"/" + newId;
	    			id = parent;
	    			parent = id2parent.get(id);
		    	} 
		    	
	    		hierarchy2url.put(newId, url);
		    	logger.warn(newId+" => "+url);
		    }
		    
		    //cachedName2Url = Collections.unmodifiableMap(hierarchy2url);
		    return hierarchy2url;
			
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
	    
	}*/
	
	/**
	 * Get the directory to use to store cache for GeoFabrik
	 * @return
	 */
	public static File getFileForCache() {
		File dir = new File(
				SpatialUtils.getFileForCache(),
				"geofabrik"
				);
		dir.mkdirs();
		return dir;
	}
	
	public final static Map<String,Object> filecode2lock = Collections.synchronizedMap(new HashMap<>());
	
	public static Object getLockForFile(String filecode) {
		synchronized (filecode2lock) {
			if (!filecode2lock.containsKey(filecode))
				filecode2lock.put(filecode, new Object());
			return filecode2lock.get(filecode);
		}
	}
	
}
