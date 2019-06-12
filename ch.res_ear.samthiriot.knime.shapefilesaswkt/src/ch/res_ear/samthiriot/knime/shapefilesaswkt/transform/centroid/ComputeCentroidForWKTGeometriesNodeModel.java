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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.transform.centroid;

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
import org.locationtech.jts.io.WKTWriter;

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
public class ComputeCentroidForWKTGeometriesNodeModel extends NodeModel {
    
	/**
	 * Constructor for the node model.
	 */
	protected ComputeCentroidForWKTGeometriesNodeModel() {
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
		
		if (!SpatialUtils.hasGeometry(spec))
			throw new InvalidSettingsException("the input table contains no WKT geometry");
					
		return new DataTableSpec[] { spec };
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

		DataTableSpec outputSpec = inputTable.getDataTableSpec();
		BufferedDataContainer container = exec.createDataContainer(outputSpec);

		final double total = inputTable.size();
				
		final int numberOfCells = inputTable.getDataTableSpec().getNumColumns();

		//CoordinateReferenceSystem crsOrig = SpatialUtils.decodeCRS(inputTable.getDataTableSpec());

		//CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory("AUTO", null);

		final int idxGeomCol = inputTable.getDataTableSpec().findColumnIndex(SpatialUtils.GEOMETRY_COLUMN_NAME);
		
		WKTWriter writer = new WKTWriter();

		// iterate each geometry of each row
		done = 0;
		SpatialUtils.applyToEachGeometry(
				inputTable, 
				geomAndRow -> {
		     																	
				    // create the row
					DataCell[] cells = new DataCell[numberOfCells];
					for (int i=0; i<numberOfCells; i++) {
						if (i == idxGeomCol)
							// convert geometry
							cells[i] = StringCellFactory.create(writer.write(geomAndRow.geometry.getCentroid()));
						else 
							cells[i] = geomAndRow.row.getCell(i);
						
					}
												
					DataRow row = new DefaultRow(
			    		  geomAndRow.row.getKey(), 
			    		  cells
			    		  );
					container.addRowToTable(row);
					  
					if (done++ % 100 == 0) {
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
		
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
	
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		
		// nothing to do
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

