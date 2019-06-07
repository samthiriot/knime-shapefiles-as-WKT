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
package ch.res_ear.samthiriot.knime.shapefilesaswkt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.NodeLogger;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class FeaturesDecodingUtils {

	public static DataColumnSpec createDataColumnSpecForGeom(CoordinateReferenceSystem crs) {
		
		DataColumnSpecCreator creatorGeom = new DataColumnSpecCreator(
    			SpatialUtils.GEOMETRY_COLUMN_NAME, 
    			StringCell.TYPE
    			);
		Map<String,String> properties = new HashMap<String, String>();

		properties.put(SpatialUtils.PROPERTY_CRS_CODE, SpatialUtils.getStringForCRS(crs));
		properties.put(SpatialUtils.PROPERTY_CRS_WKT, crs.toWKT());
		DataColumnProperties propertiesKWT = new DataColumnProperties(properties);
		creatorGeom.setProperties(propertiesKWT);
		return creatorGeom.createSpec();
	}

	/**
     * Creates the specification of a KNIME table 
     * based on a feature supposed to be representative.
     * @param f
     * @return
     */
    public static DataTableSpec createDataTableSpec(SimpleFeature f, NodeLogger logger, CoordinateReferenceSystem crs) {
    	
    	List<DataColumnSpec> specs = new ArrayList<DataColumnSpec>(f.getProperties().size()+2);
    	
    	// add column with id
    	// there is already an id
    	/*specs.add(new DataColumnSpecCreator(
    			"id", 
    			StringCell.TYPE
    			).createSpec());
    	*/
    	
    	// create a column with the geometry
    	specs.add(createDataColumnSpecForGeom(crs));
    	
    	// create one column per property
    	
    	
    	//System.out.println(f.getAttributes());
    	
    	Set<String> foundNames = new HashSet<String>();
    	for (Property property: f.getProperties()) {
    		String name = property.getName().toString();
    		if ("the_geom".equals(name) ||
    			"Feature".equals(name) ||  // TODO a set
    			"LookAt".equals(name) ||
    			"Style".equals(name) || 
    			"Region".equals(name)
    			)
    			continue;
    		if (!foundNames.add(name)) {
    			
    			int i = 1;
    			do {
    				i++;
    			} while (foundNames.contains(name+"("+i+")"));
    			
    			logger.warn("there was already a property named \""+name+"; we will rename this one "+name+"("+i+")");
    			name = name + "(" + i + ")";
    		}
    		
    		specs.add(getColumnSpecForFeatureProperty(property, name, logger));
    	}
		
        return new DataTableSpec(
        		"KML entities",
        		specs.toArray(new DataColumnSpec[specs.size()])
        		);
    }
    
	/**
	 * Decodes the property of the feature, and returns 
	 * a corresponding data column spec. 
	 * The given name will be the name of the column.
	 * @param property
	 * @param givenName
	 * @param logger
	 * @return
	 */
	public static DataColumnSpec getColumnSpecForFeatureProperty(
								Property property,
								String givenName,
								NodeLogger logger) {
		
		final String name = property.getName().toString();
		final Class<?> type = property.getType().getBinding();
		
		DataType knimeType = null;
		if (type.equals(Integer.class)) {
			knimeType = IntCell.TYPE;
		} else if (type.equals(String.class)) {
			knimeType = StringCell.TYPE;
		} else if (type.equals(Long.class)) {
			knimeType = LongCell.TYPE;
		} else if (type.equals(Double.class) || type.equals(Float.class)) {
			knimeType = DoubleCell.TYPE;
		} else if (type.equals(Boolean.class)) {
			knimeType = BooleanCell.TYPE;
		} else {
			logger.warn("The type of KML property "+name+" is not supported ("+property.getType()+"); we will convert it to String");
			knimeType = StringCell.TYPE;
		}
		
		return new DataColumnSpecCreator(
				givenName, 
    			knimeType
    			).createSpec();
	}
	
	/**
	 * Returns a datacell of the right type,
	 * or null if missing
	 * @param property
	 * @param feature
	 * @return
	 */
	public static DataCell getDataCellForProperty(
									Property property,
									SimpleFeature feature) {
		
		final String name = property.getName().toString();

		final Class<?> type = property.getType().getBinding();
		final Object value = feature.getProperty(name).getValue();
		
		DataCell resultCell = null;
		if (value == null) {
			resultCell = null;
		} else if (type.equals(Integer.class)) {
			resultCell = IntCellFactory.create(value.toString());
		} else if (type.equals(String.class)) {
			resultCell = StringCellFactory.create(value.toString());
		} else if (type.equals(Long.class)) {
			resultCell = LongCellFactory.create(value.toString());
		} else if (type.equals(Double.class) || type.equals(Float.class)) {
			resultCell = DoubleCellFactory.create(value.toString());
		} else if (type.equals(Boolean.class)) {
			resultCell = BooleanCellFactory.create(value.toString());
		} else {
			resultCell = StringCellFactory.create(value.toString());
		} 
		return resultCell;
	}
}
