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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.create.pointfrom2d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.geometry.Envelope;
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
public class CreatePointFrom2DNodeModel extends NodeModel {
    
	
	private final SettingsModelString m_colname_x = new SettingsModelString(
			"colname_x", 
			"X"
			);
	private final SettingsModelString m_colname_y = new SettingsModelString(
			"colname_y", 
			"Y"
			);
	private final SettingsModelBoolean m_delete_xy = new SettingsModelBoolean(
			"delete_xy", 
			Boolean.TRUE
			);
	private final SettingsModelString m_crs = new SettingsModelString(
			"crs", 
			SpatialUtils.getDefaultCRSString()
			);
	
	/**
	 * Constructor for the node model.
	 */
	protected CreatePointFrom2DNodeModel() {
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
		
		if (!spec.containsName(m_colname_x.getStringValue()))
			throw new InvalidSettingsException("unknown column "+m_colname_x.getStringValue()+" in the table");
		if (!spec.containsName(m_colname_y.getStringValue()))
			throw new InvalidSettingsException("unknown column "+m_colname_y.getStringValue()+" in the table");
		
		// check the extend
		final double minX = ((DoubleValue)spec.getColumnSpec(m_colname_x.getStringValue()).getDomain().getLowerBound()).getDoubleValue();
		final double maxX = ((DoubleValue)spec.getColumnSpec(m_colname_x.getStringValue()).getDomain().getUpperBound()).getDoubleValue();
		final double minY = ((DoubleValue)spec.getColumnSpec(m_colname_y.getStringValue()).getDomain().getLowerBound()).getDoubleValue();
		final double maxY = ((DoubleValue)spec.getColumnSpec(m_colname_y.getStringValue()).getDomain().getUpperBound()).getDoubleValue();
		
    	final CoordinateReferenceSystem crsTarget = SpatialUtils.getCRSforString(m_crs.getStringValue());

    	/*
    	final double crsminX = crsTarget.getCoordinateSystem().getAxis(0).getMinimumValue();
    	final double crsmaxX = crsTarget.getCoordinateSystem().getAxis(0).getMaximumValue();

    	final double crsminY = crsTarget.getCoordinateSystem().getAxis(1).getMinimumValue();
    	final double crsmaxY = crsTarget.getCoordinateSystem().getAxis(1).getMaximumValue();
    	*/
    	

    	Envelope env = CRS.getEnvelope(crsTarget);
    	
    	final double crsminX = env.getMinimum(0);
    	final double crsmaxX = env.getMaximum(0);

    	final double crsminY = env.getMinimum(1);
    	final double crsmaxY = env.getMaximum(1);

    	
    	List<String> pbs = new LinkedList<String>();
    	if (minX < crsminX)
    		pbs.add("lower bound of X values "+minX+" is smaller than the minimum X coordinate "+crsminX);
    	if (maxX > crsmaxX)
    		pbs.add("upper bound of X values "+maxX+" is greater than the maximum Y coordinate "+crsmaxX);
    	if (minY < crsminY)
    		pbs.add("lower bound of Y values "+minY+" is smaller than the minimum Y coordinate "+crsminY);
    	if (maxY > crsmaxY)
    		pbs.add("upper bound of Y values "+maxY+" is greater than the maximum Y coordinate "+crsmaxY);
    	if (!pbs.isEmpty()) {
    		throw new InvalidSettingsException("The X and Y values are not compatible with the Coordinate Reference System: "+String.join(", ", pbs));
    	}
		
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
		
		final String colname_x = m_colname_x.getStringValue();
		final String colname_y = m_colname_y.getStringValue();
		final boolean delete_xy = m_delete_xy.getBooleanValue();
    	final CoordinateReferenceSystem crsTarget = SpatialUtils.getCRSforString(m_crs.getStringValue());

		List<DataColumnSpec> newColumnSpecs = new ArrayList<>(inputTableSpec.getNumColumns()+1);
		
		// copy the existing columns
		for (int i = 0; i < inputTableSpec.getNumColumns(); i++) {
			
			String name = inputTableSpec.getColumnNames()[i];
			// skip X and Y columns
			if (delete_xy && (colname_x.equals(name) || colname_y.equals(name))) {
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
		
	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		
		BufferedDataTable inputTable = inData[0];

		DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec());
		BufferedDataContainer container = exec.createDataContainer(outputSpec);

		final boolean delete_xy = m_delete_xy.getBooleanValue();
		final int idxColumnX = inputTable.getSpec().findColumnIndex(m_colname_x.getStringValue());
		final int idxColumnY = inputTable.getSpec().findColumnIndex(m_colname_y.getStringValue());
    	final CoordinateReferenceSystem crsTarget = SpatialUtils.getCRSforString(m_crs.getStringValue());
    	
    	final double minX = crsTarget.getCoordinateSystem().getAxis(0).getMinimumValue();
    	final double maxX = crsTarget.getCoordinateSystem().getAxis(0).getMaximumValue();

    	final double minY = crsTarget.getCoordinateSystem().getAxis(1).getMinimumValue();
    	final double maxY = crsTarget.getCoordinateSystem().getAxis(1).getMaximumValue();
    	
		GeometryFactory gf = new GeometryFactory();
	        	        
		CloseableRowIterator itRow = inputTable.iterator();
		while (itRow.hasNext()) {
			final DataRow row = itRow.next();
			
			List<DataCell> cells = null;
			if (delete_xy) {
				cells = new ArrayList<>(row.getNumCells()-1);
				for (int i=0; i<row.getNumCells(); i++) {
					if (i==idxColumnX || i==idxColumnY)
						continue;
					cells.add(row.getCell(i));
				}
			} else {
				cells = row.stream().collect(Collectors.toList());
			}
						
			final double x = ((DoubleValue)row.getCell(idxColumnX)).getDoubleValue();
			final double y = ((DoubleValue)row.getCell(idxColumnY)).getDoubleValue();
			
			
			// forge the geometry
			Point point = gf.createPoint(new Coordinate(x,y));

			//System.out.println(point.toString());
	        //Geometry worldPoint = JTS.toGeographic(point, crsTarget);

	        /*
			StringBuffer sb = new StringBuffer();
			sb.append("POINT(");
			sb.append(((DoubleValue)cells.get(idxColumnX)).getDoubleValue());
			sb.append(" ");
			sb.append(((DoubleValue)cells.get(idxColumnY)).getDoubleValue());
			sb.append(")");
			*/
	        
			cells.add(StringCellFactory.create(point.toString()));
			
			container.addRowToTable(new DefaultRow(
					row.getKey(), 
					cells
					));
			
		}
		itRow.close();
		
		container.close();
		BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { out };
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		
		m_colname_x.saveSettingsTo(settings);
		m_colname_y.saveSettingsTo(settings);
		m_crs.saveSettingsTo(settings);
		m_delete_xy.saveSettingsTo(settings);
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
			
		m_colname_x.loadSettingsFrom(settings);
		m_colname_y.loadSettingsFrom(settings);
		m_crs.loadSettingsFrom(settings);
		m_delete_xy.loadSettingsFrom(settings);
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		
		m_colname_x.validateSettings(settings);
		m_colname_y.validateSettings(settings);
		m_crs.validateSettings(settings);
		m_delete_xy.validateSettings(settings);
		
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

