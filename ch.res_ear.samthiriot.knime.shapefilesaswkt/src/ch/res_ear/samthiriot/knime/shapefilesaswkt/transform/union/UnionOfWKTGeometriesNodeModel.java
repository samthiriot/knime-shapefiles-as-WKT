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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.transform.union;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.referencing.ReferencingFactoryFinder;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
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
import org.locationtech.jts.geom.Geometry;
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
public class UnionOfWKTGeometriesNodeModel extends NodeModel {
    
	/**
	 * Constructor for the node model.
	 */
	protected UnionOfWKTGeometriesNodeModel() {
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
		
		CoordinateReferenceSystem crs = SpatialUtils.decodeCRS(inputTableSpec);
		
		Map<String,String> properties = new HashMap<String, String>();
		properties.put(SpatialUtils.PROPERTY_CRS_CODE, SpatialUtils.getStringForCRS( crs));
		properties.put(SpatialUtils.PROPERTY_CRS_WKT,  crs.toWKT());
		
    	DataColumnSpecCreator creator = new DataColumnSpecCreator(
    			SpatialUtils.GEOMETRY_COLUMN_NAME, 
    			StringCell.TYPE
    			);
    	creator.setProperties(new DataColumnProperties(properties));
    	
		List<DataColumnSpec> newColumnSpecs = new ArrayList<>(2);
		
		newColumnSpecs.add( creator.createSpec() );
		newColumnSpecs.add( new DataColumnSpecCreator(
    			"id", 
    			IntCell.TYPE).createSpec()
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
					
		return new DataTableSpec[] { createOutputSpec(spec) };
	}

	long done = 0;
	Geometry merged = null;

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

    	exec.checkCanceled();
	
		final double total = inputTable.size();
		merged = null;
	    
		// iterate each geometry of each row
    	exec.setMessage("computing the union");
    	SpatialUtils.applyToEachGeometry(
				inputTable, 
				geomAndRow -> {
					if (done++ % 10 == 0) {
						exec.checkCanceled();
						exec.setProgress(done/total);
					}
					if (merged == null)
						merged = geomAndRow.geometry;
					else 
						merged = merged.union(geomAndRow.geometry);
				});

    	// add the union
		// add one unique line with the result

		DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec());
		BufferedDataContainer container = exec.createDataContainer(outputSpec);
    	
		container.addRowToTable(new DefaultRow(
				new RowKey("global_envelope"), 
				StringCellFactory.create(merged.toString()),
				IntCellFactory.create(0)
				));
    	
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

