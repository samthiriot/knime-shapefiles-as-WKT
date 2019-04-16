package ch.res_ear.samthiriot.knime.shapefilesAsWKT.writeToShapefile;

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

