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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.properties.coordinates2d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.geotools.referencing.ReferencingFactoryFinder;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.locationtech.jts.geom.Coordinate;

import ch.res_ear.samthiriot.knime.shapefilesaswkt.SpatialUtils;


/**
 * This is an example implementation of the node model of the
 * "ComputeCentroidForWKTGeometries" node.
 * 
 * This example node performs simple number formatting
 * ({@link String#format(String, Object...)}) using a user defined format string
 * on all double columns of its input table.
 *
 * @author Samuel Thiriot
 */
public class Coordinates2DOfWKTGeometriesNodeModel extends NodeModel {
    
	SettingsModelString m_colname_x = new SettingsModelString(
											"colname_x", 
											"X"
											);
		

	SettingsModelString m_colname_y = new SettingsModelString(
											"colname_y", 
											"Y"
											);
						
	/**
	 * Constructor for the node model.
	 */
	protected Coordinates2DOfWKTGeometriesNodeModel() {
		super(1, 1);
	}

	protected DataTableSpec createSpec(DataTableSpec inSpec) {
		DataTableSpecCreator creator = new DataTableSpecCreator(inSpec);
		creator.addColumns(
				new DataColumnSpecCreator(
						m_colname_x.getStringValue(),
						DoubleCell.TYPE
						).createSpec());
		creator.addColumns(
				new DataColumnSpecCreator(
						m_colname_y.getStringValue(),
						DoubleCell.TYPE
						).createSpec());
		return creator.createSpec();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		
		final DataTableSpec spec = inSpecs[0];
		if (spec == null)
			throw new InvalidSettingsException("no table as input");
		
		if (!SpatialUtils.hasGeometry(spec))
			throw new InvalidSettingsException("the input table contains no WKT geometry");
					

		if (spec.containsName(m_colname_x.getStringValue()))
			throw new InvalidSettingsException("the input table already contains a column named "+m_colname_x.getStringValue());
		
		if (spec.containsName(m_colname_y.getStringValue()))
			throw new InvalidSettingsException("the input table already contains a column named "+m_colname_y.getStringValue());
		
		if (m_colname_x.getStringValue().equals(m_colname_y.getStringValue()))
			throw new InvalidSettingsException("the names for columns X and Y should be different");
		
		return new DataTableSpec[] { createSpec(spec) };
	}

	long done = 0;

	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		
		if (!ReferencingFactoryFinder.getAuthorityNames().contains("AUTO"))
			throw new RuntimeException("No factory for autority AUTO");
		
		BufferedDataTable inputTable = inData[0];

		DataTableSpec outputSpec = createSpec(inputTable.getDataTableSpec());
		BufferedDataContainer container = exec.createDataContainer(outputSpec);

		final double total = inputTable.size();
				
		final int numberOfCells = inputTable.getDataTableSpec().getNumColumns();


		// iterate each geometry of each row
		done = 0;
		SpatialUtils.applyToEachGeometry(
				inputTable, 
				geomAndRow -> {
		     		
					// copy all cells
					List<DataCell> cells = new ArrayList<>(numberOfCells+2);
					cells.addAll(geomAndRow.row.stream().collect(Collectors.toList()));
					
					// add the cell with the type
					Coordinate coord = geomAndRow.geometry.getCoordinate();
					
					cells.add(DoubleCellFactory.create(coord.x));
					cells.add(DoubleCellFactory.create(coord.y));
					
					// append the row
					DataRow row = new DefaultRow(
			    		  geomAndRow.row.getKey(), 
			    		  cells
			    		  );
					container.addRowToTable(row);
					  
					if (done++ % 10 == 0) {
						exec.checkCanceled();
						exec.setProgress(done/total, "computing row "+done);
					}
			
				}
				);

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

		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
	
		m_colname_x.loadSettingsFrom(settings);
		m_colname_y.loadSettingsFrom(settings);
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		
		m_colname_x.validateSettings(settings);
		m_colname_y.validateSettings(settings);
		
		
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

