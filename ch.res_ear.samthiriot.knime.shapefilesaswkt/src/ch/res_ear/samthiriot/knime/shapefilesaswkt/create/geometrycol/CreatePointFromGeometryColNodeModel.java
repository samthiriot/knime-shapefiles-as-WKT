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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.create.geometrycol;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.geotools.geometry.jts.WKTReader2;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;


/**
 * This is an example implementation of the node model of the
 * "SpatialPropertySurface" node.
 * 
 * This example node performs simple number formatting
 * ({@link String#format(String, Object...)}) using a user defined format string
 * on all double columns of its input table.
 *
 * @author Samuel Thiriot
 */
public class CreatePointFromGeometryColNodeModel extends NodeModel {
    
	
	private final SettingsModelString m_colname_geom = new SettingsModelString(
			"colname_geom", 
			null
			);
	private final SettingsModelBoolean m_delete_geom = new SettingsModelBoolean(
			"delete_geom", 
			Boolean.TRUE
			);
	private final SettingsModelString m_crs = new SettingsModelString(
			"crs", 
			SpatialUtils.getDefaultCRSString()
			);
	
	/**
	 * Constructor for the node model.
	 */
	protected CreatePointFromGeometryColNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		
		final DataTableSpec spec = inSpecs[0];
		if (spec == null)
			throw new InvalidSettingsException("no table as input");
			
		if (spec.containsName(SpatialUtils.GEOMETRY_COLUMN_NAME))
			throw new InvalidSettingsException("the input table already contains a column named the_geom");
		
		if (!spec.containsName(m_colname_geom.getStringValue()))
			throw new InvalidSettingsException("unknown column "+m_colname_geom.getStringValue()+" in the table");


    	final CoordinateReferenceSystem crsTarget = SpatialUtils.getCRSforString(m_crs.getStringValue());
		
		return new DataTableSpec[] { createOutputSpec(spec) };
	}
	


	/**
	 * Creates the output table spec from the input spec. For each double column in
	 * the input, one String column will be created containing the formatted double
	 * value as String.
	 * 
	 * @param inputTableSpec
	 * @return
	 */
	private DataTableSpec createOutputSpec(DataTableSpec inputTableSpec) {
		
		final String colname_geom = m_colname_geom.getStringValue();
		final boolean delete_geom = m_delete_geom.getBooleanValue();
    	final CoordinateReferenceSystem crsTarget = SpatialUtils.getCRSforString(m_crs.getStringValue());

		List<DataColumnSpec> newColumnSpecs = new ArrayList<>(inputTableSpec.getNumColumns()+1);
		
		// copy the existing columns
		for (int i = 0; i < inputTableSpec.getNumColumns(); i++) {
			
			String name = inputTableSpec.getColumnNames()[i];
			// skip geom columns
			if (delete_geom && colname_geom.equals(name)) {
				continue;
			}
			newColumnSpecs.add(inputTableSpec.getColumnSpec(i));
		}
		
		Map<String,String> properties = new HashMap<String, String>(inputTableSpec.getProperties());
		properties.put(SpatialUtils.PROPERTY_CRS_CODE, SpatialUtils.getStringForCRS(crsTarget));
		properties.put(SpatialUtils.PROPERTY_CRS_WKT, crsTarget.toWKT());
    	DataColumnSpecCreator creator = new DataColumnSpecCreator(
    			SpatialUtils.GEOMETRY_COLUMN_NAME, 
    			StringCell.TYPE
    			);		
    	creator.setProperties(new DataColumnProperties(properties));
		newColumnSpecs.add(creator.createSpec());
		
		return new DataTableSpec(
					newColumnSpecs.toArray(
						new DataColumnSpec[newColumnSpecs.size()]));
	}
		
	protected void setWarningMessage(int countMissing, int countErrors) {
		if (countMissing == 0 && countErrors == 0)
			return;
		
		StringBuffer sb = new StringBuffer("there are ");
		if (countMissing > 0)
			sb.append(countMissing).append(" entities missing geometry");
		if (countErrors > 0) {
			if (countMissing > 0)
				sb.append(" and");
			sb.append(countErrors).append(" errors in geometries");
		}
		
		setWarningMessage(sb.toString());
	}
	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(
			final BufferedDataTable[] inData, 
			final ExecutionContext exec)
			throws Exception {
		
		BufferedDataTable inputTable = inData[0];

		DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec());
		BufferedDataContainer container = exec.createDataContainer(outputSpec);

		final boolean delete_geom = m_delete_geom.getBooleanValue();
		final int idxColumnGeom = inputTable.getSpec().findColumnIndex(m_colname_geom.getStringValue());
    	//final CoordinateReferenceSystem crsTarget = SpatialUtils.getCRSforString(m_crs.getStringValue());
    	
		GeometryFactory gf = new GeometryFactory();
	    WKTReader2 reader = new WKTReader2(gf);
	    
	    int countMissing = 0;
	    int countError = 0;
	    
	    exec.setProgress(0.0, "processing");
	    final long total = inputTable.size();
	    long done = 0;
		CloseableRowIterator itRow = inputTable.iterator();
		while (itRow.hasNext()) {
			final DataRow row = itRow.next();
			
			List<DataCell> cells = null;
			if (delete_geom) {
				cells = new ArrayList<>(row.getNumCells()-1);
				for (int i=0; i<row.getNumCells(); i++) {
					if (i==idxColumnGeom)
						continue;
					cells.add(row.getCell(i));
				}
			} else {
				cells = row.stream().collect(Collectors.toList());
			}
						
			DataCell cell = row.getCell(idxColumnGeom);
			if (cell.isMissing()) {
				cells.add(cell);
				countMissing++;
				setWarningMessage(countMissing, countError);
			} else {
				final String wkt = ((StringValue)cell).getStringValue();
				
				
				// forge the geometry
				try {
					org.locationtech.jts.geom.Geometry geom = reader.read(wkt);
					cells.add(StringCellFactory.create(geom.toString()));
				} catch (org.locationtech.jts.io.ParseException e) {
					countError++;
					setWarningMessage(countMissing, countError);
					this.getLogger().warn("unable to decode the geometry \""+wkt+"\" in row "+row.getKey()+": "+e.getMessage());
				}
				
			}
			container.addRowToTable(new DefaultRow(
					row.getKey(), 
					cells
					));
			
			done++;
			if (done % 20 == 0) {
				exec.checkCanceled();
			    exec.setProgress((double)done/total, "processing row "+row.getKey());
			}
		}
		itRow.close();
		
	    exec.setProgress(1.0, "closing...");

		container.close();
		BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { out };
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		
		m_colname_geom.saveSettingsTo(settings);
		m_delete_geom.saveSettingsTo(settings);
		m_crs.saveSettingsTo(settings);
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
			
		m_colname_geom.loadSettingsFrom(settings);
		m_delete_geom.loadSettingsFrom(settings);
		m_crs.loadSettingsFrom(settings);
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		
		m_colname_geom.validateSettings(settings);
		m_delete_geom.validateSettings(settings);
		m_crs.validateSettings(settings);
		
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		
		// nothing to do
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		
		// nothing to do
	}

	@Override
	protected void reset() {
		
		// nothing to do
	}
}

