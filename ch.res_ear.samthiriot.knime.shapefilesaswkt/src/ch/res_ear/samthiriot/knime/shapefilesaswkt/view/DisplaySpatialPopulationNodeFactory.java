package ch.res_ear.samthiriot.knime.shapefilesaswkt.view;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "DisplaySpatialPopulation" Node.
 * View the spatial population on a map.
 *
 * @author Samuel Thiriot
 */
public class DisplaySpatialPopulationNodeFactory 
        extends NodeFactory<DisplaySpatialPopulationNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public DisplaySpatialPopulationNodeModel createNodeModel() {
        return new DisplaySpatialPopulationNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<DisplaySpatialPopulationNodeModel> createNodeView(final int viewIndex,
            final DisplaySpatialPopulationNodeModel nodeModel) {
        return new DisplaySpatialPopulationNodeView(nodeModel);
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
        return new DisplaySpatialPopulationNodeDialog();
    }

}

