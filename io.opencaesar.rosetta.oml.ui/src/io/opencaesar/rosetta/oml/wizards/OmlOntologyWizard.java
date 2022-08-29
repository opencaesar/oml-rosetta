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
package io.opencaesar.rosetta.oml.wizards;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import io.opencaesar.oml.OmlFactory;
import io.opencaesar.oml.OmlPackage;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.util.OmlConstants;
import io.opencaesar.rosetta.oml.ui.OmlUiPlugin;

/**
 * Wizard for creating ontology files.
 * 
 * Presents two pages, the first lets the user configure the ontology (type,
 * namespace IRI, prefix, and separator) and the second lets the user select
 * the file path and name.
 */
public class OmlOntologyWizard extends Wizard implements INewWizard {
	
	private IWorkbench workbench;
	
	private OntologySetupPage ontologySetupPage;
	private FilePage filePage;

	private EClass ontologyKind;
	private String ontologyNamespace;
	private String ontologyPrefix;
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		IPath folderPath = null;
		
		// Derive the ontology namespace from the selected folder, or use a default
		// if not possible.
		ontologyNamespace = "http://example.com/";
		if (selection != null) {
			if (selection.getFirstElement() instanceof IFile) {
				folderPath = ((IFile)selection.getFirstElement()).getParent().getFullPath();
			} else if (selection.getFirstElement() instanceof IFolder) {
				folderPath = ((IFolder)selection.getFirstElement()).getFullPath();
			} else if (selection.getFirstElement() instanceof IProject) {
				folderPath = ((IProject)selection.getFirstElement()).getFolder("src").getFolder("oml").getFullPath();
			}
		}
		if (folderPath != null) {
			if (folderPath.segmentCount() > 3 && folderPath.segment(1).equals("src") && folderPath.segment(2).equals("oml")) {
				ontologyNamespace = "http://" + Arrays.stream(folderPath.segments()).skip(3).collect(Collectors.joining("/")) + "/";
			}
		}
		
		ontologySetupPage = new OntologySetupPage();
		filePage = new FilePage(selection);
		if (folderPath != null) {
			filePage.setContainerFullPath(folderPath);
		}
		
		ontologyNamespace += "newOntology#";
		
