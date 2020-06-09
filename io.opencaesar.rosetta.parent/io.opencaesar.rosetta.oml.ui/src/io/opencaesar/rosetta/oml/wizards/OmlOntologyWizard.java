package io.opencaesar.rosetta.oml.wizards;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
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

import io.opencaesar.oml.OmlFactory;
import io.opencaesar.oml.OmlPackage;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.util.OmlXMIResource;

/**
 * Wizard for creating ontology files.
 * 
 * Presents two pages, the first lets the user configure the ontology (type,
 * namespace IRI, prefix, and separator) and the second lets the user select
 * the file path and name.
 */
public class OmlOntologyWizard extends Wizard implements INewWizard {	
	private OntologySetupPage ontologySetupPage;
	private FilePage filePage;

	private EClass ontologyKind;
	private String ontologyNamespace;
	private String ontologyPrefix;
	private SeparatorKind ontologySeparator;
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		IPath folderPath = null;
		
		// Derive the ontology namespace from the selected folder, or use a default
		// if not possible.
		ontologyNamespace = "http://example.com/ontology";
		if (selection != null) {
			if (selection.getFirstElement() instanceof IFile) {
				folderPath = ((IFile)selection.getFirstElement()).getParent().getFullPath();
			} else if (selection.getFirstElement() instanceof IFolder) {
				folderPath = ((IFolder)selection.getFirstElement()).getFullPath();
			} else if (selection.getFirstElement() instanceof IProject) {
				folderPath = ((IProject)selection.getFirstElement()).getFolder("src").getFullPath();
			}
		}
		if (folderPath != null) {
			if (folderPath.segmentCount() > 2 && folderPath.segment(1).equals("src")) {
				ontologyNamespace = "http://" + Arrays.stream(folderPath.segments()).skip(2).collect(Collectors.joining("/")) + "/ontology";
			}
		}
		
		ontologySetupPage = new OntologySetupPage();
		filePage = new FilePage(selection);
		if (folderPath != null) {
			filePage.setContainerFullPath(folderPath);
		}
		
