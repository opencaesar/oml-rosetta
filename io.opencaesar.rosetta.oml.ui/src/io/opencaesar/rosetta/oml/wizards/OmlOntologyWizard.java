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
import java.util.Collections;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import io.opencaesar.oml.OmlFactory;
import io.opencaesar.oml.OmlPackage;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.util.OmlConstants;
import io.opencaesar.oml.util.OmlResolve;
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
	private IContainer container;
	
	private OntologySetupPage ontologySetupPage;

	private EClass ontologyKind;
	private String ontologyNamespace;
	private String ontologyPrefix;
	private String ontologyExtension;
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		
		if (selection != null) {
			if (selection.getFirstElement() instanceof IFile) {
				container = ((IFile)selection.getFirstElement()).getParent();
			} else if (selection.getFirstElement() instanceof IFolder) {
				container = ((IFolder)selection.getFirstElement());
			} else if (selection.getFirstElement() instanceof IProject) {
				container = (IProject)selection.getFirstElement();
			}
		}

		// Derive the ontology namespace from the selected folder, or use a default if not possible.
		if (container != null) {
			var uri = URI.createPlatformResourceURI(container.getFullPath().toString()+"/", true);
			var iri = OmlResolve.deresolveUri(uri);
			if (iri != null) {
				ontologyNamespace = iri.toString();
			} else {
				ontologyNamespace = "http://";
			}
		}
		
		ontologySetupPage = new OntologySetupPage();
		
		ontologyNamespace += "newOntology#";
		
		setWindowTitle("Create OML Model");
	}
	
	@Override
	public void addPages() {
		super.addPages();
		addPage(ontologySetupPage);
	}
	
	@Override
	public boolean performFinish() {
		try {
			URI context = URI.createPlatformResourceURI(container.getFullPath().toString(), true);
			String iri = ontologyNamespace.substring(0, ontologyNamespace.length()-1);
			URI uri = OmlResolve.resolveUri(context, iri);
			if (uri != null) {
				uri = uri.appendFileExtension(ontologyExtension);
			}

			Ontology ontology = (Ontology) OmlFactory.eINSTANCE.create(ontologyKind);
			ontology.setNamespace(ontologyNamespace);
			ontology.setPrefix(ontologyPrefix);
			
			ResourceSet resourceSet = new ResourceSetImpl();
			Resource resource = resourceSet.createResource(uri);
			resource.getContents().add(ontology);
			resource.save(Collections.EMPTY_MAP);

			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(uri.toPlatformString(true)));
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
		private Combo ontologyExtensionInput;
		
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
			addExtensionInput(body);
			
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
		
		private void addExtensionInput(Composite body) {
			new Label(body, SWT.NONE).setText("File Extension");
			ontologyExtensionInput = new Combo(body, SWT.BORDER | SWT.READ_ONLY);
			OmlConstants.OML_EXTENSION_LIST.forEach(i -> ontologyExtensionInput.add(i));
			ontologyExtensionInput.setLayoutData(new GridData(SWT.LEFT, SWT.BEGINNING, true, false));
			ontologyExtensionInput.addModifyListener(event -> {
				ontologyExtension = ontologyExtensionInput.getText();
				if (ontologyExtensionInput.isFocusControl()) {
					onPageUpdated();
				}
			});
			ontologyExtensionInput.select(0);
		}

		private void onPageUpdated() {
			try {
				if (!(ontologyNamespace.endsWith("/") || ontologyNamespace.endsWith("#"))) {
					// Ensure an namespace separator is set
					setPageComplete(false);
					return;
				}
				java.net.URI parsed = new java.net.URI(ontologyNamespace);
				String[] pathSegments = parsed.getSchemeSpecificPart(). split("/");
				if (pathSegments.length == 0) {
					setPageComplete(false);
					return;
				}
				String fileName = pathSegments[pathSegments.length - 1];
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
				URI context = URI.createPlatformResourceURI(container.getFullPath().toString(), true);
				String iri = ontologyNamespace.substring(0, ontologyNamespace.length()-1);
				URI uri = OmlResolve.resolveUri(context, iri);
				if (uri == null) {
					setPageComplete(false);
					setErrorMessage("No catalog.xml can be found in this project");
					return;
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

}
