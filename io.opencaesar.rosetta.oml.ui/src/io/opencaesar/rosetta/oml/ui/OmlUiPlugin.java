package io.opencaesar.rosetta.oml.ui;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class OmlUiPlugin {

	public static final Bundle BUNDLE = FrameworkUtil.getBundle(OmlUiPlugin.class);
	
	public static final String BUNDLE_NAME = BUNDLE.getSymbolicName();
	
	public static final ILog LOG = Platform.getLog(BUNDLE);
	
	public static final ImageDescriptor OML_LOGO = ImageDescriptor.createFromURL(FileLocator.find(BUNDLE, new Path("images/oml_logo.png")));
	
	public static void log(IStatus status) {
		LOG.log(status);
	}
	
}