		setWindowTitle("Create OML Model");
	}
	
	@Override
	public void addPages() {
		super.addPages();
		addPage(ontologySetupPage);
		addPage(filePage);
	}
	
	@Override
	public boolean performFinish() {
		try {
			IFile file = filePage.createNewFile();
			URI uri = URI.createURI(file.getLocationURI().toString());

			Ontology ontology = (Ontology) OmlFactory.eINSTANCE.create(ontologyKind);
			ontology.setNamespace(ontologyNamespace);
			ontology.setPrefix(ontologyPrefix);
			
			ResourceSet resourceSet = new ResourceSetImpl();
			Resource resource = resourceSet.createResource(uri);
			resource.getContents().add(ontology);
			resource.save(Collections.EMPTY_MAP);

			BasicNewResourceWizard.selectAndReveal(file, workbench.getActiveWorkbenchWindow());
			IDE.openEditor(workbench.getActiveWorkbenchWindow().getActivePage(), file);
			return true;
		} catch (IOException | CoreException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		
	}
	
	/**
	 * Allows the user to configure the ontology, specifying the type (vocabulary,
	 * bundle, or description), namespace IRI, namespace separator, and namespace
	 * prefix.
	 * 
	 * The prefix is derived from the last segment of the namespace IRI unless the
	 * user explicitly changes it.
	 */
	private class OntologySetupPage extends WizardPage {
		
		private boolean ontologyPrefixChanged = false;
		private Text ontologyPrefixInput;
		
		protected OntologySetupPage() {
			super("Model Setup");
			setPageComplete(false);
			setTitle("Model Setup");
			setDescription("Specify an ontology kind, namespace, and prefix");
			setImageDescriptor(OmlUiPlugin.OML_LOGO);
		}

		@Override
		public void createControl(Composite parent) {
			Composite body = new Composite(parent, SWT.NONE);
			body.setLayout(new GridLayout(1, true));

			addOntologyKindSelector(body);
			addNamespaceInput(body);
			addPrefixInput(body);
			
			setControl(body);
		}
		
		private void addOntologyKindSelector(Composite body) {
			new Label(body, SWT.NONE).setText("Ontology Kind");
			Composite row = new Composite(body, SWT.NONE);
			row.setLayout(new GridLayout(4, false));
			final Button descriptionKind = new Button(row, SWT.RADIO);
			descriptionKind.setText("Description");
			final Button vocabularyKind = new Button(row, SWT.RADIO);
			vocabularyKind.setText("Vocabulary");
			final Button descriptionBundleKind = new Button(row, SWT.RADIO);
			descriptionBundleKind.setText("Description Bundle");
			final Button vocabularyBundleKind = new Button(row, SWT.RADIO);
			vocabularyBundleKind.setText("Vocabulary Bundle");
			SelectionListener selectionListener = new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
				public void widgetSelected(SelectionEvent e) {
					if (e.getSource() == vocabularyKind) {
						ontologyKind = OmlPackage.Literals.VOCABULARY;
					} else if (e.getSource() == vocabularyBundleKind) {
						ontologyKind = OmlPackage.Literals.VOCABULARY_BUNDLE;
					} else if (e.getSource() == descriptionKind) {
						ontologyKind = OmlPackage.Literals.DESCRIPTION;
					} else if (e.getSource() == descriptionBundleKind) {
						ontologyKind = OmlPackage.Literals.DESCRIPTION_BUNDLE;
					} else {
						ontologyKind = null;
					}
					onPageUpdated();
				}
			};
			vocabularyKind.addSelectionListener(selectionListener);
			vocabularyBundleKind.addSelectionListener(selectionListener);
			descriptionKind.addSelectionListener(selectionListener);
			descriptionBundleKind.addSelectionListener(selectionListener);
		}
		
		private void addNamespaceInput(Composite body) {
			new Label(body, SWT.NONE).setText("Namespace");
			Text input = new Text(body, SWT.BORDER);
			input.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			input.addModifyListener(event -> {
				ontologyNamespace = input.getText();
				onPageUpdated();
			});
			input.setText(ontologyNamespace);
		}
		
		private void addPrefixInput(Composite body) {
			new Label(body, SWT.NONE).setText("Prefix");
			ontologyPrefixInput = new Text(body, SWT.BORDER);
			ontologyPrefixInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
			ontologyPrefixInput.addModifyListener(event -> {
				ontologyPrefix = ontologyPrefixInput.getText();
				if (ontologyPrefixInput.isFocusControl()) {
					ontologyPrefixChanged = true;
					onPageUpdated();
				}
			});
			if (ontologyPrefix != null) {
				ontologyPrefixInput.setText(ontologyPrefix);
			}
		}
		
		private void onPageUpdated() {
			try {
				if (!(ontologyNamespace.endsWith("/") || ontologyNamespace.endsWith("#"))) {
					// Ensure an namespace separator is set
					setPageComplete(false);
					return;
				}
				java.net.URI parsed = new java.net.URI(ontologyNamespace);
				if (parsed.getPath() != null) {
					String[] pathSegments = parsed.getPath().split("/");
					if (pathSegments.length == 0) {
						setPageComplete(false);
						return;
					}
					String fileName = pathSegments[pathSegments.length - 1];
					filePage.setFileName(fileName+"."+OmlConstants.OML_EXTENSION);
					String defaultPrefix = fileName;
					if (!ontologyPrefixChanged) {
						ontologyPrefix = defaultPrefix;
						if (ontologyPrefixInput != null) {
							ontologyPrefixInput.setText(ontologyPrefix);
						}
					}
					if (ontologyPrefix == null || ontologyPrefix.isEmpty()) {
						// Ensure an ontology prefix is set
						setPageComplete(false);
						return;
					}
				}
			} catch (URISyntaxException e) {
				// Ensure the IRI is valid
				setPageComplete(false);
				return;
			}
			if (ontologyKind == null) {
				setPageComplete(false);
				return;
			}
			setPageComplete(true);
		}
	}

	/**
	 * File Creation Page
	 * 
	 * Lets the user select a folder (by default the folder selected by the user when launching
	 * the wizard) and filename (by default the last segment of the namespace IRI, with a .oml
	 * extension for vocabularies and bundles or a .omlxmi extension for descriptions.)
	 */
	private class FilePage extends WizardNewFileCreationPage {

		protected FilePage(IStructuredSelection selection) {
			super("FilePage", selection);
			setTitle("Ontology File");
			setDescription("Specify the ontology file location and type");
			setImageDescriptor(OmlUiPlugin.OML_LOGO);
		}

		@Override
		public boolean validatePage() {
			if (!super.validatePage()) {
				return false;
			}
			String fileName = this.getFileName();
			if (fileName == null) {
				return false;
			}
			if (!getFileName().endsWith(".oml") && !getFileName().endsWith(".omlxmi")) {
				return false;
			}
			return true;
		}
		
	}
}
