package ch.res_ear.samthiriot.gosp.knime.shapefilesAsWKT.readFromShapefile;

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
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.NodeLogger;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.gosp.knime.shapefilesAsWKT.SpatialUtils;


public class DataTableToGeotoolsMapper {

	private static final MissingCell missing = new MissingCell("no data provided");
	
	public enum GeotoolDetectedType {
			
		ATTRIBUTE_STRING,
		ATTRIBUTE_INTEGER,
		ATTRIBUTE_DOUBLE,
		ATTRIBUTE_BOOLEAN,
		
		ATTRIBUTE_OTHER,
		
		SPATIAL;
		
	}
	
	private AttributeDescriptor gtDesc;
	private DataColumnSpec knimeSpec;
	private DataType knimeType;
	private GeotoolDetectedType gtDetectedType;

	protected final NodeLogger logger;
	protected final CoordinateReferenceSystem coordinateReferenceSystem;

	public DataTableToGeotoolsMapper(
			AttributeDescriptor gtDesc, 
			CoordinateReferenceSystem coordinateReferenceSystem, 
			NodeLogger logger) {
		
		this.gtDesc = gtDesc;
		this.logger = logger;
		this.coordinateReferenceSystem = coordinateReferenceSystem;
		
		createKnimeSpecForGeoToolDescriptor();
		
	}
		
    protected void createKnimeSpecForGeoToolDescriptor() {
    	

    	System.out.println(gtDesc);
    	
    	System.out.println(gtDesc.getType());
    	
    	if (gtDesc instanceof GeometryDescriptor) {
    		GeometryDescriptor gtDescGeom = (GeometryDescriptor)gtDesc;
    		// TODO ((GeometryDescriptor) gtDesc).getCoordinateReferenceSystem();
    		System.out.println(gtDescGeom.getType());
    		
    		knimeType = StringCell.TYPE;
			gtDetectedType = GeotoolDetectedType.SPATIAL;

    	} else if (gtDesc instanceof AttributeDescriptor) {
    		AttributeDescriptor gtDescAtt = (AttributeDescriptor)gtDesc;
    		
    		
    		System.out.println(gtDescAtt);
    		System.out.println(gtDescAtt.getType());
    		
    		if (gtDescAtt.getType().getBinding().equals(String.class)) {
    			knimeType = StringCell.TYPE;
    			gtDetectedType = GeotoolDetectedType.ATTRIBUTE_STRING;
    		} else if (gtDescAtt.getType().getBinding().equals(Integer.class)) {
    			knimeType = IntCell.TYPE;
    			gtDetectedType = GeotoolDetectedType.ATTRIBUTE_INTEGER;
    		} else if (gtDescAtt.getType().getBinding().equals(Double.class)) {
    			knimeType = DoubleCell.TYPE;
    			gtDetectedType = GeotoolDetectedType.ATTRIBUTE_DOUBLE;
    		} else if (gtDescAtt.getType().getBinding().equals(Boolean.class)) {
    			knimeType = BooleanCell.TYPE;
    			gtDetectedType = GeotoolDetectedType.ATTRIBUTE_BOOLEAN;
    		} else {
    			logger.warn("the column "+gtDesc.getName()
    						+" which is of unknown type: "+gtDesc.getType()
    						+" will be considered as a String");
    			knimeType = StringCell.TYPE;
    			gtDetectedType = GeotoolDetectedType.ATTRIBUTE_OTHER;

    		}
    		
    	} 

    	DataColumnSpecCreator creator = new DataColumnSpecCreator(
    			gtDesc.getLocalName(), 
    			knimeType
    			);

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
		
		switch (gtDetectedType) {
		case ATTRIBUTE_BOOLEAN:
			return BooleanCellFactory.create((Boolean)gtObject);
		case ATTRIBUTE_INTEGER:
			return IntCellFactory.create((Integer)gtObject);
		case ATTRIBUTE_DOUBLE:
			return DoubleCellFactory.create((Double)gtObject);
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
