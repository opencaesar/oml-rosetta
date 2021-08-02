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
package io.opencaesar.rosetta.oml.ui.compare;

import java.util.Collections;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.compare.ide.ui.logical.AbstractModelResolver;
import org.eclipse.emf.compare.ide.ui.logical.IStorageProviderAccessor;
import org.eclipse.emf.compare.ide.ui.logical.IStorageProviderAccessor.DiffSide;
import org.eclipse.emf.compare.ide.ui.logical.SynchronizationModel;
import org.eclipse.emf.compare.ide.utils.ResourceUtil;
import org.eclipse.emf.compare.ide.utils.StorageTraversal;

/**
 * Model resolver that doesn't really do any resolving. Instead it provides a
 * SynchronizationModel with only the resources the user selected for comparison.
 *
 */
public class OmlModelResolver extends AbstractModelResolver {

	@Override
	public boolean canResolve(IStorage sourceStorage) {
		return true;
	}
	
	// Local
	
	@Override
	public StorageTraversal resolveLocalModel(IResource resource, IProgressMonitor monitor)
			throws InterruptedException {
		return getLocalTraversal(resource);
	}

	@Override
	public SynchronizationModel resolveLocalModels(IResource left, IResource right, IResource origin,
			IProgressMonitor monitor) throws InterruptedException {
		return new SynchronizationModel(getLocalTraversal(left), getLocalTraversal(right), getLocalTraversal(origin));
	}

	/**
	 * For local resources, include the given resource directly in the StorageTraversal.
	 */
	private static StorageTraversal getLocalTraversal(IResource resource) {
		if (resource instanceof IStorage) {
			return new StorageTraversal(Collections.singleton((IStorage)resource));
		} else {
			return new StorageTraversal(Collections.emptySet());
		}
	}
	
	// Remote

	@Override
	public SynchronizationModel resolveModels(IStorageProviderAccessor storageAccessor, IStorage left, IStorage right,
			IStorage origin, IProgressMonitor monitor) throws InterruptedException {
		var subMonitor = SubMonitor.convert(monitor, 3);
		return new SynchronizationModel(
				getRemoteTraversal(storageAccessor, left, DiffSide.SOURCE, subMonitor.split(1)),
				getRemoteTraversal(storageAccessor, right, DiffSide.REMOTE, subMonitor.split(1)),
				getRemoteTraversal(storageAccessor, origin, DiffSide.ORIGIN, subMonitor.split(1))
		);
	}

	/**
	 * For remote traversals, the IStorage given to us may be a wrapper for an underlying IStorage object that
	 * doesn't actually exist and is null; trying to load the contents of this IStorage results in an error.
	 * 
	 * To prevent this, try to locate the storage object ourselves from the
	 * storageAccessor using the given storage object's name to ensure it exists.
	 */
	private static StorageTraversal getRemoteTraversal(IStorageProviderAccessor storageAccessor, IStorage givenStorage, DiffSide side, IProgressMonitor monitor) {
		try {
			if (givenStorage != null) {
				var path = ResourceUtil.getAbsolutePath(givenStorage);
				var file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				var storageProvider = storageAccessor.getStorageProvider(file, side);
				if (storageProvider != null) {
					var foundStorage = storageProvider.getStorage(monitor);
					if (foundStorage != null) {
						return new StorageTraversal(Collections.singleton(foundStorage));
					}
				}
			}
		} catch (CoreException e) {
			// returns an empty traversal in the event of a CoreException
			e.printStackTrace();
		} finally {
			monitor.done();
		}
		return new StorageTraversal(Collections.emptySet());
	}
	
}
