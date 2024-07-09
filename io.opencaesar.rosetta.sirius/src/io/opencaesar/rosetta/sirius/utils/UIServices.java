package io.opencaesar.rosetta.sirius.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

public class UIServices {

	public static void openAndSelectInEditor(EObject eObject) {
		String fileURI = eObject.eResource().getURI().toPlatformString(true);
		IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(fileURI));
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(workspaceFile.getName());
		try {
			IEditorPart openEditor = page.openEditor(new FileEditorInput(workspaceFile), desc.getId(), true);
			if (openEditor instanceof XtextEditor) {
				IXtextDocument document = ((XtextEditor) openEditor).getDocument();
				XtextResource xtextResource = document.readOnly(new IUnitOfWork<XtextResource, XtextResource>() {
					public XtextResource exec(XtextResource state) throws Exception {
						return state;
			    	}
			    });				
				EObject xtextEObject = xtextResource.getResourceSet().getEObject(EcoreUtil.getURI(eObject), false);
				if (xtextEObject != null) {
					ICompositeNode node = NodeModelUtils.findActualNodeFor(xtextEObject);
					if (node != null) {
						int offset = node.getOffset();
						int length = node.getTotalEndOffset() - offset;
						((XtextEditor) openEditor).selectAndReveal(offset, length);
					}
				}
			} else if (openEditor instanceof ISelectionProvider) {
				var selection = new StructuredSelection(eObject);
				((ISelectionProvider)openEditor).setSelection(selection);
			}
		} catch (PartInitException e) {
			System.err.println(e);
		}
	}

}