		setWindowTitle("Create OML Ontology");
	}
	
	@Override
	public void addPages() {
		super.addPages();
		addPage(ontologySetupPage);
		addPage(filePage);
	}

	@Override
	public boolean performFinish() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			if (filePage.getFileName().endsWith(".oml")) {
				// If the user saves the file with a .oml extension, generate OML text
				OutputStreamWriter sb = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
				if (ontologyKind == OmlPackage.Literals.DESCRIPTION) {
					sb.append("description <");
				} else if (ontologyKind == OmlPackage.Literals.VOCABULARY) {
					sb.append("vocabulary <");
				} else if (ontologyKind == OmlPackage.Literals.VOCABULARY_BUNDLE) {
					sb.append("vocabulary bundle <");
				}
				sb.append(ontologyNamespace).append("> with ").append(ontologySeparator.getLiteral()).append(" as ").append(ontologyPrefix).append(" {\n");
				sb.append("    \n");
				sb.append("}\n");
				sb.flush();
			} else if (filePage.getFileName().endsWith(".omlxmi")) {
				// If the user saves the file with a .omlxmi extension, generate OML XMI.
				Ontology ontology = (Ontology) OmlFactory.eINSTANCE.create(ontologyKind);
				ontology.setIri(ontologyNamespace);
				ontology.setSeparator(ontologySeparator);
				ontology.setPrefix(ontologyPrefix);
				OmlXMIResource resource = new OmlXMIResource(URI.createURI(ontologyNamespace));
				resource.getContents().add(ontology);
				resource.doSave(baos, Collections.EMPTY_MAP);
			} else {
				throw new IllegalStateException("File not .oml or .omlxmi");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(filePage.getContainerFullPath());
		IFile file = folder.getFile(filePage.getFileName());
		try {
			file.create(new ByteArrayInputStream(baos.toByteArray()), true, new NullProgressMonitor());
			return true;
		} catch (CoreException e) {
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
			super("Ontology Setup");
			ontologySeparator = SeparatorKind.HASH;
			setPageComplete(false);
		}

		@Override
		public void createControl(Composite parent) {
			Composite body = new Composite(parent, SWT.NONE);
			body.setLayout(new GridLayout(1, true));

			addOntologyKindSelector(body);
			addNamespaceInput(body);
			addPrefixInput(body);
			addSeparatorInput(body);
			
			setControl(body);
		}
		
		private void addOntologyKindSelector(Composite body) {
			new Label(body, SWT.NONE).setText("Ontology Kind");
			Composite row = new Composite(body, SWT.NONE);
			row.setLayout(new GridLayout(3, false));
			final Button vocabularyKind = new Button(row, SWT.RADIO);
			vocabularyKind.setText("Vocabulary");
			final Button bundleKind = new Button(row, SWT.RADIO);
			bundleKind.setText("Bundle");
			final Button descriptionKind = new Button(row, SWT.RADIO);
			descriptionKind.setText("Description");
			SelectionListener selectionListener = new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
				public void widgetSelected(SelectionEvent e) {
					if (e.getSource() == vocabularyKind) {
						ontologyKind = OmlPackage.Literals.VOCABULARY;
					} else if (e.getSource() == bundleKind) {
						ontologyKind = OmlPackage.Literals.VOCABULARY_BUNDLE;
					} else if (e.getSource() == descriptionKind) {
						ontologyKind = OmlPackage.Literals.DESCRIPTION;
					} else {
						ontologyKind = null;
					}
					onPageUpdated();
				}
			};
			vocabularyKind.addSelectionListener(selectionListener);
			bundleKind.addSelectionListener(selectionListener);
			descriptionKind.addSelectionListener(selectionListener);
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
		
		private void addSeparatorInput(Composite body) {
			new Label(body, SWT.NONE).setText("Separator");
			Composite row = new Composite(body, SWT.NONE);
			row.setLayout(new GridLayout(2, false));
			final Button hash = new Button(row, SWT.RADIO);
			hash.setText("#");
			hash.setSelection(true);
			final Button slash = new Button(row, SWT.RADIO);
			slash.setText("/");
			SelectionListener selectionListener = new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
				public void widgetSelected(SelectionEvent e) {
					if (e.getSource() == slash) {
						ontologySeparator = SeparatorKind.SLASH;
					} else {
						ontologySeparator = SeparatorKind.HASH;
					}
					onPageUpdated();
				}
			};
			hash.addSelectionListener(selectionListener);
			slash.addSelectionListener(selectionListener);
		}

		private void onPageUpdated() {
			try {
				java.net.URI parsed = new java.net.URI(ontologyNamespace);
				if (parsed.getPath() != null) {
					String[] pathSegments = parsed.getPath().split("/");
					String fileName = pathSegments[pathSegments.length - 1];
					String defaultPrefix;
					if (fileName.endsWith(".oml") || fileName.endsWith(".omlxmi")) {
						filePage.setFileName(fileName);
						defaultPrefix = fileName.substring(0, fileName.indexOf("."));
					} else {
						if (ontologyKind == OmlPackage.Literals.DESCRIPTION) {
							filePage.setFileName(fileName + ".omlxmi");
						} else {
							filePage.setFileName(fileName + ".oml");
						}
						defaultPrefix = fileName;
					}
					if (!ontologyPrefixChanged) {
						ontologyPrefix = defaultPrefix;
						if (ontologyPrefixInput != null) {
							ontologyPrefixInput.setText(ontologyPrefix);
						}
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
		}

		@Override
		public boolean validatePage() {
			if (super.validatePage()) {
				String fileName = this.getFileName();
				if (fileName == null) {
					return false;
				}
				if (!getFileName().endsWith(".oml") && !getFileName().endsWith(".omlxmi")) {
					return false;
				}
				if (this.getContainerFullPath() == null) {
					return false;
				}
				IPath containerPath = this.getContainerFullPath();
				IContainer container = ResourcesPlugin.getWorkspace().getRoot().getFolder(containerPath);
				if (container == null) {
					return false;
				}
			}
			return true;
		}
		
	}
}
