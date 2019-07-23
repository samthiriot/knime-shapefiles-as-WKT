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
package ch.res_ear.samthiriot.knime.shapefilesaswkt.transform.randompoint;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * This is an example implementation of the node dialog of the
 * "ComputeCentroidForWKTGeometries" node.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}. In general, one can create an
 * arbitrary complex dialog using Java Swing.
 * 
 * @author Samuel Thiriot
 */
public class RandomPointInShapeNodeDialog extends DefaultNodeSettingsPane {

	protected SettingsModelBoolean checkBoxAutoSeed ;
    protected SettingsModelIntegerBounded checkBoxSeed ;

	/**
	 * New dialog pane for configuring the node. The dialog created here
	 * will show up when double clicking on a node in KNIME Analytics Platform.
	 */
    protected RandomPointInShapeNodeDialog() {
        super();
      

        addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                    "count",
                    1,
                    0, Integer.MAX_VALUE), // TODO long?
                    "Count to sample:", /*step*/ 1, /*componentwidth*/ 8));
        

        checkBoxAutoSeed = new SettingsModelBoolean("seed_auto", true);
        checkBoxSeed = new SettingsModelIntegerBounded("seed",
                55555,
                Integer.MIN_VALUE, Integer.MAX_VALUE
                );
        
        checkBoxAutoSeed.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				checkBoxSeed.setEnabled(!checkBoxAutoSeed.getBooleanValue());
			}
		});
        addDialogComponent(new DialogComponentBoolean(
        		checkBoxAutoSeed,
        		"automatic seed"
        		));
        
        
        addDialogComponent(new DialogComponentNumber(
        		checkBoxSeed,
        		"seed",
        		1
        		));
        
        
    }
}

