package ch.res_ear.samthiriot.knime.shapefilesAsWKT.view;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapPane;
import org.geotools.swing.tool.ScrollWheelTool;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "DisplaySpatialPopulation" Node.
 * View the spatial population on a map.
 *
 * @author Samuel Thiriot
 */
public class DisplaySpatialPopulationNodeView extends NodeView<DisplaySpatialPopulationNodeModel> {
	
    private static final NodeLogger logger = NodeLogger
            .getLogger(DisplaySpatialPopulationNodeView.class);
        
	private MapContent content = null;
	private JMapPane mapPane = null;


    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link DisplaySpatialPopulationNodeModel})
     */
    protected DisplaySpatialPopulationNodeView(final DisplaySpatialPopulationNodeModel nodeModel) {
        super(nodeModel);

        content = new MapContent();

        GTRenderer renderer = new StreamingRenderer();
	    mapPane = new JMapPane(content);
	    mapPane.setRenderer(renderer);
	    mapPane.addMouseListener(new ScrollWheelTool(mapPane));
	    //mapPane.addMouseListener(new PanTool());
	    
	    /*
	    try {
		    PanTool panTool = new PanTool();
		    panTool.setMapPane(mapPane);
	    } catch (RuntimeException | NoClassDefFoundError e) {
	    	e.printStackTrace();
	    	logger.warn("unable to use panning: "+e.getMessage());
	    }
	    */
	    
	    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    
        mapPane.setPreferredSize(new Dimension(screenSize.width*2/3, screenSize.height*2/3));
       
        setComponent(mapPane);
        
 	   	
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        // update the view.
        DisplaySpatialPopulationNodeModel nodeModel = 
            (DisplaySpatialPopulationNodeModel)getNodeModel();
        assert nodeModel != null;
        
        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.
        if (nodeModel.datastore1 == null)
        	return;
        
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    
    	content.dispose();
    	

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

        DisplaySpatialPopulationNodeModel nodeModel = 
                (DisplaySpatialPopulationNodeModel)getNodeModel();
        
        if (nodeModel == null || nodeModel.datastore1 == null) {
        	System.err.println("No node model");
        	return;
        }
        
        try {
        	
        	//System.out.println("layers:" + nodeModel.datastore1.getNames());
        	
        	// add layer 1
    		SimpleFeatureSource shapefileSource = nodeModel.datastore1.getFeatureSource(
    				nodeModel.datastore1.getNames().get(0));
    	    Style shpStyle = SLD.createPolygonStyle(nodeModel.m_color1.getColorValue(), nodeModel.m_color1.getColorValue(), 0.0f);
    	    Layer shpLayer = new FeatureLayer(shapefileSource, shpStyle);       
            content.addLayer(shpLayer);
            mapPane.setDisplayArea(content.getMaxBounds());

        	//System.out.println("added layer 1");
        	
            // add layer 2
            if (nodeModel.datastore2 != null) {
            	System.out.println("display data source 2 "+nodeModel.datastore2.getNames());
            	SimpleFeatureSource shapefileSource2 = nodeModel.datastore2.getFeatureSource(
        				nodeModel.datastore2.getNames().get(0));
        	    Style shpStyle2 = SLD.createPolygonStyle(nodeModel.m_color2.getColorValue(), null, 0.0f);
        	    Layer shpLayer2 = new FeatureLayer(shapefileSource2, shpStyle2);       
                content.addLayer(shpLayer2);

            }
          

        } catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("error when displaying the map: "+e.getMessage(), e);
		}    
    }

}

