package io.opencaesar.rosetta.oml.wizards;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.buildship.core.BuildConfiguration;
import org.eclipse.buildship.core.GradleBuild;
import org.eclipse.buildship.core.GradleCore;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.ui.XtextProjectHelper;

import io.opencaesar.oml.OmlFactory;
import io.opencaesar.oml.OmlPackage;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.rosetta.oml.ui.OmlUiPlugin;
import io.opencaesar.rosetta.oml.ui.project.OmlProject;
import io.opencaesar.rosetta.oml.ui.project.OmlProjectBuilder;

/**
 * Wizard for creating OML projects. Creates a "src" folder and configures
 * the project to have OML and Xtext natures.
 */
public class OmlProjectWizard extends Wizard implements INewWizard {
	
	private IWorkbench workbench;
	private WizardNewProjectCreationPage newProjectPage = new WizardNewProjectCreationPage("Create OML Project");
	
	private ProjectSetupPage setupPage = new ProjectSetupPage();
	private IProject newProject;
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		setWindowTitle("Create OML Project");
		newProjectPage.setTitle("Create an OML Project");
		newProjectPage.setDescription("Specify the project name.");
		newProjectPage.setImageDescriptor(OmlUiPlugin.OML_LOGO);
	}
	
	@Override
	public void addPages() {
		super.addPages();
		addPage(newProjectPage);
		addPage(setupPage);
	}

	@Override
	public boolean performFinish() {
		URI locationUri = newProjectPage.getLocationURI();
		String projectName = newProjectPage.getProjectName();
		if (locationUri== null) {
			return false;
		}
		if (newProject == null) {
			try {
				getContainer().run(true, false, monitor -> createProject(monitor, projectName, locationUri));
				workbench.getWorkingSetManager().addToWorkingSets(newProject, newProjectPage.getSelectedWorkingSets());
				BasicNewResourceWizard.selectAndReveal(newProject, workbench.getActiveWorkbenchWindow());
				return true;
			} catch (InvocationTargetException | InterruptedException e) {
				OmlUiPlugin.log(new Status(IStatus.ERROR, OmlUiPlugin.BUNDLE_NAME, IStatus.ERROR, e.getMessage(), e));
			}
		}
		return false;
	}

	private void createProject(IProgressMonitor monitor, String projectName, URI locationUri) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 5);
		IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
		description.setLocationURI(locationUri);
		description.setNatureIds(OmlProject.getRequiredNatures());
		ICommand xtextBuildCommand = description.newCommand();
		xtextBuildCommand.setBuilderName(XtextProjectHelper.BUILDER_ID);
		ICommand omlBuildCommand = description.newCommand();
		omlBuildCommand.setBuilderName(OmlProjectBuilder.NAME);
		description.setBuildSpec(new ICommand[] { xtextBuildCommand, omlBuildCommand });
		newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		try {
			newProject.create(description, subMonitor.split(1));
			newProject.open(subMonitor.newChild(1));
			
			// Create directory structure
			
			IFolder srcFolder = newProject.getFolder("src");
			srcFolder.create(true, true, new NullProgressMonitor());
			IFolder omlFolder = srcFolder.getFolder("oml");
			omlFolder.create(true, true, new NullProgressMonitor());
			
			IFolder baseFolder = omlFolder;
			List<String> basePathSegments = getPathSegments(setupPage.baseIri);
			for (String pathSegment : basePathSegments) {
				IFolder subFolder = baseFolder.getFolder(pathSegment);
				subFolder.create(true, true, new NullProgressMonitor());
				baseFolder = subFolder;
			}
			
			IFolder bundleFolder = omlFolder;
			List<String> bundlePathSegments = getPathSegments(setupPage.bundleIri);
			List<String> bundleFolderSegments = bundlePathSegments.subList(0, bundlePathSegments.size()-1);
			String bundleName = bundlePathSegments.get(bundlePathSegments.size()-1);
			for (String pathSegment : bundleFolderSegments) {
				IFolder subFolder = bundleFolder.getFolder(pathSegment);
				if (!subFolder.exists()) {
					subFolder.create(true, true, new NullProgressMonitor());
				}
				bundleFolder = subFolder;
			}
			
			subMonitor.worked(1);
			
			// Configure templates
			
			OmlProjectResourceTemplates templates = new OmlProjectResourceTemplates();
			templates.uriStartStringsToRewritePrefixes.put(setupPage.baseIri + (setupPage.baseIri.endsWith("/") ? "" : "/"), "src/oml/" + basePathSegments.stream().collect(Collectors.joining("/")) + "/");
			templates.uriStartStringsToRewritePrefixes.put("http://", "build/oml/");
			templates.baseIri = setupPage.baseIri;
			templates.bundleIri = setupPage.bundleIri;

			if (setupPage.configureGradle) {
				templates.gradleProjectName = newProject.getName();
				templates.gradleProjectGroup = setupPage.gradleGroupId;
				templates.gradleProjectTitle = setupPage.gradleTitle;
				templates.gradleProjectDescription = setupPage.gradleDescription;
				templates.gradleProjectVersion = setupPage.gradleVersion;
			}
			
			// Create catalog
			
			IFile catalogFile = newProject.getFile("catalog.xml");
			catalogFile.create(new ByteArrayInputStream(templates.catalogXml().getBytes(StandardCharsets.UTF_8)), true, subMonitor.split(1));
			
			// Create .fuseki.ttl
			
			IFile fusekiFile = newProject.getFile(".fuseki.ttl");
			fusekiFile.create(new ByteArrayInputStream(templates.fusekiTtl().getBytes(StandardCharsets.UTF_8)), true, subMonitor.split(1));

			// Create bundle
			
			Ontology bundle = (Ontology) OmlFactory.eINSTANCE.create(setupPage.bundleType);
			bundle.setIri(setupPage.bundleIri);
			bundle.setPrefix(bundleName);
			bundle.setSeparator(SeparatorKind.HASH);
						
			IFile bundleFile = bundleFolder.getFile(bundleName + ".oml");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			XtextResourceSet resourceSet = new XtextResourceSet();
			resourceSet.getPackageRegistry().put(OmlPackage.eNS_URI, OmlPackage.eINSTANCE);
			XtextResource resource = (XtextResource) resourceSet.createResource(org.eclipse.emf.common.util.URI.createURI(bundleFile.getLocationURI().toString()), "oml");
			resource.getContents().add(bundle);
			resource.doSave(baos, Collections.EMPTY_MAP);
			bundleFile.create(new ByteArrayInputStream(baos.toByteArray()), true, subMonitor.split(1));
			
			// Configure Gradle
			
			if (setupPage.configureGradle) {
				// Generate build.gradle
				
				IFile buildGradleFile = newProject.getFile("build.gradle");
				buildGradleFile.create(new ByteArrayInputStream(templates.buildGradle().getBytes(StandardCharsets.UTF_8)), true, subMonitor.split(1));
				
				Job.create("Configure Gradle", configureGradleMonitor -> {
					SubMonitor runGradleWrapperMonitor = SubMonitor.convert(configureGradleMonitor, 15);
					
					// Apply Gradle project nature
					GradleBuild build = GradleCore.getWorkspace()
						.createBuild(BuildConfiguration.forRootProjectDirectory(newProject.getLocation().toFile()).build());
					build.synchronize(runGradleWrapperMonitor.split(4));
					
					try {
						build.withConnection(connection -> {
							// Download gradlew
							connection.newBuild().forTasks("wrapper").run();
							configureGradleMonitor.worked(5);
							// Load OML dependencies
							connection.newBuild().forTasks("omlDependencies").run();
							configureGradleMonitor.worked(5);
							return null;
						}, runGradleWrapperMonitor.split(10));
					} catch (Exception e) {
						if (e instanceof RuntimeException) {
							throw (RuntimeException)e;
						}
						throw new RuntimeException(e);
					}
					newProject.refreshLocal(IResource.DEPTH_INFINITE, runGradleWrapperMonitor.split(1));
				}).schedule();
			}
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
	

	/**
	 * Wizard page for configuring Catalog, Bundle, and Gradle project
	 */
	private static class ProjectSetupPage extends WizardPage {
		
		private String baseIri = "http://example.com/project";
		
		private String bundleIri = baseIri + "/bundle";
		
		private Text bundleIriInput;
		
		private boolean configureGradle = true;
		
		private boolean gradleGroupIdModified = false;
		
		private String gradleGroupId = "com.example";
		
		private String gradleTitle = "Example";

		private String gradleDescription = "This is an example";
		
		private String gradleVersion = "1.0.0";
		
		private Text groupIdInput;

		private EClass bundleType = OmlPackage.Literals.DESCRIPTION_BUNDLE;
		
		protected ProjectSetupPage() {
			super("Project Setup");
			setPageComplete(false);
			setTitle("Configure New OML Project");
			setDescription("Set up an initial catalog, bundle ontology, and Gradle script.");
			setImageDescriptor(OmlUiPlugin.OML_LOGO);
		}

		@Override
		public void createControl(Composite parent) {
			Composite body = new Composite(parent, SWT.NONE);
			body.setLayout(new GridLayout(1, true));
			
			// Configure Catalog (set project base IRI)
			
			Group catalogGroup = new Group(body, SWT.NONE);
			catalogGroup.setText("Catalog");
			catalogGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			catalogGroup.setLayout(new GridLayout(1, true));
			new Label(catalogGroup, SWT.NONE).setText("Base URI");
			Text baseIriInput = new Text(catalogGroup, SWT.BORDER);
			baseIriInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			baseIriInput.setText(baseIri);
			baseIriInput.addModifyListener(e -> {
				String newBaseIri = baseIriInput.getText();
				if (bundleIri.startsWith(baseIri)) {
					bundleIriInput.setText(bundleIri.replaceFirst(Pattern.quote(baseIri), newBaseIri));
				}
				baseIri = newBaseIri;
				if (!gradleGroupIdModified) {
					try {
						URI uri = new URI(baseIri);
						if (uri.getHost() != null) {
							List<String> domainParts = Arrays.asList(uri.getHost().split("\\."));
							Collections.reverse(domainParts);
							groupIdInput.setText(gradleGroupId = domainParts.stream().collect(Collectors.joining(".")));
						}
					} catch (URISyntaxException ex) {
						// ignore
					}
				}
				validateInputs();
			});
			
			// Configure bundle (set ontology IRI and kind)
			
			Group bundleGroup = new Group(body, SWT.NONE);
			bundleGroup.setText("Bundle");
			bundleGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			bundleGroup.setLayout(new GridLayout(1, true));

			new Label(bundleGroup, SWT.NONE).setText("Bundle Kind");
			Composite bundleTypeRow = new Composite(bundleGroup, SWT.NONE);
			bundleTypeRow.setLayout(new GridLayout(2, false));
			Button descriptionBundleButton = new Button(bundleTypeRow, SWT.RADIO);
			descriptionBundleButton.setText("Description");
			descriptionBundleButton.setSelection(true);
			descriptionBundleButton.addListener(SWT.Selection, ev -> {
				if (descriptionBundleButton.getSelection()) bundleType = OmlPackage.Literals.DESCRIPTION_BUNDLE;
			});
			Button vocabularyBundleButton = new Button(bundleTypeRow, SWT.RADIO);
			vocabularyBundleButton.setText("Vocabulary");
			vocabularyBundleButton.addListener(SWT.Selection, ev -> {
				if (vocabularyBundleButton.getSelection()) bundleType = OmlPackage.Literals.VOCABULARY_BUNDLE;
			});
			
			new Label(bundleGroup, SWT.NONE).setText("Bundle IRI");
			bundleIriInput = new Text(bundleGroup, SWT.BORDER);
			bundleIriInput.setText(baseIri + "/bundle");
			bundleIriInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			bundleIriInput.addModifyListener(e -> {
				bundleIri = bundleIriInput.getText();
				validateInputs();
			});
			
			// Configure Gradle (project groupId, version, and description; and whether to download foundation vocabulary).
			
			Group gradleGroup = new Group(body, SWT.NONE);
			gradleGroup.setText("Gradle");
			gradleGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			gradleGroup.setLayout(new GridLayout(1, true));
			
			Button configureGradleCheckbox = new Button(gradleGroup, SWT.CHECK);
			configureGradleCheckbox.setSelection(configureGradle);
			configureGradleCheckbox.setText("Configure Gradle");
			configureGradleCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			
			new Label(gradleGroup, SWT.NONE).setText("Group ID");
			groupIdInput = new Text(gradleGroup, SWT.BORDER);
			groupIdInput.setText(gradleGroupId);
			groupIdInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			groupIdInput.addModifyListener(ev -> {
				if (groupIdInput.isFocusControl()) {
					gradleGroupIdModified = true;
				}
				gradleGroupId = groupIdInput.getText();
				validateInputs();
			});
			
			new Label(gradleGroup, SWT.NONE).setText("Version");
			Text versionInput = new Text(gradleGroup, SWT.BORDER);
			versionInput.setText(gradleVersion);
			versionInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			versionInput.addModifyListener(ev -> {
				gradleVersion = versionInput.getText();
				validateInputs();
			});

			new Label(gradleGroup, SWT.NONE).setText("Title");
			Text titleInput = new Text(gradleGroup, SWT.BORDER);
			titleInput.setText(gradleTitle);
			titleInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			titleInput.addModifyListener(ev -> {
				gradleTitle = titleInput.getText();
				validateInputs();
			});

			new Label(gradleGroup, SWT.NONE).setText("Description");
			Text descriptionInput = new Text(gradleGroup, SWT.BORDER);
			descriptionInput.setText(gradleDescription);
			descriptionInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			descriptionInput.addModifyListener(ev -> {
				gradleDescription = descriptionInput.getText();
				validateInputs();
			});
			
			configureGradleCheckbox.addListener(SWT.Selection, ev -> {
				configureGradle = configureGradleCheckbox.getSelection();
				groupIdInput.setEnabled(configureGradle);
				titleInput.setEnabled(configureGradle);
				descriptionInput.setEnabled(configureGradle);
				versionInput.setEnabled(configureGradle);
				validateInputs();
			});
			
			PlatformUI.getWorkbench().getHelpSystem().setHelp(body, OmlUiPlugin.BUNDLE_NAME + ".newProjectWizard");
			
			setControl(body);
		}

		
		@Override
		public void setVisible(boolean visible) {
			super.setVisible(visible);
			if (visible) {
				validateInputs();
			}
		}
		
		private void validateInputs() {
			setPageComplete(inputsValid());
		}
		
		private boolean inputsValid() {
			try {
				if (baseIri == null || baseIri.trim().isEmpty()) {
					return false;
				}
				URI baseUri = new URI(baseIri);
				if (baseUri.getHost() == null || baseUri.getHost().trim().isEmpty()) {
					return false;
				}
				if (bundleIri == null || !bundleIri.startsWith(baseIri.endsWith("/") ? baseIri : baseIri + "/")) {
					return false;
				}
				if (configureGradle) {
					if (gradleGroupId == null || gradleGroupId.trim().isEmpty()) {
						return false;
					}
					if (gradleVersion == null || gradleVersion.trim().isEmpty()) {
						return false;
					}
					if (gradleTitle == null || gradleTitle.trim().isEmpty()) {
						return false;
					}
				}
			} catch (Exception e) {
				return false;
			}
			return true;
		}
		
	}
	
	private static List<String> getPathSegments(String uri) throws URISyntaxException {
		List<String> pathSegments = new ArrayList<String>();
		URI baseUri = new URI(uri);
		pathSegments.add(baseUri.getHost());
		for (String pathSegment : baseUri.getPath().split("/")) {
			if (!pathSegment.trim().isEmpty()) {
				pathSegments.add(pathSegment.trim());
			}
		}
		return pathSegments;
	}
	
}
