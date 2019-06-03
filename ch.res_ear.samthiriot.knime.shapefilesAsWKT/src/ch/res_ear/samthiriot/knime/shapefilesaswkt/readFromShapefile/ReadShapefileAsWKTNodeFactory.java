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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.readFromShapefile;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ReadShapefileAsKML" Node.
 * Reads spatial features (geometries) from a <a href="https://en.wikipedia.org/wiki/Shapefile">shapefile</a>. Accepts any geometry type: points, lines, or polygons.  * n * nActual computation relies on the <a href="https://geotools.org/">geotools library</a>.
 *
 * @author EIFER
 */
public class ReadShapefileAsWKTNodeFactory 
        extends NodeFactory<ReadShapefileAsWKTNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadShapefileAsWKTNodeModel createNodeModel() {
        return new ReadShapefileAsWKTNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<ReadShapefileAsWKTNodeModel> createNodeView(final int viewIndex,
            final ReadShapefileAsWKTNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new ReadShapefileAsWKTNodeDialog();
    }

}

