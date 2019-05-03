package ch.res_ear.samthiriot.knime.shapefilesAsWKT.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.swing.JMapPane;
import org.geotools.swing.action.InfoAction;
import org.geotools.swing.action.MapAction;
import org.geotools.swing.action.NoToolAction;
import org.geotools.swing.action.PanAction;
import org.geotools.swing.action.ResetAction;
import org.geotools.swing.action.ZoomInAction;
import org.geotools.swing.action.ZoomOutAction;
import org.geotools.swing.tool.InfoTool;
import org.geotools.swing.tool.PanTool;
import org.geotools.swing.tool.ScrollWheelTool;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.opengis.filter.FilterFactory2;

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
	   
	    // prefered dimension is large but not the entire screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        mapPane.setPreferredSize(new Dimension(screenSize.width*2/3, screenSize.height*2/3));
        
	    setComponent(mapPane);
	    
	    // add a menu with tools to zoom, pan, etc.
	    try {
		    JMenuBar menuBar = getJMenuBar();
		    
		    ButtonGroup group = new ButtonGroup();

	        JMenu menu = new JMenu("Spatial tools");
	        menu.setMnemonic('S');
	        
	        {
	        	JMenuItem resetMenu = new JMenuItem("Zoom to fit");
	        	MapAction a = new ResetAction(mapPane);
	        	resetMenu.setIcon((Icon) a.getValue(ResetAction.SMALL_ICON));
	        	resetMenu.addActionListener(a);
	        	menu.add(resetMenu);
	        }
	        menu.addSeparator();

	        {
	        	JRadioButtonMenuItem noToolMenu = new JRadioButtonMenuItem(NoToolAction.TOOL_NAME);
	        	MapAction a = new NoToolAction(mapPane);
	        	group.add(noToolMenu);
	        	noToolMenu.setSelected(true);
	        	noToolMenu.setIcon((Icon) a.getValue(NoToolAction.SMALL_ICON));
	        	noToolMenu.addActionListener(a);
	        	menu.add(noToolMenu);
	        }
	        {
	        	JRadioButtonMenuItem panMenu = new JRadioButtonMenuItem(PanTool.TOOL_NAME);
	        	group.add(panMenu);
	        	try {
	        		panMenu.setIcon(new ImageIcon(ImageIO.read(getClass().getResourceAsStream(PanTool.ICON_IMAGE))));
				} catch (IOException e) {
					logger.error("unable to load image resource "+PanTool.ICON_IMAGE, e);
					e.printStackTrace();
				}
	        	panMenu.setMnemonic('P');
	        	panMenu.addActionListener(new PanAction(mapPane));
	        	menu.add(panMenu);
	        }
	        {
	        	JRadioButtonMenuItem infoMenu = new JRadioButtonMenuItem(InfoTool.TOOL_NAME);
	        	group.add(infoMenu);
	        	try {
	        		infoMenu.setIcon(new ImageIcon(ImageIO.read(getClass().getResourceAsStream(InfoTool.ICON_IMAGE))));
				} catch (IOException e) {
					logger.error("unable to load image resource "+InfoTool.ICON_IMAGE, e);
					e.printStackTrace();
				}
	        	infoMenu.addActionListener(new InfoAction(mapPane));
	        	menu.add(infoMenu);
	        }
	        {
	        	JRadioButtonMenuItem zoomInMenu = new JRadioButtonMenuItem("Zoom in");
	        	group.add(zoomInMenu);
	        	MapAction a = new ZoomInAction(mapPane);
	        	zoomInMenu.setMnemonic('+');
	        	zoomInMenu.setIcon((Icon) a.getValue(ZoomInAction.SMALL_ICON));
				zoomInMenu.addActionListener(a);
	        	menu.add(zoomInMenu);
	        }
	        {
	        	JRadioButtonMenuItem zoomOutMenu = new JRadioButtonMenuItem("Zoom out");
	        	group.add(zoomOutMenu);
	        	zoomOutMenu.setMnemonic('-');
	        	MapAction a = new ZoomInAction(mapPane);
	        	zoomOutMenu.setIcon((Icon) a.getValue(ZoomOutAction.SMALL_ICON));
	        	zoomOutMenu.addActionListener(a);
	        	menu.add(zoomOutMenu);
	        }
	       
		    menuBar.add(menu);
		    
	    } catch (NoClassDefFoundError e) {
	    	e.printStackTrace();
	    	logger.warn("unable to display toolbars");
	    }
	    
    }
    
    protected void uncheckAll() {
    	
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
    		
    		Style shpStyle = SLD.createSimpleStyle(shapefileSource.getSchema(), nodeModel.m_color1.getColorValue());
    		
    	    Layer shpLayer = new FeatureLayer(shapefileSource, shpStyle);       
            content.addLayer(shpLayer);
            mapPane.setDisplayArea(content.getMaxBounds());

        	//System.out.println("added layer 1");
        	
            // add layer 2
            if (nodeModel.datastore2 != null) {
            	System.out.println("display data source 2 "+nodeModel.datastore2.getNames());
            	SimpleFeatureSource shapefileSource2 = nodeModel.datastore2.getFeatureSource(
        				nodeModel.datastore2.getNames().get(0));
        	    //Style shpStyle2 = SLD.createPolygonStyle(nodeModel.m_color2.getColorValue(), null, 0.0f);
        		//Style shpStyle2 = SLD.createSimpleStyle(nodeModel.datastore2, geometryType, nodeModel.m_color2.getColorValue());
        		Style shpStyle2 = SLD.createSimpleStyle(shapefileSource2.getSchema(), nodeModel.m_color2.getColorValue());

        	    Layer shpLayer2 = new FeatureLayer(shapefileSource2, shpStyle2);       
                content.addLayer(shpLayer2);

            }
          

        } catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("error when displaying the map: "+e.getMessage(), e);
		}    
    }

}

