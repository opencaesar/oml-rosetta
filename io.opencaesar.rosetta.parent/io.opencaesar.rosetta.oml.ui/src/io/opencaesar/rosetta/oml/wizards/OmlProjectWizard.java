package io.opencaesar.rosetta.oml.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import io.opencaesar.rosetta.oml.ui.OmlUiPlugin;
import io.opencaesar.rosetta.oml.ui.project.OmlProject;
import io.opencaesar.rosetta.oml.ui.project.OmlProjectBuilder;

/**
 * Wizard for creating OML projects. Creates a "src" folder and configures
 * the project to have OML and Xtext natures.
 */
public class OmlProjectWizard extends BasicNewResourceWizard {
		
	private WizardNewProjectCreationPage mainPage = new WizardNewProjectCreationPage("Create OML Project");
	private IProject newProject;
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		setWindowTitle("Create OML Project");
	}
	
	@Override
	public void addPages() {
		super.addPages();
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		String projectName = mainPage.getProjectName();
		URI projectLocation = mainPage.getLocationURI();
		if (projectLocation == null) {
			return false;
		}
		if (newProject == null) {
			try {
				getContainer().run(true, false, monitor -> createProject(monitor, projectName, projectLocation));
				getWorkbench().getWorkingSetManager().addToWorkingSets(newProject, mainPage.getSelectedWorkingSets());
				selectAndReveal(newProject);
				return true;
			} catch (InvocationTargetException | InterruptedException e) {
				OmlUiPlugin.log(new Status(IStatus.ERROR, OmlUiPlugin.BUNDLE_NAME, IStatus.ERROR, e.getMessage(), e));
			}
		}
		return false;
	}
	
	private void createProject(IProgressMonitor monitor, String projectName, URI projectLocation) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 30);
		IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
		description.setLocationURI(projectLocation);
		description.setNatureIds(OmlProject.getRequiredNatures());
		ICommand buildCommand = description.newCommand();
		buildCommand.setBuilderName(OmlProjectBuilder.NAME);
		description.setBuildSpec(new ICommand[] { buildCommand });
		newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		try {
			newProject.create(description, subMonitor.split(10));
			newProject.open(subMonitor.split(10));
			IFolder srcFolder = newProject.getFolder("src");
			srcFolder.create(true, true, subMonitor.split(10));
		} catch (Exception e) {
			if (newProject != null && newProject.exists()) {
				try {
					newProject.delete(true, new NullProgressMonitor());
				} catch (CoreException ignored) {
					
				}
			}
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
}
