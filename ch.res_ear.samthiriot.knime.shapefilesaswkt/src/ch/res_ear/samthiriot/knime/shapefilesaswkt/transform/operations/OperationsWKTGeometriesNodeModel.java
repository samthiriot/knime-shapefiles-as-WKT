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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.transform.operations;

import java.io.File;
import java.io.IOException;

import org.geotools.referencing.ReferencingFactoryFinder;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
public class OperationsWKTGeometriesNodeModel extends NodeModel {
    

	private SettingsModelString m_relationship = new SettingsModelString(
			"operation",
			"union"
			);
	
	private SettingsModelString m_colname = new SettingsModelString(
			"colname",
			"result"
			);
		
	/**
	 * Constructor for the node model.
	 */
	protected OperationsWKTGeometriesNodeModel() {
		super(2, 1);
	}

    
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		
		final DataTableSpec spec1 = inSpecs[0];
		if (spec1 == null)
			throw new InvalidSettingsException("no top table as input");
		
		if (!SpatialUtils.hasGeometry(spec1))
			throw new InvalidSettingsException("the top input table contains no WKT geometry");
					
		final DataTableSpec spec2 = inSpecs[0];
		if (spec2 == null)
			throw new InvalidSettingsException("no bottom table as input");
		
		if (!SpatialUtils.hasGeometry(spec2))
			throw new InvalidSettingsException("the bottom input table contains no WKT geometry");
		
		CoordinateReferenceSystem crs1 = SpatialUtils.decodeCRS(spec1);
		CoordinateReferenceSystem crs2 = SpatialUtils.decodeCRS(spec2);
		if (!crs1.equals(crs2))
			throw new InvalidSettingsException("the two tables are not spatialized on the same Coordinate Reference Sytems; please reproject them first");
		
		return new DataTableSpec[] { spec1 };
	}

	long done = 0;

	private static interface IOperationComputer {
		public Geometry compute(Geometry geom1, Geometry geom2);
	}
	
	private static class UnionComputer implements IOperationComputer {
		@Override
		public Geometry compute(Geometry geom1, Geometry geom2) {
			return geom1.union(geom2);
		}
	}
	private static class DifferenceComputer implements IOperationComputer {
		@Override
		public Geometry compute(Geometry geom1, Geometry geom2) {
			return geom1.difference(geom2);
		}
	}
	private static class IntersectionComputer implements IOperationComputer {
		@Override
		public Geometry compute(Geometry geom1, Geometry geom2) {
			return geom1.difference(geom2);
		}
	}
	
	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		
		if (!ReferencingFactoryFinder.getAuthorityNames().contains("AUTO"))
			throw new RuntimeException("No factory for autority AUTO");
		
		BufferedDataTable inputTable1 = inData[0];
		BufferedDataTable inputTable2 = inData[1];
		if (inputTable1.size() != inputTable2.size()) {
			throw new InvalidSettingsException("the two tables should have the same number of rows");
		}

		DataTableSpec outputSpec = inputTable1.getDataTableSpec();
		BufferedDataContainer container = exec.createDataContainer(outputSpec);

		final double total = inputTable1.size();
				
		final int numberOfCells = inputTable1.getDataTableSpec().getNumColumns();

		final int idxGeom = inputTable1.getSpec().findColumnIndex(SpatialUtils.GEOMETRY_COLUMN_NAME);

		final String operation = m_relationship.getStringValue();
		IOperationComputer computer = null;
		if (operation.equals("difference"))
			computer = new UnionComputer();
		else if (operation.equals("intersects"))
			computer = new DifferenceComputer();
		else if (operation.equals("touches"))
			computer = new IntersectionComputer();
		else
			throw new RuntimeException("Unknown operation: "+operation);

		final IOperationComputer operationFinal = computer;

		WKTWriter writer = new WKTWriter();
		
		// iterate each geometry of each row
		done = 0;
		SpatialUtils.applyToEachGeometry(
				inputTable1,
				inputTable2,
				geomsAndRows -> {
		     																	
				    // create the row
					DataCell[] cells = new DataCell[numberOfCells];
					for (int i=0; i<idxGeom; i++) {
						cells[i] = geomsAndRows.row1.getCell(i);
					}
					
					cells[idxGeom] = StringCellFactory.create(
							writer.write(
									operationFinal.compute(
											geomsAndRows.geometry1, 
											geomsAndRows.geometry2
											))
							);
					
					for (int i=idxGeom+1; i<numberOfCells; i++) {
						cells[i] = geomsAndRows.row1.getCell(i);
					}
					
												
					DataRow row = new DefaultRow(
							geomsAndRows.row1.getKey(), 
							cells
							);
					container.addRowToTable(row);
					  
					exec.checkCanceled();
					exec.setProgress(done++/total, "computing "+operation+" of rows "+done);
			
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

		m_relationship.saveSettingsTo(settings);
		m_colname.saveSettingsTo(settings);
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
	
		m_relationship.loadSettingsFrom(settings);
		m_colname.loadSettingsFrom(settings);
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		
		m_relationship.validateSettings(settings);
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

