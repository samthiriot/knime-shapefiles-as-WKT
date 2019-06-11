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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_shapefile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
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
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;

/**
 * Maps a geotools attribute into a KNIME column. 
 * One is created per attribute. It is then called 
 * to translate every single cell of the column. 
 * 
 * @author Samuel Thiriot
 */
public class GeotoolsToDataTableMapper {

	private static final MissingCell missing = new MissingCell("no data provided");
	
	public enum GeotoolDetectedType {
			
		ATTRIBUTE_STRING,
		ATTRIBUTE_INTEGER,
		ATTRIBUTE_DOUBLE,
		ATTRIBUTE_BOOLEAN,
		ATTRIBUTE_LONG,
		ATTRIBUTE_BIGDECIMAL,
		
		ATTRIBUTE_OTHER,
		
		SPATIAL;
		
	}
	
	private AttributeDescriptor gtDesc;
	private DataColumnSpec knimeSpec;
	private DataType knimeType;
	private GeotoolDetectedType gtDetectedType;

	protected final NodeLogger logger;
	protected final CoordinateReferenceSystem coordinateReferenceSystem;

	protected MissingCell missingCell = new MissingCell("no data");
	
	public GeotoolsToDataTableMapper(
			AttributeDescriptor gtDesc, 
			CoordinateReferenceSystem coordinateReferenceSystem, 
			NodeLogger logger) {
		
		this.gtDesc = gtDesc;
		this.logger = logger;
		this.coordinateReferenceSystem = coordinateReferenceSystem;
		
		createKnimeSpecForGeoToolDescriptor();
		
	}
		
    protected void createKnimeSpecForGeoToolDescriptor() {

    	if (gtDesc instanceof GeometryDescriptor) {
    		GeometryDescriptor gtDescGeom = (GeometryDescriptor)gtDesc;
    		// TODO ((GeometryDescriptor) gtDesc).getCoordinateReferenceSystem();
    		
    		knimeType = StringCell.TYPE;
			gtDetectedType = GeotoolDetectedType.SPATIAL;

    	} else if (gtDesc instanceof AttributeDescriptor) {
    		AttributeDescriptor gtDescAtt = (AttributeDescriptor)gtDesc;
    		
    		if (String.class.isAssignableFrom(gtDescAtt.getType().getBinding())) {
    			knimeType = StringCell.TYPE;
    			gtDetectedType = GeotoolDetectedType.ATTRIBUTE_STRING;
    		} else if (Integer.class.isAssignableFrom(gtDescAtt.getType().getBinding())) {
    			knimeType = IntCell.TYPE;
    			gtDetectedType = GeotoolDetectedType.ATTRIBUTE_INTEGER;
    		} else if (Double.class.isAssignableFrom(gtDescAtt.getType().getBinding())) {
    			knimeType = DoubleCell.TYPE;
    			gtDetectedType = GeotoolDetectedType.ATTRIBUTE_DOUBLE;
    		} else if (Boolean.class.isAssignableFrom(gtDescAtt.getType().getBinding())) {
    			knimeType = BooleanCell.TYPE;
    			gtDetectedType = GeotoolDetectedType.ATTRIBUTE_BOOLEAN;
    		} else if (Long.class.isAssignableFrom(gtDescAtt.getType().getBinding())) {
    			knimeType = LongCell.TYPE;
    			gtDetectedType = GeotoolDetectedType.ATTRIBUTE_LONG;
    		} else if (BigDecimal.class.isAssignableFrom(gtDescAtt.getType().getBinding())) {
    			knimeType = DoubleCell.TYPE;
    			gtDetectedType = GeotoolDetectedType.ATTRIBUTE_BIGDECIMAL;
    			logger.warn("the column "+gtDesc.getName()
					+" of type BigDecimal will be converted to a Double, possibly with a loss of precision");
		
    		} else {
    			logger.warn("the column "+gtDesc.getName()
    						+" which is of unknown type: "+gtDesc.getType()
    						+" will be considered as a String");
    			knimeType = StringCell.TYPE;
    			gtDetectedType = GeotoolDetectedType.ATTRIBUTE_OTHER;

    		}
    		
    	} else {
    		throw new RuntimeException("unknown attribute type "+gtDesc);
    	}

    	DataColumnSpecCreator creator = null;
    	if (gtDetectedType == GeotoolDetectedType.SPATIAL) {
    		creator = new DataColumnSpecCreator(
        			SpatialUtils.GEOMETRY_COLUMN_NAME, 
        			knimeType
        			);
    	} else {
    		creator = new DataColumnSpecCreator(
        			gtDesc.getLocalName(), 
        			knimeType
        			);
    	}
    	 
    	if (this.gtDetectedType == GeotoolDetectedType.SPATIAL) {
    		
    		Map<String,String> properties = new HashMap<String, String>();
    		properties.put(SpatialUtils.PROPERTY_CRS_CODE, SpatialUtils.getStringForCRS(coordinateReferenceSystem));
    		properties.put(SpatialUtils.PROPERTY_CRS_WKT, coordinateReferenceSystem.toWKT());
    		DataColumnProperties propertiesKWT = new DataColumnProperties(properties);
    		creator.setProperties(propertiesKWT);
    	}
    	
    			
    	this.knimeSpec = creator.createSpec();
    	
    }
    
    public DataColumnSpec getKnimeColumnSpec() {
    	return this.knimeSpec;
    }
    
    public DataType getKnimeType() {
    	return this.knimeType;
    }
	
    /**
     * Convert a value from Geotools into 
     * a Knime DataCell for a DataTable
     * @param gtObject
     * @return
     */
	public DataCell convert(Object gtObject) {
		
		if (gtObject == null)
			return missingCell;
		
		switch (gtDetectedType) {
		case ATTRIBUTE_BOOLEAN:
			return BooleanCellFactory.create((Boolean)gtObject);
		case ATTRIBUTE_INTEGER:
			return IntCellFactory.create((Integer)gtObject);
		case ATTRIBUTE_DOUBLE:
			return DoubleCellFactory.create((Double)gtObject);
		case ATTRIBUTE_LONG:
			return LongCellFactory.create((Long)gtObject);
		case ATTRIBUTE_BIGDECIMAL:
			return DoubleCellFactory.create(((BigDecimal)gtObject).doubleValue());
		case ATTRIBUTE_STRING:
		case ATTRIBUTE_OTHER:
		case SPATIAL:
			final String val = gtObject.toString();
			if (val.isEmpty()) {
				return missing;
			} else {
				return StringCellFactory.create(val);
			}
		default:
			throw new RuntimeException("unknown type "+gtDetectedType);
		}
	}

    
}
