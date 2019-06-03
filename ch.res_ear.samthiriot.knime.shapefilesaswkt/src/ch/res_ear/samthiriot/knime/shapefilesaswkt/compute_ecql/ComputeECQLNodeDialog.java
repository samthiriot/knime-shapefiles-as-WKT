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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.compute_ecql;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * <code>NodeDialog</code> for the "Reproject" Node.
 * Reprojects a spatialized population
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Samuel Thiriot
 */
public class ComputeECQLNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring Reproject node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected ComputeECQLNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentMultiLineString(
        		new SettingsModelString(
        				"query",
        				"area(the_geom)"
        				),
        		"query",
        		true,
        		50,
        		5
        		));
        
        SettingsModelString m_type = new SettingsModelString(
										"type",
										"Double"
										);

        SettingsModelString m_colname = new SettingsModelString(
				"colname",
				"area"
				);
        
        m_type.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				m_colname.setEnabled(!m_type.getStringValue().equals("Geometry"));
			}
		});
        
        addDialogComponent(
        		new DialogComponentStringSelection(
    				m_type,
	        		"type", 
					"String",
					"Double",
					"Integer",
					"Long",
					"Boolean",
					"Geometry"
					)
        		);
        

        
        addDialogComponent(new DialogComponentString(
        		m_colname,
        		"column name",
        		true,
        		30
        		));
    }
}

