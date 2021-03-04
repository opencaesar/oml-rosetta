package io.opencaesar.rosetta.sirius;

import java.util.Set;

import org.eclipse.emf.common.ui.viewer.IViewerProvider;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * This detects when a Sirius table editor is opened and adds a workaround for
 * a bug where on macOS the selected line appears blank.
 * 
 * Eclipse Bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=472052
 */
public class BlankSelectedLineWorkaround implements IStartup {

	private static final Set<String> SIRIUS_TABLE_EDITOR_IDS = Set.of(
		"org.eclipse.sirius.table.ui.CrossTableEditorID",
		"org.eclipse.sirius.table.ui.EditionTableEditorID"
	);
	
	private static final String DTableEraseItemListener_CLASS_NAME =
			"org.eclipse.sirius.table.ui.tools.internal.editor.DTableEraseItemListener";

	/**
	 * Install the workaround to the specified editor part, if applicable.
	 */
	private static void installSiriusTableBlankLineWorkaround(IWorkbenchPartReference partRef) {
		if (!SIRIUS_TABLE_EDITOR_IDS.contains(partRef.getId())) {
			return;
		}
		var part = partRef.getPart(true);
		if (!(part instanceof IViewerProvider)) {
			return;
		}
		var viewer = ((IViewerProvider)part).getViewer();
		if (viewer == null) {
			return;
		}
		var control = viewer.getControl();
		if (control == null) {
			return;
		}

		// Wrap the DTableEraseItemListener with our workaround for the bug. To do this,
		// first remove all the EraseItem listeners so we can add them back in the same order,
		// but with the DTableEraseItemListener wrapped with our own.
		var listeners = control.getListeners(SWT.EraseItem);
		for (var listener : listeners) {
			control.removeListener(SWT.EraseItem, listener);
		}
		for (var listener : listeners) {
			if (listener.getClass().getName().equals(DTableEraseItemListener_CLASS_NAME)) {
				control.addListener(SWT.EraseItem, event -> {
					var selected = (event.detail & SWT.SELECTED) != 0;
					listener.handleEvent(event);
					if (selected) {
						// Set the SWT.BACKGROUND flag to false to prevent the background color
						// from being painted, since DTableEraseItemLitener takes care of that.
						// On macOS, not doing this seems to cause the standard background to be
						// painted over the background from DTableEraseItemLitener which can
						// cause the line to get blanked out when the foreground and background
						// colors are the same.
						event.detail &= ~SWT.BACKGROUND;
					}
				});
			} else {
				control.addListener(SWT.EraseItem, listener);
			}
		}
	}

	/**
	 * On startup, adds a listener to detect opened windows.
	 */
	@Override
	public void earlyStartup() {
		var workbench = PlatformUI.getWorkbench();
		workbench.getDisplay().asyncExec(() -> {
			for (var window : workbench.getWorkbenchWindows()) {
				WINDOW_LISTENER.windowOpened(window);
			}
			workbench.addWindowListener(WINDOW_LISTENER);
		});
	}
	
	/**
	 * When a window is opened, adds a listener to detect opened pages
	 */
	private static final IWindowListener WINDOW_LISTENER = new IWindowListener() {
		@Override
		public void windowOpened(IWorkbenchWindow window) {
			for (var page : window.getPages()) {
				PAGE_LISTENER.pageOpened(page);
			}
			window.addPageListener(PAGE_LISTENER);
		}
		
		@Override
		public void windowActivated(IWorkbenchWindow window) {
			// ignored
		}

		@Override
		public void windowDeactivated(IWorkbenchWindow window) {
			// ignored
		}

		@Override
		public void windowClosed(IWorkbenchWindow window) {
			window.removePageListener(PAGE_LISTENER);
		}
	};
	
	/**
	 * When a page is opened, adds a listener to detect opened parts
	 */
	private static final IPageListener PAGE_LISTENER = new IPageListener() {
		@Override
		public void pageOpened(IWorkbenchPage page) {
			for (var partRef : page.getEditorReferences()) {
				PART_LISTENER.partOpened(partRef);
			}
			page.addPartListener(PART_LISTENER);
		}
		
		@Override
		public void pageActivated(IWorkbenchPage page) {
			// ignored
		}

		@Override
		public void pageClosed(IWorkbenchPage page) {
			page.removePartListener(PART_LISTENER);
		}
	};
	
	/**
	 * When a part is opened, installs the workaround (if applicable).
	 */
	private static final IPartListener2 PART_LISTENER = new IPartListener2() {
		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
			installSiriusTableBlankLineWorkaround(partRef);
		}
	};
}
