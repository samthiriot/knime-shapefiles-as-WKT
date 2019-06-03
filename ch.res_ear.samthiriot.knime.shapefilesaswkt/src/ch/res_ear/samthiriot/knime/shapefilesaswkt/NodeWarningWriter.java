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

import java.util.LinkedHashSet;

import org.knime.core.node.NodeLogger;

/**
 * Displays the warnings in a node logger, 
 * and also as a node warning. 
 * Avoids to display the same message two times.
 * 
 * @author Samuel Thiriot
 *
 */
public class NodeWarningWriter implements IWarningWriter {

	private final NodeLogger logger;
	private LinkedHashSet<String> messages = new LinkedHashSet<String>();
	
	public final static int MAX_MESSAGES = 100;
	
	public NodeWarningWriter(NodeLogger logger) {
		this.logger = logger;
	}

	@Override
	public void warn(String s) {
		
		final int size = messages.size();
		
		if (size == MAX_MESSAGES)
			messages.add("[...]");

		else if (size < MAX_MESSAGES && messages.add(s))
			this.logger.warn(s);
	}
	
	/**
	 * Returns null if no message,
	 * or a string containing the messages
	 * @return
	 */
	public String buildWarnings() {
		if (messages.isEmpty())
			return null;
		StringBuffer sb = new StringBuffer();
		for (String s: messages) {
			if (sb.length() > 0)
				sb.append("\n");
			sb.append(s);
		}
		return sb.toString();
	}

}
