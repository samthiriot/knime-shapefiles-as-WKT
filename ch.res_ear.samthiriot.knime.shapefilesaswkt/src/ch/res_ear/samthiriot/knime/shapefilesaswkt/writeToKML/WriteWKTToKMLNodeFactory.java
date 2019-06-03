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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.writeToKML;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This is an example implementation of the node factory of the
 * "WriteWKTToKML" node.
 *
 * @author Samuel Thiriot
 */
public class WriteWKTToKMLNodeFactory 
        extends NodeFactory<WriteWKTToKMLNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteWKTToKMLNodeModel createNodeModel() {
		// Create and return a new node model.
        return new WriteWKTToKMLNodeModel();
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
    public NodeView<WriteWKTToKMLNodeModel> createNodeView(final int viewIndex,
            final WriteWKTToKMLNodeModel nodeModel) {
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
		// This example node has a dialog, hence we create and return it here. Also see "hasDialog()".
        return new WriteWKTToKMLNodeDialog();
    }

}

