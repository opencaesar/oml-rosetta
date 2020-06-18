package io.opencaesar.rosetta.oml.ui.project;

import java.util.Arrays;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.xtext.ui.XtextProjectHelper;

import io.opencaesar.rosetta.oml.ui.OmlUiPlugin;

/**
 * OML Project Nature.
 * 
 * Adds OmlProjectBuilder to the project buildSpec.
 */
public class OmlProject implements IProjectNature {
	
	public static final String NATURE_ID = OmlUiPlugin.BUNDLE_NAME + ".nature";
	
	public static final String[] getRequiredNatures() {
		return new String[] { NATURE_ID, XtextProjectHelper.NATURE_ID };
	}
	
	private IProject project;

	/**
	 * Adds OmlProjectBuilder to the project buildSpec
	 */
	@Override
	public void configure() throws CoreException {
		IProjectDescription description = project.getDescription();
		ICommand[] buildSpec = description.getBuildSpec();
		if (Arrays.stream(buildSpec).noneMatch(OmlProjectBuilder::isBuilderFor)) {
			ICommand[] newBuildSpec = new ICommand[buildSpec.length + 1];
			System.arraycopy(buildSpec, 0, newBuildSpec, 0, buildSpec.length);
			ICommand newCommand = description.newCommand();
			newCommand.setBuilderName(OmlProjectBuilder.NAME);
			newBuildSpec[newBuildSpec.length - 1] = newCommand;
			description.setBuildSpec(newBuildSpec);
			project.setDescription(description, null);
		}
	}

	/**
	 * Removes OmlProjectBuilder from the project buildSpec
	 */
	@Override
	public void deconfigure() throws CoreException {
		IProjectDescription description = project.getDescription();
		ICommand[] buildSpec = description.getBuildSpec();
		if (Arrays.stream(buildSpec).anyMatch(OmlProjectBuilder::isBuilderFor)) {
			ICommand[] newBuildSpec = Arrays.stream(buildSpec).filter(command -> !OmlProjectBuilder.isBuilderFor(command)).toArray(ICommand[]::new);
			description.setBuildSpec(newBuildSpec);
			project.setDescription(description, null);
		}
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

}
