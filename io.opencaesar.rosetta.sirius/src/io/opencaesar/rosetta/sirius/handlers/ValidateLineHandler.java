package io.opencaesar.rosetta.sirius.handlers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.impl.EValidatorRegistryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.ui.action.ValidateAction;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.sirius.table.metamodel.table.DLine;
import org.eclipse.sirius.table.metamodel.table.DTable;
import org.eclipse.sirius.viewpoint.DRepresentation;
import org.eclipse.sirius.viewpoint.description.JavaExtension;
import org.eclipse.sirius.viewpoint.description.Viewpoint;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;

import io.opencaesar.rosetta.sirius.MarkerRepresentationElementSelector;
import io.opencaesar.rosetta.sirius.validation.ValidationService;

/**
 * Validates a selected Sirius table line and all its sub-lines using
 * ValidationService instances registered in the table's viewpoint.
 */
public class ValidateLineHandler extends AbstractLineHandler {

	@Override
	protected void execute(ExecutionEvent event, List<DLine> lines) {
		if (!lines.isEmpty()) {
			// Map allows determining which representation element to show for a given semantic element.
			var semanticElementsToLine = new LinkedHashMap<EObject, DLine>();
			for (var line : lines) {
				collectSemanticElementsFromLineAndChildren(line, semanticElementsToLine);
			}
			var table = findContainerOfType(lines.get(0), DTable.class);
			var viewpoint = findContainerOfType(table.getDescription(), Viewpoint.class);
			var validationServices = viewpoint.getOwnedJavaExtensions().stream()
					.map(JavaExtension::getQualifiedClassName).map(ValidationService::getValidationService)
					.filter(s -> s != null).collect(Collectors.toList());
			var validateAction = new ValidateActionEx(validationServices, semanticElementsToLine);
			validateAction.setActiveWorkbenchPart(HandlerUtil.getActiveEditor(event));
			validateAction.run();
		}
	}

	/**
	 * Walk up EObject.eContainer() tree until an object of a given type is found, or the root is reached.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T findContainerOfType(EObject element, Class<T> type) {
		if (element != null) {
			if (type.isInstance(element)) {
				return (T) element;
			} else {
				return findContainerOfType(element.eContainer(), type);
			}
		}
		return null;
	}

	/**
	 * Recursively collect all semantic elements from a table line and all its children.
	 */
	private static void collectSemanticElementsFromLineAndChildren(DLine line, LinkedHashMap<EObject, DLine> semanticElementsToLine) {
		for (var semanticElement : line.getSemanticElements()) {
			semanticElementsToLine.put(semanticElement, line);
		}
		for (var subline : line.getLines()) {
			collectSemanticElementsFromLineAndChildren(subline, semanticElementsToLine);
		}
	}
	
	/**
	 * Extends EMF ValidateAction to use a customized EclipseResourcesUtil to configure
	 * markers so that MarkerRepresentationElementSelector can select elements with failed
	 * validation constraints and to use a custom EValidatorRegistry that delegates to
	 * a viewpoint's ValidationServices.
	 */
	private static class ValidateActionEx extends ValidateAction {
		private EValidatorRegistryImpl validatorRegistry = new EValidatorRegistryImpl();
		
		private ValidateActionEx(List<ValidationService> validationServices, Map<EObject, DLine> semanticElementsToLine) {
			// Ensure the EValidatorRegistry has a EValidator for the EPackage
			// of every validated object.
			var validator = new EValidatorImpl(validationServices);
			for (var semanticElement : semanticElementsToLine.keySet()) {
				validatorRegistry.put(semanticElement.eClass().getEPackage(), validator);
			}
			eclipseResourcesUtil = new EclipseResourcesUtilEx(semanticElementsToLine);
			updateSelection(new StructuredSelection(semanticElementsToLine.keySet().toArray()));
		}

