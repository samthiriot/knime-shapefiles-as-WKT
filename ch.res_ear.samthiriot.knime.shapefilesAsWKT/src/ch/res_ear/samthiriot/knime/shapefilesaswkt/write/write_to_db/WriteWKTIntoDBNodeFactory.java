package ch.res_ear.samthiriot.knime.shapefilesaswkt.write.write_to_db;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "WriteWKTAsShapefile" Node.
 * Stores the WKT data as a shapefile.
 *
 * @author Samuel Thiriot
 */
public class WriteWKTIntoDBNodeFactory 
        extends NodeFactory<WriteWKTIntoDBNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteWKTIntoDBNodeModel createNodeModel() {
        return new WriteWKTIntoDBNodeModel();
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
    public NodeView<WriteWKTIntoDBNodeModel> createNodeView(final int viewIndex,
            final WriteWKTIntoDBNodeModel nodeModel) {
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
        return new WriteWKTIntoDBNodeDialog();
    }

}

