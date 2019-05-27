package ch.res_ear.samthiriot.knime.shapefilesaswkt;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
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

public class FeaturesDecodingUtils {

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
