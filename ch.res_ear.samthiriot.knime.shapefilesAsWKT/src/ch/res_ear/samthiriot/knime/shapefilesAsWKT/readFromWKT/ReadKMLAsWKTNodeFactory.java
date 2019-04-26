package ch.res_ear.samthiriot.knime.shapefilesAsWKT.readFromWKT;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This is an example implementation of the node factory of the
 * "ReadKMLAsWKT" node.
 *
 * @author Samuel Thiriot
 */
public class ReadKMLAsWKTNodeFactory 
        extends NodeFactory<ReadKMLAsWKTNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadKMLAsWKTNodeModel createNodeModel() {
        return new ReadKMLAsWKTNodeModel();
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
    public NodeView<ReadKMLAsWKTNodeModel> createNodeView(final int viewIndex,
            final ReadKMLAsWKTNodeModel nodeModel) {
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
        return new ReadKMLAsWKTNodeDialog();
    }

}

