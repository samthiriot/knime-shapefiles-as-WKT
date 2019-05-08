package ch.res_ear.samthiriot.knime.shapefilesAsWKT.writeToShapefile;

import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;

import ch.res_ear.samthiriot.knime.shapefilesAsWKT.IWarningWriter;

/**
 * Converts Knime table data into shapefile data; 
 * enforces shapefile limitations. 
 * 
 * @author Samuel Thiriot
 *
 */
public class DataTableToGeotoolsMapperForShapefile extends DataTableToGeotoolsMapper {

	protected final Set<String> usedNames;
	
	private static final int MAX_LENGTH_NAME = 10;
	private static final int MAX_STRING_LENGTH = 254;

	public DataTableToGeotoolsMapperForShapefile(
			IWarningWriter warnWriter, 
			DataColumnSpec knimeColSpec,
			Set<String> usedNames) {
		
		super(warnWriter, knimeColSpec);

		this.usedNames = usedNames;
	}

	/**
	 * returns a name compliant with the limitations
	 * Many columns with the same name will be 
	 * renamed like "long column" becomes "long col0",
	 * "long col1"... "long co99"
	 * @return
	 */
	@Override
	public String getName() {
		final String s = colspec.getName();
		String res = s;
		if (s.length() >= MAX_LENGTH_NAME || usedNames.contains(s)) {
			// we have to shorten the name
			String temptative = s.substring(0, MAX_LENGTH_NAME);
			int postfix = 0;
			int places = 1;
			while (usedNames.contains(temptative)) {
				// we have to search for another one
				temptative = s.substring(0, MAX_LENGTH_NAME - places)+postfix;
				postfix++;
				if (postfix == 10)
					places = 2;
				else if (postfix == 100)
					places = 3;
			}
			res = temptative;
			usedNames.add(temptative);
		}
		if (!s.equals(res)) {
			warnWriter.warn(
					"The column \""+s+"\" will be shortened to \""+res+"\" according to shapefile format limitations.");
		}
		return res;
	}
	
	@Override
	public Object getValue(DataCell cell) {
		
		Object res = super.getValue(cell);
		
		if ((res != null) && (res instanceof String)) {
			String s = (String)res;;
			if (s.length() > MAX_STRING_LENGTH) {
				warnWriter.warn(
						"due to shapefile limitations, truncating for column "+
						colspec.getName()+" the value "+s);
				return s.substring(0, MAX_STRING_LENGTH);
			}
		}

		return res;
	}
	
	@Override
	public Object getValueNoNull(DataCell cell) {
		Object res = super.getValueNoNull(cell);
		
		if ((res != null) && (res instanceof String)) {
			String s = (String)res;;
			if (s.length() > MAX_STRING_LENGTH) {
				warnWriter.warn(
						"due to shapefile limitations, truncating for column "+
						colspec.getName()+" the value "+s);
				return s.substring(0, MAX_STRING_LENGTH);
			}
		}

		return res;
	}
}
