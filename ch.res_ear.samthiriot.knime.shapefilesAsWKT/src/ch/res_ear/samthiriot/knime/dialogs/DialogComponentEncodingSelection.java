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
package ch.res_ear.samthiriot.knime.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.DefaultStringIconOption;
import org.knime.core.node.util.StringIconListCellRenderer;
import org.knime.core.node.util.StringIconOption;

/**
 * A dialog component which offers to choose one text encoding 
 * based on the ones available on the system.
 *  
 * A large part of the code is taken from DialogComponentStringSelection which we would
 * have overloaded if it was not final.
 * 
 * @author Samuel Thiriot
 *
 */
public class DialogComponentEncodingSelection extends DialogComponent {
	
	public static final String CHOICE_AUTOMATIC = "<automatic>";

    private final JComboBox m_combobox;

    private final JLabel m_label;

    
	public DialogComponentEncodingSelection(
				final SettingsModelString stringModel,
				boolean automaticEnabled) {
		super(stringModel);
		
        m_label = new JLabel("encoding");
        getComponentPanel().add(m_label);
        m_combobox = new JComboBox();
        m_combobox.setRenderer(new StringIconListCellRenderer());

        //StringIconOption[] options;
        // create the list of options
        List<StringIconOption> options = new ArrayList<>();
        int currentIdx = 0;
		if (automaticEnabled) {
			options.add(new DefaultStringIconOption(CHOICE_AUTOMATIC)); // TODO icon for default?
			currentIdx++;
		}
		int defaultCharsetIdx = 0;
	    SortedMap<String,Charset> charsets = Charset.availableCharsets();
	    final String defaultCharset = Charset.defaultCharset().name();
	    for (String l: charsets.keySet()) {
			options.add(new DefaultStringIconOption(l));
			if (defaultCharset.equals(l)) {
				defaultCharsetIdx = currentIdx;
			}
			currentIdx++;
	    }
	    
	    // TODO define current charset
	    
	    // create the component to display the options
	    
        for (final StringIconOption o : options) {
            m_combobox.addItem(o);
        }

        m_combobox.setEditable(false);
        m_combobox.setSelectedIndex(defaultCharsetIdx);
        
        getComponentPanel().add(m_combobox);

        m_combobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // if a new item is selected update the model
                    try {
                        updateModel();
                    } catch (final InvalidSettingsException ise) {
                        // ignore it here
                    }
                }
            }
        });

        // we need to update the selection, when the model changes.
        getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });
	    

        //call this method to be in sync with the settings model
        updateComponent();
    }

	@Override
	protected void updateComponent() {
		final String strVal =
	            ((SettingsModelString)getModel()).getStringValue();
        StringIconOption val = null;
        if (strVal == null) {
            val = null;
        } else {
            for (int i = 0, length = m_combobox.getItemCount();
                i < length; i++) {
                final StringIconOption curVal =
                    (StringIconOption)m_combobox.getItemAt(i);
                if (curVal.getText().equals(strVal)) {
                    val = curVal;
                    break;
                }
            }
            if (val == null) {
                val = new DefaultStringIconOption(strVal);
            }
        }
        boolean update;
        if (val == null) {
            update = m_combobox.getSelectedItem() != null;
        } else {
            update = !val.equals(m_combobox.getSelectedItem());
        }
        if (update) {
            m_combobox.setSelectedItem(val);
        }
        // also update the enable status
        setEnabledComponents(getModel().isEnabled());

        // make sure the model is in sync (in case model value isn't selected)
        StringIconOption selItem =
            (StringIconOption)m_combobox.getSelectedItem();
        try {
            if ((selItem == null && strVal != null)
                    || (selItem != null && !selItem.getText().equals(strVal))) {
                // if the (initial) value in the model is not in the list
                updateModel();
            }
        } catch (InvalidSettingsException e) {
            // ignore invalid values here
        }
	}
	
	/**
     * Transfers the current value from the component into the model.
     */
    private void updateModel() throws InvalidSettingsException {

        if (m_combobox.getSelectedItem() == null) {
            ((SettingsModelString)getModel()).setStringValue(null);
            m_combobox.setBackground(Color.RED);
            // put the color back to normal with the next selection.
            m_combobox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    m_combobox.setBackground(DialogComponent.DEFAULT_BG);
                }
            });
            throw new InvalidSettingsException(
                    "Please select an item from the list.");
        }
        // we transfer the value from the field into the model
        ((SettingsModelString)getModel()).setStringValue(
                ((StringIconOption)m_combobox.getSelectedItem()).getText());
    }


	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        updateModel();

	}

	@Override
	protected void checkConfigurabilityBeforeLoad(PortObjectSpec[] specs) throws NotConfigurableException {
        // we are always good.

	}

	@Override
	protected void setEnabledComponents(boolean enabled) {
        m_combobox.setEnabled(enabled);

	}

	@Override
	public void setToolTipText(String text) {
        m_label.setToolTipText(text);
        m_combobox.setToolTipText(text);
	}
	

    /**
     * Sets the preferred size of the internal component.
     *
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        m_combobox.setPreferredSize(new Dimension(width, height));
    }


    /**
     * Replaces the list of selectable strings in the component. If
     * <code>select</code> is specified (not null) and it exists in the
     * collection it will be selected. If <code>select</code> is null, the
     * previous value will stay selected (if it exists in the new list).
     *
     * @param newItems new strings for the combo box
     * @param select the item to select after the replace. Can be null, in which
     *            case the previous selection remains - if it exists in the new
     *            list.
     */
    public void replaceListItems(final Collection<String> newItems,
            final String select) {
        if (newItems == null || newItems.size() < 1) {
            throw new NullPointerException("The container with the new items"
                    + " can't be null or empty.");
        }
        final StringIconOption[] options =
            DefaultStringIconOption.createOptionArray(newItems);
        replaceListItems(options, select);
    }

    /**
     * Replaces the list of selectable strings in the component. If
     * <code>select</code> is specified (not null) and it exists in the
     * collection it will be selected. If <code>select</code> is null, the
     * previous value will stay selected (if it exists in the new list).
     *
     * @param newItems new {@link StringIconOption}s for the combo box
     * @param select the item to select after the replace. Can be null, in which
     *            case the previous selection remains - if it exists in the new
     *            list.
     */
    public void replaceListItems(final StringIconOption[] newItems,
            final String select) {
        if (newItems == null || newItems.length < 1) {
            throw new NullPointerException("The container with the new items"
                    + " can't be null or empty.");
        }
        final String sel;
        if (select == null) {
            sel = ((SettingsModelString)getModel()).getStringValue();
        } else {
            sel = select;
        }

        m_combobox.removeAllItems();
        StringIconOption selOption = null;
        for (final StringIconOption option : newItems) {
            if (option == null) {
                throw new NullPointerException("Options in the selection"
                        + " list can't be null");
            }
            m_combobox.addItem(option);
            if (option.getText().equals(sel)) {
                selOption = option;
            }
        }

        if (selOption == null) {
            m_combobox.setSelectedIndex(0);
        } else {
            m_combobox.setSelectedItem(selOption);
        }
        //update the size of the comboBox and force the repainting
        //of the whole panel
        m_combobox.setSize(m_combobox.getPreferredSize());
        getComponentPanel().validate();
    }

}
