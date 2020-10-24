package io.opencaesar.rosetta.oml.ui.project;

import java.util.Map;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import io.opencaesar.rosetta.oml.ui.OmlUiPlugin;

/**
 * OML Project builder
 * 
 * This currently does nothing and exists as a placeholder.
 */
public class OmlProjectBuilder extends IncrementalProjectBuilder {
	
	public static final String NAME = OmlUiPlugin.BUNDLE_NAME + ".builder";

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		return null;
	}

	public static boolean isBuilderFor(ICommand command) {
		return NAME.equals(command.getBuilderName());
	}
}
