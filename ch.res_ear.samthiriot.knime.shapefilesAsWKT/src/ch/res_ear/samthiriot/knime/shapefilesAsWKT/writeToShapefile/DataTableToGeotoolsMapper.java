package ch.res_ear.samthiriot.knime.shapefilesAsWKT.writeToShapefile;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;

/**
 * Maps a KNIME column into a geotools attribute.  
 * One is created per attribute. It is then called 
 * to translate every single cell of the column. 
 * 
 * @author Samuel Thiriot
 */
public class DataTableToGeotoolsMapper {

	public static enum GeotoolTargetType {
		Integer,
		Long,
		String,
		Double,
		Boolean,
		Ignore
	}
	
	protected final NodeLogger logger;
	protected final DataColumnSpec colspec;
	protected final GeotoolTargetType targetType;
	
	public DataTableToGeotoolsMapper(NodeLogger logger, DataColumnSpec knimeColSpec) {
		this.logger = logger;
		this.colspec = knimeColSpec;
		this.targetType = detectAttributeTypeForSpec(knimeColSpec);
	}
	
	/**
	 * Identifies which geotools type we will use to map 
	 * this given column.
	 * 
	 * @param colspec
	 * @return
	 */
	public GeotoolTargetType detectAttributeTypeForSpec(
				DataColumnSpec colspec) {
		
		if (colspec.getType().equals(IntCell.TYPE)) {
			return GeotoolTargetType.Integer;
		} 
		if (colspec.getType().equals(StringCell.TYPE)) {
			return GeotoolTargetType.String;
		}
		if (colspec.getType().equals(DoubleCell.TYPE)) {
			return GeotoolTargetType.Double;
		} 
		if (colspec.getType().equals(LongCell.TYPE)) {
			return GeotoolTargetType.Long;
		}
		if (colspec.getType().equals(BooleanCell.TYPE)) {
			return GeotoolTargetType.Boolean;
		}
		
		// TODO other?
		
		logger.warn("the column "+colspec.getName()+" is of unknown type "+colspec.getType()+"; it will be mapped to String");
		
		return GeotoolTargetType.Ignore;
	}
	

	/**
	 * Add the geotools attribute to this builder
	 * for this knime column.
	 * @param builder
	 */
	public void addAttributeForSpec(
				SimpleFeatureTypeBuilder builder 
				) {

		switch (targetType) {
		
			case Integer:
				builder.add(colspec.getName(), Integer.class);
				break;
			case String:
				builder.add(colspec.getName(), String.class);
				break;
			case Double:
				builder.add(colspec.getName(), Double.class);
				break;
			case Long:
				builder.add(colspec.getName(), Long.class);
				break;
			case Boolean:
				builder.add(colspec.getName(), Boolean.class);
				break;
			case Ignore:
				builder.add(colspec.getName(), String.class);
				break;
			default:
				throw new RuntimeException("Program(er) error: we should have dealt with type "+targetType);
		}
		builder.nillable(true);
	}
	
	public Object getValue(DataCell cell) {
		
		if (cell.isMissing())
			return null;
		
		switch (targetType) {
			case Integer:
				return ((IntCell)cell).getIntValue();
			case String:
				return ((StringCell)cell).getStringValue();
			case Double:
				return ((DoubleCell)cell).getDoubleValue();
			case Long:
				return ((LongCell)cell).getLongValue();
			case Boolean:
				return ((BooleanCell)cell).getBooleanValue();
			case Ignore:
				return cell.toString();
			default:
				throw new RuntimeException("Program(er) error: we should have dealt with type "+targetType);
		}
	}
	
	
	public Object getValueNoNull(DataCell cell) {
		
		if (cell.isMissing())
			return "";
		
		switch (targetType) {
			case Integer:
				return ((IntCell)cell).getIntValue();
			case String:
				return ((StringCell)cell).getStringValue();
			case Double:
				return ((DoubleCell)cell).getDoubleValue();
			case Long:
				return ((LongCell)cell).getLongValue();
			case Boolean:
				return ((BooleanCell)cell).getBooleanValue();
			case Ignore:
				return cell.toString();
			default:
				throw new RuntimeException("Program(er) error: we should have dealt with type "+targetType);
		}
	}
	
}
