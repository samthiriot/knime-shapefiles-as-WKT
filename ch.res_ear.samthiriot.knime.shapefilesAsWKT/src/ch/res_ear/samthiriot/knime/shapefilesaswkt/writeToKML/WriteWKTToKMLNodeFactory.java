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