		// This method is copied from EMF ValidateAction, modified to use a custom
		// EValidatorRegsitry instead of the default.
		protected Diagnostician createDiagnostician(final AdapterFactory adapterFactory,
				final IProgressMonitor progressMonitor) {
			final ResourceSet resourceSet = domain.getResourceSet();
			return new Diagnostician(validatorRegistry) {
				@Override
				public String getObjectLabel(EObject eObject) {
					if (adapterFactory != null && !eObject.eIsProxy()) {
						IItemLabelProvider itemLabelProvider = (IItemLabelProvider) adapterFactory.adapt(eObject,
								IItemLabelProvider.class);
						if (itemLabelProvider != null) {
							return itemLabelProvider.getText(eObject);
						}
					}

					return super.getObjectLabel(eObject);
				}

				@Override
				protected boolean doValidate(EValidator eValidator, EClass eClass, EObject eObject,
						DiagnosticChain diagnostics, Map<Object, Object> context) {
					progressMonitor.worked(1);
					Resource resource = eObject.eResource();
					if (resource == null) {
						synchronized (resourceSet) {
							return super.doValidate(eValidator, eClass, eObject, diagnostics, context);
						}
					} else {
						synchronized (resource) {
							synchronized (resourceSet) {
								return super.doValidate(eValidator, eClass, eObject, diagnostics, context);
							}
						}
					}
				}
			};
		}
	}

	/**
	 * EValidator implementation that delegates to ValidationServices
	 */
	private static class EValidatorImpl implements EValidator {
		private List<ValidationService> validationServices;
		
		private EValidatorImpl(List<ValidationService> validationServices) {
			this.validationServices = validationServices;
		}
		
		@Override
		public boolean validate(EObject eObject, DiagnosticChain diagnostics, Map<Object, Object> context) {
			var allOk = true;
			for (var validationService : validationServices) {
				allOk &= validationService.validate(eObject, diagnostics, context);
			}
			return allOk;
		}
		@Override
		public boolean validate(EClass eClass, EObject eObject, DiagnosticChain diagnostics,
				Map<Object, Object> context) {
			return validate(eObject, diagnostics, context);
		}
		@Override
		public boolean validate(EDataType eDataType, Object value, DiagnosticChain diagnostics,
				Map<Object, Object> context) {
			return true;
		}
	}
	
	/**
	 * EclipseResourcesUtil extension that includes information about selected
	 * representation elements in generated markers as well as binding it to the
	 * MarkerRepresentationElementSelector editor ID to allow opening a marker to
	 * take the user to the corresponding Sirius representation element.
	 */
	private static class EclipseResourcesUtilEx extends ValidateAction.EclipseResourcesUtil {
		private Map<EObject, DLine> semanticElementsToLine;
		
		EclipseResourcesUtilEx(Map<EObject, DLine> semanticElementsToLine) {
			this.semanticElementsToLine = semanticElementsToLine;
		}
		
		@Override
		protected void adjustMarker(IMarker marker, Diagnostic diagnostic, Diagnostic parentDiagnostic)
				throws CoreException {
			super.adjustMarker(marker, diagnostic, parentDiagnostic);
			var semanticElement = diagnostic.getData().stream().filter(o -> o instanceof EObject).findFirst()
					.orElse(null);
			if (semanticElement != null && semanticElementsToLine.containsKey(semanticElement)) {
				var line = semanticElementsToLine.get(semanticElement);
				EObject representation = line;
				while (representation != null && !(representation instanceof DRepresentation)) {
					representation = representation.eContainer();
				}
				if (representation != null) {
					marker.setAttribute(IDE.EDITOR_ID_ATTR, MarkerRepresentationElementSelector.EDITOR_ID);
					marker.setAttribute(MarkerRepresentationElementSelector.REPRESENTATION_URI, EcoreUtil.getURI(representation).toString());
					marker.setAttribute(MarkerRepresentationElementSelector.REPRESENTATION_ELEMENT_FRAGMENT, representation.eResource().getURIFragment(line));
					marker.setAttribute(MarkerRepresentationElementSelector.REPRESENTATION_NAME, ((DRepresentation)representation).getName());
				}
			}
		}
	}

}
