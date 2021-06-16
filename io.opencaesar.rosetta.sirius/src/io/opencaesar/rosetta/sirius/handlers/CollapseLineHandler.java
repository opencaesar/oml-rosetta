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

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.sirius.business.api.session.SessionManager;
import org.eclipse.sirius.table.metamodel.table.DLine;
import org.eclipse.sirius.table.metamodel.table.TablePackage;

/**
 * Collapses a Sirius DLine and all its children.
 */
public class CollapseLineHandler extends AbstractLineHandler {

	@Override
	protected void execute(ExecutionEvent event, List<DLine> lines) {
		var domain = SessionManager.INSTANCE.getSession(lines.get(0)).getTransactionalEditingDomain();
		var command = new CompoundCommand("Collapse Lines");
		for (var line : lines) {
			addCollapseCommands(domain, line, command);
		}
		if (command.canExecute()) {
			domain.getCommandStack().execute(command);
		}
	}
	
	private static void addCollapseCommands(TransactionalEditingDomain domain, DLine line, CompoundCommand command) {
		for (var subline : line.getLines()) {
			addCollapseCommands(domain, subline, command);
		}
		if (!line.isCollapsed()) {
			command.append(SetCommand.create(domain, line, TablePackage.Literals.DLINE__COLLAPSED, true));
		}
	}
}
