package ch.res_ear.samthiriot.knime.shapefilesAsWKT.properties.surface;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeocentricCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
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
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import ch.res_ear.samthiriot.knime.shapefilesAsWKT.SpatialUtils;


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
public class SpatialPropertySurfaceNodeModel extends NodeModel {
    
	
	private final SettingsModelString m_colname = new SettingsModelString(
					"colname", 
					"geom_surface");

	/**
	 * Constructor for the node model.
	 */
	protected SpatialPropertySurfaceNodeModel() {
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
			
		final String colname = m_colname.getStringValue();
		
		if (null != spec.getColumnSpec(colname))
			throw new InvalidSettingsException("the table already contains a column named "+colname);
		
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
		
		final String colname = m_colname.getStringValue();

		List<DataColumnSpec> newColumnSpecs = new ArrayList<>(inputTableSpec.getNumColumns()+1);
		
		// copy the existing columns
		for (int i = 0; i < inputTableSpec.getNumColumns(); i++) {
			newColumnSpecs.add(inputTableSpec.getColumnSpec(i));
		}
		
		newColumnSpecs.add(
				new DataColumnSpecCreator(colname, DoubleCell.TYPE).createSpec()
				);
		
		return new DataTableSpec(
					newColumnSpecs.toArray(
						new DataColumnSpec[newColumnSpecs.size()]));
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

		CoordinateReferenceSystem crsOrig = SpatialUtils.decodeCRS(inputTable.getDataTableSpec());

		CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory("AUTO", null);

		/*
		// depending to the original CRS, it might be necessary to pass through an intermediate CRS
		CoordinateReferenceSystem crsInter = DefaultGeocentricCRS.CARTESIAN;
		MathTransform transform1 = CRS.findMathTransform(
				crsOrig, 
				crsInter, 
				true);
		boolean reprojectInter = false;
		*/
		
		// iterate each geometry of each row
		done = 0;
		SpatialUtils.applyToEachGeometry(
				inputTable, 
				geomAndRow -> {
			      
					try {
						
						Geometry transformed = null;
						

						//try {
							Point centroid = geomAndRow.geometry.getCentroid(); 

							CoordinateReferenceSystem crsTarget = factory.createProjectedCRS(
									"AUTO:42001," + centroid.getX()//String.format(Locale.ENGLISH, "%.2f", centroid.getX()) 
									+ "," + centroid.getY()//String.format(Locale.ENGLISH, "%.5f", centroid.getY())
									);
							MathTransform transform2 = CRS.findMathTransform(
									crsOrig, 
									crsTarget, 
									true);
							transformed = JTS.transform(geomAndRow.geometry, transform2);
							/*
						} catch (FactoryException | TransformException e) {
							e.printStackTrace();
							System.out.println("using a second projection");
							Geometry geomInter = JTS.transform(geomAndRow.geometry, transform1);
							Point centroid = geomInter.getCentroid(); 
							CoordinateReferenceSystem crsTarget = factory.createProjectedCRS(
									"AUTO:42001," + centroid.getX()//String.format(Locale.ENGLISH, "%.2f", centroid.getX()) 
									+ "," + centroid.getY()//String.format(Locale.ENGLISH, "%.5f", centroid.getY())
									);
							MathTransform transform2 = CRS.findMathTransform(
									crsInter, 
									crsTarget, 
									true);
							transformed = JTS.transform(geomInter, transform2);

						}
						*/
					    // compute the geometry
					    double surfaceSquareMeter = transformed.getArea();
						
					    // create the row
						DataCell[] cells = new DataCell[numberOfCells+1];
						for (int i=0; i<numberOfCells; i++)
							cells[i] = geomAndRow.row.getCell(i);
							
						cells[numberOfCells] = DoubleCellFactory.create(surfaceSquareMeter);
						
						DataRow row = new DefaultRow(
				    		  geomAndRow.row.getKey(), 
				    		  cells
				    		  );
						container.addRowToTable(row);
						  
						if (done++ % 100 == 0) {
							exec.checkCanceled();
							exec.setProgress(done/total, "computing surface of row "+done);
						}
						  
					} catch (FactoryException | MismatchedDimensionException | TransformException e) {
						e.printStackTrace();
						throw new InvalidSettingsException("An error occured during the reprojection of geometries; please reproject your geometries first: "+e.getMessage());
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

