/*******************************************************************************
 * Copyright (c) 2019 EIfER[1] (European Institute for Energy Research).
 * This program and the accompanying materials
 * are made available under the terms of the GNU GENERAL PUBLIC LICENSE
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Contributors:
 *     Samuel Thiriot - original version and contributions
 *******************************************************************************/
package ch.res_ear.samthiriot.knime.shapefilesaswkt.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wms.WMSCapabilities;
import org.geotools.ows.wms.WMSUtils;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.ows.wms.map.WMSLayer;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
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
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.filter.FilterFactory;

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

	private ReferencedEnvelope envelope = null;
	//private WMSLayer overlay = null;
	
	private List<WMSLayer> overlays = null;
	
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
	        	MapAction a = //new ResetAction(mapPane);
	        			new MapAction() {
							/**
							 * 
							 */
							private static final long serialVersionUID = 1L;

							@Override
							public void actionPerformed(ActionEvent e) {
								if (envelope != null)
									mapPane.setDisplayArea(envelope);					
							}
						};
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
	       
	        // add buttons for all the overlay layers
            try {
       	
                String url = nodeModel.m_urlWMS.getStringValue();
               
                WebMapServer wms = new WebMapServer(new URL(url));

            	WMSCapabilities capabilities = wms.getCapabilities();

            	// gets all the layers in a flat list, in the order they appear in
            	// the capabilities document (so the rootLayer is at index 0)
            	//List<org.geotools.ows.wms.Layer> layers = capabilities.getLayerList();
            	org.geotools.ows.wms.Layer[] layers = WMSUtils.getNamedLayers(capabilities);
    	        
            	menu.addSeparator();

            	overlays = new LinkedList<>();
    		    ButtonGroup groupLayers = new ButtonGroup();
    		    
        		JRadioButtonMenuItem overlayNone = new JRadioButtonMenuItem("no overlay");
        		overlayNone.setSelected(false);
        		overlayNone.addActionListener(new MapAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						for (WMSLayer l: overlays)
							l.setVisible(false);
					}
				});
	        	groupLayers.add(overlayNone);
	        	menu.add(overlayNone);
	        	
    		    int i=0;
    		    for (org.geotools.ows.wms.Layer layer: layers) {
            		WMSLayer l = new WMSLayer(wms, layer);
            		l.setVisible(i == 0);
            		content.addLayer(l);
            		overlays.add(l);
            		
            		JRadioButtonMenuItem overlayMenu = new JRadioButtonMenuItem("layer "+layer.getName());
            		overlayMenu.setSelected(i==0);
            		final int index = i++;
    	        	overlayMenu.addActionListener(new MapAction() {
    					@Override
    					public void actionPerformed(ActionEvent e) {
    						for (WMSLayer l: overlays)
    							l.setVisible(false);
    						overlays.get(index).setVisible(true);
    					}
    				});
    	        	groupLayers.add(overlayMenu);
    	        	menu.add(overlayMenu);
            	}
            
            } catch (ServiceException | IOException e) {
                e.printStackTrace();
                logger.warn("unable to load overlay: "+e.getMessage());
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
    
    	try {
    		for (Layer l: content.layers()) {
    			l.dispose();
    		}
    		content.dispose();
    	} catch (RuntimeException e) {
    		logger.warn("error when disposing the view: "+e.getMessage(), e);
    	}
    }
    
    protected Symbolizer createDefaultSymbolizer(SimpleFeatureSource shapefileSource, Fill fill, Stroke stroke, double opacity) {
    	
        StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
		StyleBuilder sb = new StyleBuilder();

    	GeometryType geomType = shapefileSource.getSchema().getGeometryDescriptor().getType(); 
        Symbolizer sym = null;
        if (Point.class.isAssignableFrom(geomType.getBinding()) || MultiPoint.class.isAssignableFrom(geomType.getBinding())) { 
        //if (geomType.equals(SimpleSchema.POINT) || geomType.equals(SimpleSchema.MULTIPOINT)) {
        	Mark circle = sb.createMark(StyleBuilder.MARK_CIRCLE, fill, stroke);
        	Graphic graph = sb.createGraphic(null, circle, null, 1.0, 5.0, 0); // opacity
        	sym = sf.createPointSymbolizer(graph, null);
        } else if (Polygon.class.isAssignableFrom(geomType.getBinding()) || MultiPolygon.class.isAssignableFrom(geomType.getBinding()) )
        	sym = sf.createPolygonSymbolizer(stroke, fill, null);
        else 
        	sym = sf.createLineSymbolizer(stroke, null);
        
        return sym;
    }
    
    protected Style createStyleForStore(SimpleFeatureSource shapefileSource, Color color, double opacity) {
    	
		Style shpStyle = null;
		AttributeDescriptor desc = shapefileSource.getSchema().getDescriptor("color");
		if (desc != null) {
			
	        
			// a color is defined; use it 
    		StyleBuilder sb = new StyleBuilder();
            FilterFactory ff = sb.getFilterFactory();
            Style style = sb.createStyle();
            style.setName("MyStyle");

            StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
            
            Stroke stroke = sf.createStroke(sb.attributeExpression("color"), ff.literal(1));
            Fill fill = sf.createFill(sb.attributeExpression("color"), ff.literal(opacity));
            Symbolizer sym = createDefaultSymbolizer(shapefileSource, fill, stroke, opacity);

	        FeatureTypeStyle featureTypeStyle = sf.createFeatureTypeStyle();

            // add a rule which, when the color attribute is not null, uses this color to display the geometry
	        Rule rule1 = sf.createRule();
	        rule1.setName("rule1");
	        rule1.getDescription().setTitle("City");
	        rule1.getDescription().setAbstract("Rule for drawing cities");
	        rule1.setFilter(ff.not(ff.isNull(ff.property("color"))));
            
            rule1.symbolizers().add(sym);

            featureTypeStyle.rules().add(rule1);
            
            
            //shpStyle = SLD.wrapSymbolizers(sym);
            List<org.opengis.style.Symbolizer> symbolizers = new ArrayList<>();
            Stroke strokeDefault = sf.createStroke(sb.colorExpression(color), ff.literal(1));
            Fill fillDefault = sf.createFill(sb.colorExpression(color), ff.literal(opacity));
            Symbolizer sym2 = createDefaultSymbolizer(shapefileSource, fillDefault, strokeDefault, opacity);
            symbolizers.add(sym2);
           
            Rule rule2 = sf.rule(
                            "default",
                            null,
                            null,
                            Double.MIN_VALUE,
                            Double.MAX_VALUE,
                            symbolizers,
                            null
                            );
            featureTypeStyle.rules().add(rule2);
            
            style.featureTypeStyles().add(featureTypeStyle);
            
            shpStyle = style;
		} else {
			// create a default style
        /*
        Style shpStyle = SLD.createSimpleStyle(
        		shapefileSource.getSchema(), 
        		
        		sb.createFill(sb.attributeExpression("color"),sb.attributeExpression("opacity"))
        		);
        		*/
			shpStyle = SLD.createSimpleStyle(shapefileSource.getSchema(), color);
		}
		
		return shpStyle;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

    	
        DisplaySpatialPopulationNodeModel nodeModel = 
                (DisplaySpatialPopulationNodeModel)getNodeModel();

        final double opacity1 = nodeModel.m_opacity1.getDoubleValue();
        final double opacity2 = nodeModel.m_opacity1.getDoubleValue();

        if (nodeModel == null || nodeModel.datastore1 == null) {
        	logger.warn("nothing to display");
        	return;
        }

        try {
        	
        	        	
        	// add layer 1
    		SimpleFeatureSource shapefileSource = nodeModel.datastore1.getFeatureSource(
    				nodeModel.datastore1.getNames().get(0));
    		
    		envelope = shapefileSource.getBounds();

            
    		Style shpStyle = createStyleForStore(shapefileSource, nodeModel.m_color1.getColorValue(), opacity1);
            
    	    Layer shpLayer = new FeatureLayer(shapefileSource, shpStyle);       
            content.addLayer(shpLayer);
            mapPane.setDisplayArea(content.getMaxBounds());
        	
            
            // add layer 2
            if (nodeModel.datastore2 != null) {
            	SimpleFeatureSource shapefileSource2 = nodeModel.datastore2.getFeatureSource(
        				nodeModel.datastore2.getNames().get(0));
            	
            	envelope.expandToInclude(shapefileSource2.getBounds());
            	
        		Style shpStyle2 = createStyleForStore(shapefileSource2, nodeModel.m_color2.getColorValue(), opacity2);

        	    Layer shpLayer2 = new FeatureLayer(shapefileSource2, shpStyle2);       
                content.addLayer(shpLayer2);

            }
          
			mapPane.setDisplayArea(envelope);					

        } catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("error when displaying the map: "+e.getMessage(), e);
		}    
    }

}

