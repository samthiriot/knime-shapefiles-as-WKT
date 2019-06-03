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

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This is an example implementation of the node factory of the
 * "ComputeCentroidForWKTGeometries" node.
 *
 * @author Samuel Thiriot
 */
public class Coordinates2DOfWKTGeometriesNodeFactory 
        extends NodeFactory<Coordinates2DOfWKTGeometriesNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Coordinates2DOfWKTGeometriesNodeModel createNodeModel() {
		// Create and return a new node model.
        return new Coordinates2DOfWKTGeometriesNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
		// The number of views the node should have, in this cases there is none.
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<Coordinates2DOfWKTGeometriesNodeModel> createNodeView(final int viewIndex,
            final Coordinates2DOfWKTGeometriesNodeModel nodeModel) {
		// We return null as this example node does not provide a view. Also see "getNrNodeViews()".
		return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
		// Indication whether the node has a dialog or not.
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
		// This example node has a dialog, hence we create and return it here. Also see "hasDialog()".
        return new Coordinates2DOfWKTGeometriesNodeDialog();
    }

}

