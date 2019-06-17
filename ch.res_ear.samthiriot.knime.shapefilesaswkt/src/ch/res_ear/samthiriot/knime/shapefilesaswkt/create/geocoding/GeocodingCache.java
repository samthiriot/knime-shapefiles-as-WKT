package ch.res_ear.samthiriot.knime.shapefilesaswkt.create.geocoding;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.h2.jdbcx.JdbcConnectionPool;
import org.knime.core.node.NodeLogger;

/**
 * Base class to construct caches for (notably) geocoding. 
 * Stores the data into an H2 file stored somewhere in user directory.
 * 
 * TODO add date
 * 
 * @author Samuel Thiriot
 *
 * @param <T>
 */
public abstract class GeocodingCache<T> { // TODO extends Serializable

	public final String tablename;
	public final String name;
	private final NodeLogger logger = NodeLogger.getLogger(GeocodingCache.class);

	private Connection conn = null;
	private PreparedStatement preparedStatementReadOSMGeocoding = null;
	private PreparedStatement preparedStatementWriteOSMGeocoding = null;
	private PreparedStatement preparedStatementUpdateOSMGeocoding = null;

	public GeocodingCache(String name, String tablename) {

		this.name = name;
		this.tablename = tablename;
		
		final String tmpDir = getStorageDir();
		final String filename = tmpDir+File.separator+"knime_geocoding_cache.h2";

        JdbcConnectionPool cp = JdbcConnectionPool.create(
                "jdbc:h2:"+filename, "sa", "sa");
        try {
			conn = cp.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			logger.warn("unable to create the H2 cache for JSON geocoding: "+e.getLocalizedMessage(), e);
			return;
		}

        // create the table
        try {
            Statement st = conn.createStatement();
			st.execute(
					"CREATE TABLE IF NOT EXISTS "+tablename+"("
					+ "ADDRESS VARCHAR(255) PRIMARY KEY, "
					+ "RESULT other);"
					);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.warn("unable to create the table for OSM cache:"+e.getLocalizedMessage(), e);
			return;
		}

        try {
			preparedStatementReadOSMGeocoding = conn.prepareStatement(
					"SELECT * FROM "+tablename+" WHERE ADDRESS = ?"
					);
		   preparedStatementWriteOSMGeocoding = conn.prepareStatement(
	        		"INSERT INTO "+tablename+" VALUES(?,?)"
	        		);
		   preparedStatementUpdateOSMGeocoding = conn.prepareStatement(
	        		"UPDATE "+tablename+" SET RESULT=? WHERE ADDRESS = ?"
	        		);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.warn("unable to prepare the statements for OSM cache: "+e.getLocalizedMessage(), e);
		}
     
        logger.info("cache for OSM geocoding queries is up");
        
	}
	

	/**
	 * Returns the OSM result of geocoding, or null if not cached
	 * @param address
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public T getOSMGeocodingForAddress(String address) {
		
		// cache is disabled
		if (conn == null || preparedStatementReadOSMGeocoding == null)
			return null;
		
		// no need to run a query for too long strings !
		if (address.length() > 255)
			return null;
		
		ResultSet rs = null;
		try {
			preparedStatementReadOSMGeocoding.setString(1, address);
			rs = preparedStatementReadOSMGeocoding.executeQuery();
			if (!rs.next())
				return null;
		} catch (SQLException e2) {
			e2.printStackTrace();
			return null;
		}
		try {
			try {
				return (T)rs.getObject("RESULT");
			} catch (SQLException e) {
				e.printStackTrace();
				logger.warn("error when searching for cached "+address+": "+e.getLocalizedMessage(), e);
				return null;
			}
		} catch (ClassCastException e) {
			e.printStackTrace();
			// clear the table which is not valid anymore
			logger.warn("the database contains obsolete java classes; clearing cache");
			clearCache();
			return null;
			
		}
	}
	
	public void storeInCache(String address, T result) {
		if (conn == null || preparedStatementWriteOSMGeocoding == null)
			return;
		
		if (address.length() > 255) {
			logger.warn("will not cache this address which is too long (more than 255 char): "+address);
			return;
		}
		
		try {
			preparedStatementWriteOSMGeocoding.setString(1, address);
			preparedStatementWriteOSMGeocoding.setObject(2, result); 
			preparedStatementWriteOSMGeocoding.execute();
		} catch (SQLException e) {
			// maybe it existed already; trying to update
			try {
				preparedStatementUpdateOSMGeocoding.setObject(1, result);
				preparedStatementUpdateOSMGeocoding.setString(2, address);
				preparedStatementUpdateOSMGeocoding.execute();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				logger.warn("unable to cache the OSM result for address "+address);
			}
		}

	}
	
	public void clearCache() {
		try {
            Statement st = conn.createStatement();
			st.executeQuery(
					"TRUNCATE TABLE "+tablename
					);
		} catch (SQLException e1) {
			e1.printStackTrace();
			logger.error("unable to truncate the cache table; please delete the file manually");
		}
	}
	
	
	@Override
	protected void finalize() throws Throwable {
		
		if (conn != null)
			conn.close();
		
		super.finalize();
	}



	private String getStorageDir() {
		// windows?
		String basedir = System.getProperty("LOCALAPPDATA");
		if (basedir == null)
			basedir = System.getProperty("APPDATA");
		if (basedir == null)
			basedir = System.getProperty("user.home")+File.separator+".knime";
		
		logger.info("will store the cache of OpenStreetMap geocoding in "+basedir);
		return basedir;
		
	}
	

}
