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
