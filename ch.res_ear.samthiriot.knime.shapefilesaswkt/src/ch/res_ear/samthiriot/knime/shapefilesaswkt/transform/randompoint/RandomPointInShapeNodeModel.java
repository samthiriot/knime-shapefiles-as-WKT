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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.transform.randompoint;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTWriter;

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;
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
public class RandomPointInShapeNodeModel extends NodeModel {
    

    private final SettingsModelBoolean m_autoseed = 
    		new SettingsModelBoolean("seed_auto", true);
    
    private final SettingsModelIntegerBounded m_seed =
            new SettingsModelIntegerBounded("seed",
                        55555,
                        Integer.MIN_VALUE, Integer.MAX_VALUE); 
    
    private final SettingsModelIntegerBounded m_count =
            new SettingsModelIntegerBounded("count",
                        1,
                        1, Integer.MAX_VALUE); 
            
    
	/**
	 * Constructor for the node model.
	 */
	protected RandomPointInShapeNodeModel() {
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

	protected MersenneTwister random;
	protected Uniform uniform; 
	protected GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
	
	protected Point getRandomPointOnGeometry(Point point) {
		return point;
	}
	
	protected Point getRandomPointOnGeometry(Polygon polygon) {
		
		Envelope envelope = polygon.getEnvelopeInternal();
		
		final double minx = envelope.getMinX();
		final double miny  = envelope.getMinY();
		final double maxx = envelope.getMaxX();
		final double maxy = envelope.getMaxY();
		
		Point point = null;
		// draw a random point, and continue to draw points as long as we did not 
		// found one inside the shape
		int iterations = 0;
		do {
			if (iterations++ > 5)
				// TODO remove
				System.out.println("find random point: iteration "+iterations);
			
			point = geometryFactory.createPoint(new Coordinate(
					uniform.nextDoubleFromTo(minx, maxx), 
					uniform.nextDoubleFromTo(miny, maxy)
					));
			
		} while (!point.within(polygon));
		
		return point;
	}
	
	protected Point getRandomPointOnLine(LineString line) {
		
		final double length = line.getLength();
		
		// search for a point we will select somewhere in the way
		final double randomDistance = uniform.nextDoubleFromTo(0.0, length);
		
		// find the segment of interest
		double cumulatedDistance = 0.0;
		Coordinate previousPoint = line.getStartPoint().getCoordinate();
		int i;
		for (i=1; i<line.getNumPoints(); i++) {
			Coordinate currentPoint = line.getCoordinateN(i);
			cumulatedDistance += currentPoint.distance(previousPoint);
			previousPoint = currentPoint;
			if (cumulatedDistance >= randomDistance)
				break;
		}
		
		Coordinate from = line.getCoordinateN(i-1);
		Coordinate to = line.getCoordinateN(i);
		double prop = uniform.nextDoubleFromTo(0.0, 1.0);
		
		Coordinate c = new Coordinate(
				from.getX() + prop * (to.getX() - from.getX()),
				from.getY() + prop * (to.getY() - from.getY())
				);
		geometryFactory.getPrecisionModel().makePrecise(c);
		
		return geometryFactory.createPoint(c);
	}
			
	protected Point getRandomPointOnGeometry(MultiPolygon multipoly) {
		
		// first select randomly one polygon proportionaly to its surface
		final double totalArea = multipoly.getArea();
		final double roulette = uniform.nextDoubleFromTo(0.0, totalArea);
		
		int i;
		double cumulatedArea = 0.0;
		for (i=0; i<multipoly.getNumGeometries(); i++) {
			cumulatedArea += multipoly.getGeometryN(i).getArea();
			if (cumulatedArea >= roulette)
				break;
		}
		
		Polygon polygon = (Polygon) multipoly.getGeometryN(i);
		return getRandomPointOnGeometry(polygon);
		
	}
	
	protected Point getRandomPointOnGeometry(MultiPoint multipoint) {

		// select which point we will return
		int idx = uniform.nextIntFromTo(0, multipoint.getNumGeometries());
		
		return (Point)multipoint.getGeometryN(idx);
		
	}
	
	protected Point getRandomPointOnGeometry(MultiLineString multiline) {
		
		// first select randomly one line proportionaly to its surface

		final double totalLength = multiline.getLength();
		final double roulette = uniform.nextDoubleFromTo(0.0, totalLength);
		
		int i;
		double cumulatedLength = 0.0;
		for (i=0; i<multiline.getNumGeometries(); i++) {
			cumulatedLength += multiline.getGeometryN(i).getLength();
			if (cumulatedLength >= roulette)
				break;
		}
		
		LineString line = (LineString) multiline.getGeometryN(i);
		return getRandomPointOnGeometry(line);
	}
	
	protected Point getRandomPointOnGeometry(Geometry geom) {
		
		if (geom instanceof MultiPolygon)
			return getRandomPointOnGeometry((MultiPolygon)geom);
		else if (geom instanceof Polygon)
			return getRandomPointOnGeometry((Polygon)geom);
		else if (geom instanceof MultiLineString)
			return getRandomPointOnGeometry((MultiLineString)geom);
		else if (geom instanceof LineString)
			return getRandomPointOnLine((LineString)geom);
		else if (geom instanceof MultiPoint)
			return getRandomPointOnGeometry((MultiPoint)geom);
		else if (geom instanceof Point)
			return getRandomPointOnGeometry((Point)geom);
		else 
			throw new RuntimeException("unable to compute unknown geometry type "+geom);
	}
	
	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		
		BufferedDataTable inputTable = inData[0];

		final boolean autoseed = m_autoseed.getBooleanValue();
		final int providedSeed = m_seed.getIntValue();
		final int count = m_count.getIntValue();
		
		DataTableSpec outputSpec = inputTable.getDataTableSpec();
		BufferedDataContainer container = exec.createDataContainer(outputSpec);

		//crs = SpatialUtils.decodeCRS(inputTable.getDataTableSpec());
		
		// init random
		int seed ;
		if (autoseed) {
    		seed = (int)(new Date()).getTime();
    	} else {
    		seed = providedSeed;
    	}
    	this.random = new MersenneTwister(seed);
    	this.uniform = new Uniform(random);

		final double total = inputTable.size() * count;
				
		final int numberOfCells = inputTable.getDataTableSpec().getNumColumns();

		//CoordinateReferenceSystem crsOrig = SpatialUtils.decodeCRS(inputTable.getDataTableSpec());

		//CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory("AUTO", null);

		final int idxGeomCol = inputTable.getDataTableSpec().findColumnIndex(SpatialUtils.GEOMETRY_COLUMN_NAME);
		
		WKTWriter writer = new WKTWriter();

		// iterate each geometry of each row
		done = 0;
		final MissingCell missing = new MissingCell("empty geometry");
		SpatialUtils.applyToEachGeometry(
				inputTable, 
				geomAndRow -> {
		     	
					for (int j=0; j<count; j++) {
					    // create the row
						DataCell[] cells = new DataCell[numberOfCells];
						for (int i=0; i<numberOfCells; i++) {
							if (i == idxGeomCol) {
								if (geomAndRow.geometry.isEmpty()) {
									// cannot convert it
									cells[i] = missing;
								} else 
									// convert geometry
									cells[i] = StringCellFactory.create(writer.write(getRandomPointOnGeometry(geomAndRow.geometry)));
							} else 
								cells[i] = geomAndRow.row.getCell(i);
							
						}
									
						RowKey key;
						if (j == 0)
							key = geomAndRow.row.getKey();
						else 
							key = new RowKey(geomAndRow.row.getKey().getString()+"_"+j);
						
						DataRow row = new DefaultRow(
								key, 
				    		  cells
				    		  );
						container.addRowToTable(row);
						
						exec.checkCanceled();
						exec.setProgress(done/total, "creating points for row "+done);
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

		m_autoseed.saveSettingsTo(settings);
		m_seed.saveSettingsTo(settings);
		m_count.saveSettingsTo(settings);
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_autoseed.loadSettingsFrom(settings);
		m_seed.loadSettingsFrom(settings);
		m_count.loadSettingsFrom(settings);
			
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_autoseed.validateSettings(settings);
		m_seed.validateSettings(settings);
		m_count.validateSettings(settings);
		
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

