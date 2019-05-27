package ch.res_ear.samthiriot.knime.shapefilesAsWKT.readFromGML;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This is an example implementation of the node factory of the
 * "ReadKMLAsWKT" node.
 *
 * @author Samuel Thiriot
 */
public class ReadGMLAsWKTNodeFactory 
        extends NodeFactory<ReadGMLAsWKTNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadGMLAsWKTNodeModel createNodeModel() {
        return new ReadGMLAsWKTNodeModel();
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
    public NodeView<ReadGMLAsWKTNodeModel> createNodeView(final int viewIndex,
            final ReadGMLAsWKTNodeModel nodeModel) {
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
        return new ReadGMLAsWKTNodeDialog();
    }

}

