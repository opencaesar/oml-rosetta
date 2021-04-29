package io.opencaesar.rosetta.sirius;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.ui.viewer.IViewerProvider;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.sirius.ui.business.api.dialect.DialectEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.EditorPart;

/**
 * An IGotoMarker implementation that selects a Sirius Representation Element 
 * referenced from a Marker in an open Sirius editor. If the representation is
 * closed or the representation no longer contains the referenced representation
 * element, an error dialog is displayed.
 * 
 * When a marker is created, this editor's ID is set as the marker's IDE.EDITOR_ID_ATTR
 * attribute along with the representation URI, representation element fragment, and
 * representation name (for display in an error dialog). Opening the marker causes this
 * editor to be activated, at which point it looks for the referenced representation
 * element in all the open Sirius editor, and then selects it before closing
 * itself.
 */
public class MarkerRepresentationElementSelector extends EditorPart implements IGotoMarker {
	public static final String EDITOR_ID = "io.opencaesar.rosetta.sirius.MarkerRepresentationElementSelector";
	public static final String REPRESENTATION_URI = EDITOR_ID + ".REPRESENTATION_URI";
	public static final String REPRESENTATION_ELEMENT_FRAGMENT = EDITOR_ID + ".REPRESENTATION_ELEMENT_FRAGMENT";
	public static final String REPRESENTATION_NAME = EDITOR_ID + ".REPRESENTATION_NAME";

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public void gotoMarker(IMarker marker) {
		var shell = getSite().getShell();
		var openedRepresentationElement = false;
		var representationOpen = false;
		try {
			var representationUri = URI.createURI(marker.getAttribute(REPRESENTATION_URI).toString());
			var representationElementFragment = marker.getAttribute(REPRESENTATION_ELEMENT_FRAGMENT).toString();
			for (var editorReference : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences()) {
				var editor = editorReference.getPart(false);
				if (editor == null || !(editor instanceof DialectEditor) || !(editor instanceof IViewerProvider)) {
					continue;
				}
				var representation = ((DialectEditor)editor).getRepresentation();
				if (!EcoreUtil.getURI(representation).equals(representationUri)) {
					continue;
				}
				representationOpen = true;
				var representationElement = representation.eResource().getEObject(representationElementFragment);
				if (representationElement != null) {
					getSite().getPage().activate(editor);
					((IViewerProvider)editor).getViewer().setSelection(new StructuredSelection(representationElement));
					openedRepresentationElement = true;
					break;
				}
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			getSite().getPage().closeEditor(this, false);
		}
		if (!openedRepresentationElement) {
			var representationName = marker.getAttribute(REPRESENTATION_NAME, "(unknown)");
			var message = representationOpen
					? "it no longer appears to be included in the " + representationName + " view."
					: "the " + representationName + " view it was presented in appears to be closed.";
			MessageDialog.openError(shell, "Unable to open element", "The selected element couldn't be found; " + message);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// does nothing
	}

	@Override
	public void doSaveAs() {
		// does nothing
	}

	@Override
	public boolean isDirty() {
		// does nothing
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// does nothing
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		// does nothing
	}

	@Override
	public void setFocus() {
		// does nothing
	}

}
