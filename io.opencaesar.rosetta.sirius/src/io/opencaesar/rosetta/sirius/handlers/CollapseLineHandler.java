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
