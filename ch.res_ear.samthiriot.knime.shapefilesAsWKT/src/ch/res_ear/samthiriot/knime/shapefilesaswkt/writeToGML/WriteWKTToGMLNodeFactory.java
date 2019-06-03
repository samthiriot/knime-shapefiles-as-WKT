package ch.res_ear.samthiriot.knime.shapefilesaswkt.writeToGML;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This is an example implementation of the node factory of the
 * "WriteWKTToKML" node.
 *
 * @author Samuel Thiriot
 */
public class WriteWKTToGMLNodeFactory 
        extends NodeFactory<WriteWKTToGMLNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteWKTToGMLNodeModel createNodeModel() {
		// Create and return a new node model.
        return new WriteWKTToGMLNodeModel();
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
    public NodeView<WriteWKTToGMLNodeModel> createNodeView(final int viewIndex,
            final WriteWKTToGMLNodeModel nodeModel) {
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
        return new WriteWKTToGMLNodeDialog();
    }

}

