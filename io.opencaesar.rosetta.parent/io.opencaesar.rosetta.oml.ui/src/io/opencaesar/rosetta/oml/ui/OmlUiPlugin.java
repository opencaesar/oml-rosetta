package io.opencaesar.rosetta.oml.ui;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class OmlUiPlugin {

	public static final String BUNDLE_NAME;
	
	public static final ILog LOG;
	
	static {
		Bundle bundle = FrameworkUtil.getBundle(OmlUiPlugin.class);
		BUNDLE_NAME = bundle.getSymbolicName();
		LOG = Platform.getLog(bundle);
	}
	
	public static void log(IStatus status) {
		LOG.log(status);
	}
	
}
