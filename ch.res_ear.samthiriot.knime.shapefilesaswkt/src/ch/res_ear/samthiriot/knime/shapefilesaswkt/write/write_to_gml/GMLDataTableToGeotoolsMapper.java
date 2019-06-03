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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.write.write_to_gml;

import org.apache.xmlbeans.impl.common.XMLChar;
import org.knime.core.data.DataColumnSpec;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.DataTableToGeotoolsMapper;
import ch.res_ear.samthiriot.knime.shapefilesaswkt.IWarningWriter;

/**
 * Specific declination of the mapper for GML.
 * Converts names of attributes by stripping 
 * characters creating encoding issues.
 * 
 * @author Samuel Thiriot
 *
 */
public class GMLDataTableToGeotoolsMapper extends DataTableToGeotoolsMapper {

	public GMLDataTableToGeotoolsMapper(IWarningWriter warnWriter, DataColumnSpec knimeColSpec) {
		super(warnWriter, knimeColSpec);
		
	}

	@Override
	public String getName() {
		//return StringEscapeUtils.escapeXml10(colspec.getName());
		if (!XMLChar.isValidName(colspec.getName())) {
			this.warnWriter.warn(
					"the column named "+colspec.getName()+" is not valid for XML; "
					+ "this will probably make the GML file impossible to use. "
					+ "You might rename these columns to avoid problems");
		} 
		return colspec.getName();
		//return StringEscapeUtils.escapeHtml4(colspec.getName());
	}
}

