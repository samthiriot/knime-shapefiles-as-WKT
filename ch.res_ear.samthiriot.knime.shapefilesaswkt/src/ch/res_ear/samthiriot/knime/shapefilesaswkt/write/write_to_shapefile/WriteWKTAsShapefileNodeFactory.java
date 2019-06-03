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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.write.write_to_shapefile;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "WriteWKTAsShapefile" Node.
 * Stores the WKT data as a shapefile.
 *
 * @author Samuel Thiriot
 */
public class WriteWKTAsShapefileNodeFactory 
        extends NodeFactory<WriteWKTAsShapefileNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteWKTAsShapefileNodeModel createNodeModel() {
        return new WriteWKTAsShapefileNodeModel();
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
    public NodeView<WriteWKTAsShapefileNodeModel> createNodeView(final int viewIndex,
            final WriteWKTAsShapefileNodeModel nodeModel) {
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
        return new WriteWKTAsShapefileNodeDialog();
    }

}

