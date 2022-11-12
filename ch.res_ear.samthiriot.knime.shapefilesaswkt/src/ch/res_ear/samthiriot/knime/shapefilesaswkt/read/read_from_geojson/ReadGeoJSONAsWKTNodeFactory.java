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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.read.read_from_geojson;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This is an example implementation of the node factory of the
 * "ReadKMLAsWKT" node.
 *
 * @author Samuel Thiriot
 */
public class ReadGeoJSONAsWKTNodeFactory 
        extends NodeFactory<ReadGeoJSONAsWKTNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadGeoJSONAsWKTNodeModel createNodeModel() {
        return new ReadGeoJSONAsWKTNodeModel();
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
    public NodeView<ReadGeoJSONAsWKTNodeModel> createNodeView(final int viewIndex,
            final ReadGeoJSONAsWKTNodeModel nodeModel) {
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
        return new ReadGeoJSONAsWKTNodeDialog();
    }

}

