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
package ch.res_ear.samthiriot.knime.shapefilesaswkt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.geotools.swing.dialog.JCRSChooser;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A dialog component to choose the Component Reference System
 * 
 * @author Samuel Thiriot
 */
public class DialogComponentReferenceSystem extends DialogComponent {

    private final JTextField m_textField;
    private final JLabel m_label;
    private final JButton m_choose;
    
	public DialogComponentReferenceSystem(
			final SettingsModelString stringModel,
			final String label) {
		super(stringModel);
		
        m_label = new JLabel(label);
        getComponentPanel().add(m_label);
        
        m_textField = new JTextField(stringModel.getStringValue());
        m_textField.setEditable(false);
        getComponentPanel().add(m_textField);
        
        m_choose = new JButton("choose");
        m_choose.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				CoordinateReferenceSystem crs = null;
				try {
					crs = JCRSChooser.showDialog(
						"Coordinate Reference System", 
						m_textField.getText());
				} catch (NullPointerException e1) {
					e1.printStackTrace();
					crs = JCRSChooser.showDialog(
							"Coordinate Reference System");
				}
			    m_textField.setText(SpatialUtils.getStringForCRS(crs));
			    stringModel.setStringValue(SpatialUtils.getStringForCRS(crs));
			}
		});
        getComponentPanel().add(m_choose);
	}
	
	
	@Override
	protected void updateComponent() {


	}

	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {


	}

	@Override
	protected void checkConfigurabilityBeforeLoad(PortObjectSpec[] specs) throws NotConfigurableException {


	}

	@Override
	protected void setEnabledComponents(boolean enabled) {


	}

	@Override
	public void setToolTipText(String text) {


	}

}
