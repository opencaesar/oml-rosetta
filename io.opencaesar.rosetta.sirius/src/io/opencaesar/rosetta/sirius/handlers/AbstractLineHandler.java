/**
 * 
 * Copyright 2019-2021 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package io.opencaesar.rosetta.sirius.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.sirius.table.metamodel.table.DLine;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Abstract base class for commands on line selections in Sirius tables.
 */
public abstract class AbstractLineHandler extends AbstractHandler {

	@Override
	public final Object execute(ExecutionEvent event) throws ExecutionException {
		var activeWindow = HandlerUtil.getActiveWorkbenchWindow(event);
		if (activeWindow == null) {
			return null;
		}
		var activePage = activeWindow.getActivePage();
		if (activePage == null) {
			return null;
		}
		if (!(activePage.getSelection() instanceof IStructuredSelection)) {
			return null;
		}
		var selection = (IStructuredSelection)activePage.getSelection();
		
		var lines = new ArrayList<DLine>(selection.size());
		for (var selectedObject : selection) {
			if (selectedObject instanceof DLine) {
				lines.add((DLine)selectedObject);
			}
		}
		if (!lines.isEmpty()) {
			execute(event, lines);
		}
		return null;
	}

	protected abstract void execute(ExecutionEvent event, List<DLine> lines);
}
