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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.properties.coordinates;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.geotools.referencing.ReferencingFactoryFinder;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
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
public class CoordinatesOfWKTGeometriesNodeModel extends NodeModel {
    

	SettingsModelString m_colname = new SettingsModelString(
											"colname", 
											"coordinates"
											);
		
	
	/**
	 * Constructor for the node model.
	 */
	protected CoordinatesOfWKTGeometriesNodeModel() {
		super(1, 1);
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
		
		final String colname = m_colname.getStringValue();

		List<DataColumnSpec> newColumnSpecs = new ArrayList<>(inputTableSpec.getNumColumns()+1);
		
		// copy the existing columns
		for (int i = 0; i < inputTableSpec.getNumColumns(); i++) {
			newColumnSpecs.add(inputTableSpec.getColumnSpec(i));
		}
		
		newColumnSpecs.add(
				new DataColumnSpecCreator(
						colname, 
						ListCell.getCollectionType(DoubleCell.TYPE)).createSpec()
				);
		
		return new DataTableSpec(
					newColumnSpecs.toArray(
						new DataColumnSpec[newColumnSpecs.size()]));
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
					
		final String colname = m_colname.getStringValue();

		if (spec.containsName(colname))
			throw new InvalidSettingsException("the table already contains a column named "+colname);

		return new DataTableSpec[] { createOutputSpec(spec) };
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

		DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec());
		BufferedDataContainer container = exec.createDataContainer(outputSpec);

		final double total = inputTable.size();
				
		final int numberOfCells = inputTable.getDataTableSpec().getNumColumns();


		// iterate each geometry of each row
		done = 0;
		SpatialUtils.applyToEachGeometry(
				inputTable, 
				geomAndRow -> {
		     								
					List<DataCell> cells = new ArrayList<>(numberOfCells+1);
					
					cells.addAll(geomAndRow.row.stream().collect(Collectors.toList()));
					
					
					Coordinate[] coords = geomAndRow.geometry.getCoordinates();
					List<DataCell> cellsInside = new ArrayList<>(coords.length*2);
					for (int i=0; i<coords.length; i++) {
						cellsInside.add(DoubleCellFactory.create(coords[i].x));
						cellsInside.add(DoubleCellFactory.create(coords[i].y));
					}
						
					cells.add(CollectionCellFactory.createListCell(cellsInside));
						
					DataRow row = new DefaultRow(
			    		  geomAndRow.row.getKey(), 
			    		  cells
			    		  );
					container.addRowToTable(row);
					  
					if (done++ % 10 == 0) {
						exec.checkCanceled();
						exec.setProgress(done/total, "computing surface of row "+done);
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
		
		m_colname.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
	
		m_colname.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		
		m_colname.validateSettings(settings);
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

