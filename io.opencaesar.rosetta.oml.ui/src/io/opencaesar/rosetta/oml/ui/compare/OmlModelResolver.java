package io.opencaesar.rosetta.oml.ui.compare;

import java.util.Collections;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.compare.ide.ui.logical.AbstractModelResolver;
import org.eclipse.emf.compare.ide.ui.logical.IStorageProviderAccessor;
import org.eclipse.emf.compare.ide.ui.logical.SynchronizationModel;
import org.eclipse.emf.compare.ide.utils.StorageTraversal;

/**
 * Model resolver that doesn't really do any resolving. Instead it provides a
 * SynchronizationModel with only the resources the user selected for comparison.
 *
 */
public class OmlModelResolver extends AbstractModelResolver {

	@Override
	public SynchronizationModel resolveLocalModels(IResource left, IResource right, IResource origin,
			IProgressMonitor monitor) throws InterruptedException {
		return new SynchronizationModel(getTraversal(left), getTraversal(right), getTraversal(origin));
	}

	@Override
	public SynchronizationModel resolveModels(IStorageProviderAccessor storageAccessor, IStorage left, IStorage right,
			IStorage origin, IProgressMonitor monitor) throws InterruptedException {
		return new SynchronizationModel(getTraversal(left), getTraversal(right), getTraversal(origin));
	}

	@Override
	public StorageTraversal resolveLocalModel(IResource resource, IProgressMonitor monitor)
			throws InterruptedException {
		return getTraversal(resource);
	}

	@Override
	public boolean canResolve(IStorage sourceStorage) {
		return true;
	}

	private static StorageTraversal getTraversal(Object resource) {
		if (resource instanceof IStorage) {
			return new StorageTraversal(Collections.singleton((IStorage)resource));
		} else {
			return new StorageTraversal(Collections.emptySet());
		}
	}
}
