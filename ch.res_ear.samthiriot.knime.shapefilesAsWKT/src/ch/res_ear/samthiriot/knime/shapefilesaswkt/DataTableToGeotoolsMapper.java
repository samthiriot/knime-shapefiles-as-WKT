package ch.res_ear.samthiriot.knime.shapefilesaswkt;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

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

	
	protected final IWarningWriter warnWriter;
	protected final DataColumnSpec colspec;
	protected final GeotoolTargetType targetType;
	
	public DataTableToGeotoolsMapper(
			IWarningWriter warnWriter, 
			DataColumnSpec knimeColSpec
			) {
		this.warnWriter =warnWriter;
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
		
		warnWriter.warn("the column "+colspec.getName()+" is of unknown type "+colspec.getType()+"; it will be mapped to String");
		
		return GeotoolTargetType.Ignore;
	}
	
	/**
	 * returns a name compliant with the limitation
	 * @return
	 */
	public String getName() {
		return colspec.getName();
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
				builder.add(getName(), Integer.class);
				break;
			case String:
				builder.add(getName(), String.class);
				break;
			case Double:
				builder.add(getName(), Double.class);
				break;
			case Long:
				builder.add(getName(), Long.class);
				break;
			case Boolean:
				builder.add(getName(), Boolean.class);
				break;
			case Ignore:
				builder.add(getName(), String.class);
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
				//System.out.println("Int value for "+colspec.getName()+": "+((IntCell)cell).getIntValue());
				return new Integer(((IntValue)cell).getIntValue());
			case String:
				return ((StringValue)cell).getStringValue(); 
			case Double:
				return ((DoubleValue)cell).getDoubleValue();
			case Long:
				return ((LongValue)cell).getLongValue();
			case Boolean:
				return ((BooleanValue)cell).getBooleanValue();
			case Ignore:
				final String s2 = cell.toString();
				if (s2.length() > 254) {
					warnWriter.warn("due to shapefile limitations, truncating for column "+colspec.getName()+" the value "+s2);
					return s2.substring(0, 254);
				}
				return s2;
			default:
				throw new RuntimeException("Program(er) error: we should have dealt with type "+targetType);
		}
	}
	
	
	public Object getValueNoNull(DataCell cell) {
		
		if (cell.isMissing())
			return "";
		
		switch (targetType) {
			case Integer:
				return ((IntValue)cell).getIntValue();
			case String:
				return ((StringValue)cell).getStringValue();
			case Double:
				return ((DoubleValue)cell).getDoubleValue();
			case Long:
				return ((LongValue)cell).getLongValue();
			case Boolean:
				return ((BooleanValue)cell).getBooleanValue();
			case Ignore:
				return cell.toString();
			default:
				throw new RuntimeException("Program(er) error: we should have dealt with type "+targetType);
		}
	}
	
}
