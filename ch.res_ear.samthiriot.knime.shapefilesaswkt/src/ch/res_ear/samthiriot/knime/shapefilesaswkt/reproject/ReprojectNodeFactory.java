package ch.res_ear.samthiriot.knime.shapefilesaswkt.reproject;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Reproject" Node.
 * Reprojects a spatialized population
 *
 * @author Samuel Thiriot
 */
public class ReprojectNodeFactory 
        extends NodeFactory<ReprojectNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ReprojectNodeModel createNodeModel() {
        return new ReprojectNodeModel();
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
    public NodeView<ReprojectNodeModel> createNodeView(final int viewIndex,
            final ReprojectNodeModel nodeModel) {
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
        return new ReprojectNodeDialog();
    }

}

