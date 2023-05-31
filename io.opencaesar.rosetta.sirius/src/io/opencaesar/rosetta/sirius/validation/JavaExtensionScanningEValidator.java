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
package io.opencaesar.rosetta.sirius.validation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.sirius.common.tools.api.interpreter.ClassLoadingCallback;
import org.eclipse.sirius.common.tools.api.interpreter.JavaExtensionsManager;
import org.eclipse.sirius.common.tools.internal.interpreter.ClassLoadingService;
import org.eclipse.sirius.viewpoint.description.Viewpoint;

import io.opencaesar.oml.Classifier;
import io.opencaesar.oml.Instance;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;

/**
 * EValidator that loads constraints from Java extensions registered with a Sirius viewpoint
 * 
 * Constraints are defined as methods annotated with {@link Constraint} in a class annotated
 * with {@link ValidationService}. Valid constraints methods accept a required {@link EObject}
 * subclass parameter (the object to validate), and optionally may accept a context {@link Map}
 * parameter.
 * 
 * Applicable constraints are filtered by the specified {@link EObject} parameter type; OML
 * {@link Instance} parameters may be further filtered by OML type using the {@link TypeIri}
 * or {@link AbbreviatedTypeIri} which specify the abbreviated or full type IRI respectively. 
 */
public class JavaExtensionScanningEValidator implements EValidator {
	
	/**
	 * Warning messages to add to diagnostics when a class or constraint method
	 * could not be loaded.
	 */
	private Set<String> invalidConstraintWarnings = new LinkedHashSet<>();
	
	/**
	 * Maps a predicate used to filter an object to validate to all constraint handlers
	 * to run if the predicate is true.
	 */
	private Map<Predicate<EObject>, ArrayList<ConstraintHandler>> handlers = new LinkedHashMap<>();
	
	/**
	 * Constructor.
	 * 
	 * Locates all constraint methods annotated with {@link Constraint} in validation service
	 * classes annotated with {@link ValidationService}.
	 */
	@SuppressWarnings("restriction")
	public JavaExtensionScanningEValidator(Viewpoint viewpoint) {
		// Load Viewpoint Java Extension Classes
		var validationServices = new LinkedHashMap<String, Class<?>>();
		var extensionsManager = new JavaExtensionsManager();
		try {
			extensionsManager.setClassLoadingOverride(ClassLoadingService.getClassLoading());
			extensionsManager.addClassLoadingCallBack(new ClassLoadingCallback() {
				@Override
				public void loaded(String className, Class<?> classObject) {
					if (classObject.getAnnotation(ValidationService.class) != null) {
						validationServices.put(className, classObject);
					}
				}
				@Override
				public void notFound(String className) {
					validationServices.put(className, null);
				}
				@Override
				public void unloaded(String className, Class<?> classObject) {
					// Not applicable since we are just loading extension classes at a single point in time
				}
			});
			for (var javaExtension : viewpoint.getOwnedJavaExtensions()) {
				extensionsManager.addImport(javaExtension.getQualifiedClassName());
			}
			var viewpointResourceUri = viewpoint.eResource().getURI();
			if (viewpointResourceUri.isPlatformPlugin()) {
				extensionsManager.updateScope(new LinkedHashSet<>(List.of(viewpointResourceUri.segment(1))), new LinkedHashSet<>());
			} else if (viewpointResourceUri.isPlatformResource()) {
				extensionsManager.updateScope(new LinkedHashSet<>(), new LinkedHashSet<>(List.of(viewpointResourceUri.segment(1))));
			}
			extensionsManager.reloadIfNeeded();
		} finally {
			extensionsManager.dispose();
		}
		
		// Instantiate Viewpoint Service Classes and reflectively find constraint methods
		for (var nameAndClass : validationServices.entrySet()) {
			var validationServiceClass = nameAndClass.getValue();
			if (validationServiceClass == null) {
				invalidConstraintWarnings.add("Unable to locate Java extension class " + nameAndClass.getKey());
				continue;
			}
			Object validationService = null;
			try {
				var constructor = validationServiceClass.getDeclaredConstructor();
				constructor.setAccessible(true);
				validationService = constructor.newInstance();
			} catch (Throwable e) {
				if (e instanceof InvocationTargetException && e.getCause() != null) {
					e = e.getCause();
				}
				e.printStackTrace();
				invalidConstraintWarnings.add("Unable to instantiate validation service class " + validationServiceClass.getName() + ": " + e.getClass().getName() + ": " + e.getMessage());
				continue;
			}
			findMethods: for (var method : validationServiceClass.getDeclaredMethods()) {
				var constraintAnnotation = method.getAnnotation(Constraint.class);
				if (constraintAnnotation == null) {
					continue;
				}
				if (method.getReturnType() != Result.class) {
					invalidConstraintWarnings.add("Ignoring constraint " + validationServiceClass.getSimpleName() + "." + method.getName() + " because the method doesn't return a Result object.");
					continue;
				}
				Predicate<EObject> objectPredicate = null;
				boolean hasContextParameter = false;
				for (var parameter : method.getParameters()) {
					if (EObject.class.isAssignableFrom(parameter.getType())) {
						if (objectPredicate != null) {
							invalidConstraintWarnings.add("Ignoring constraint " + getClass().getSimpleName() + "." + method.getName() + " because because the method accepts more than one EObject parameter.");
							continue findMethods;
						}
						var typeIriAnnotation = parameter.getAnnotation(TypeIri.class);
						var abbreviatedTypeIriAnnotation = parameter.getAnnotation(AbbreviatedTypeIri.class);
						if (typeIriAnnotation != null && abbreviatedTypeIriAnnotation != null) {
							invalidConstraintWarnings.add("Ignoring constraint " + getClass().getSimpleName() + "." + method.getName() + " because the EObject parameter has both @TypeIri and @AbbreviatedTypeIri annotations.");
							continue findMethods;
						} else if (typeIriAnnotation != null) {
							if (!Instance.class.isAssignableFrom(parameter.getType())) {
								invalidConstraintWarnings.add("Ignoring constraint " + getClass().getSimpleName() + "." + method.getName() + " becuse the parameter annotated with @TypeIri is not an Instance parameter");
								continue findMethods;
							}
							objectPredicate = new TypeIriPredicate(parameter.getType(), typeIriAnnotation.value());
						} else if (abbreviatedTypeIriAnnotation != null) {
							if (!Instance.class.isAssignableFrom(parameter.getType())) {
								invalidConstraintWarnings.add("Ignoring constraint " + getClass().getSimpleName() + "." + method.getName() + " becuse the parameter annotated with @AbbreviatedTypeIri is not an Instance parameter");
								continue findMethods;
							}
							objectPredicate = new AbbreviatedTypeIriPredicate(parameter.getType(), abbreviatedTypeIriAnnotation.value());
						} else {
							objectPredicate = new IsInstancePredicate(parameter.getType());
						}
					} else if (Map.class.equals(parameter.getType())) {
						if (hasContextParameter) {
							invalidConstraintWarnings.add("Ignoring constraint " + getClass().getSimpleName() + "." + method.getName() + " because the method has more than one context Map parameter.");
							continue findMethods;
						}
						hasContextParameter = true;
					} else {
						invalidConstraintWarnings.add("Ignoring constraint " + getClass().getSimpleName() + "." + method.getName() + " because the method accepts an unrecognized parameter " + parameter.getName() + ".");
						continue findMethods;
					}
				}
				if (objectPredicate == null) {
					invalidConstraintWarnings.add("Ignoring constraint " + getClass().getSimpleName() + "." + method.getName() + " because the method does not accept an EObject paramter.");
				}
				method.setAccessible(true);
				handlers.computeIfAbsent(objectPredicate, k -> new ArrayList<>()).add(new ConstraintHandler(validationService, method));
			}
		}
	}
	
	@Override
	public boolean validate(EObject eObject, DiagnosticChain diagnostics, Map<Object, Object> context) {
		if (context.put(getClass().getName() + ".invalidConstraintWarningsAdded", true) == null) {
			for (var warning : invalidConstraintWarnings) {
				diagnostics.add(new BasicDiagnostic(Diagnostic.WARNING, getClass().getName(), 1, warning, new Object[] { }));
			}
		}
		var allOk = true;
		for (var predicateAndHandlers : handlers.entrySet()) {
			if (!predicateAndHandlers.getKey().test(eObject)) {
				continue;
			}
			for (var handler : predicateAndHandlers.getValue()) {
				try {
					allOk &= handler.validate(eObject, diagnostics, context);
				} catch (Throwable e) {
					e.printStackTrace();
					allOk = false;
					if (e instanceof InvocationTargetException && e.getCause() != null) {
						e = e.getCause();
					}
					diagnostics.add(new BasicDiagnostic(Diagnostic.ERROR, getClass().getName(), 2, "Constraint " + handler.method.getDeclaringClass().getSimpleName() + "." + handler.method.getName() + " threw " + e.getClass().getName() + ": " + e.getMessage(), new Object[] { eObject }));
				}
			}
		}
		return allOk;
	}

	@Override
	public boolean validate(EClass eClass, EObject eObject, DiagnosticChain diagnostics, Map<Object, Object> context) {
		return validate(eObject, diagnostics, context);
	}

	@Override
	public boolean validate(EDataType eDataType, Object value, DiagnosticChain diagnostics,
			Map<Object, Object> context) {
		return true;
	}
	
	/**
	 * Runs a constraint method and creates diagnostics based on the returned {@link Result} object.
	 */
	private static class ConstraintHandler {
		private Object thisValue;
		private Method method;
		ConstraintHandler(Object thisValue, Method method) {
			this.thisValue = thisValue;
			this.method = method;
		}
		private boolean validate(EObject eObject, DiagnosticChain diagnostics, Map<Object, Object> context) throws Exception {
			var params = method.getParameters();
			var paramValues = new Object[params.length];
			for (var i = 0; i < params.length; i++) {
				if (params[i].getType() == Map.class) {
					paramValues[i] = context;
				} else if (EObject.class.isAssignableFrom(params[i].getType())) {
					paramValues[i] = eObject;
				} else {
					throw new IllegalStateException("Unrecognized parameter type " + i);
				}
			}
			var result = (Result) method.invoke(thisValue, paramValues);
			// Only create a Diagnostic if the result is failed.
			if (!result.isSuccess()) {
				var annotation = method.getAnnotation(Constraint.class);
				diagnostics.add(new BasicDiagnostic(annotation.severity(), method.getDeclaringClass().getName(), annotation.code(), MessageFormat.format(annotation.message(), result.getData().toArray()), new Object[] { eObject }));
			}
			return result.isSuccess();
		}
	}

	/**
	 * Default type predicate that checks if an object is an instance of the formal parameter type.
	 */
	private static class IsInstancePredicate implements Predicate<EObject> {
		private Class<?> type;
		private IsInstancePredicate(Class<?> type) {
			this.type = type;
		}
		
		@Override
		public boolean test(EObject t) {
			return type.isInstance(t);
		}

		@Override
		public int hashCode() {
			return type.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return obj != null
					&& obj.getClass() == getClass()
					&& ((IsInstancePredicate)obj).type.equals(type);
		}
	}

	/**
	 * Type predicate for parameters annotated by {@link TypeIri} to filter by the full OML {@link Instance} type IRI.
	 */
	private static class TypeIriPredicate implements Predicate<EObject> {

		private Class<?> javaType;
		private String typeIri;

		public TypeIriPredicate(Class<?> javaType, String typeIri) {
			if (!Instance.class.isAssignableFrom(javaType)) {
				throw new IllegalArgumentException("Not an instance type: " + javaType);
			}
			this.javaType = javaType;
			this.typeIri = typeIri;
		}

		@Override
		public boolean test(EObject instance) {
			if (javaType.isInstance(instance)) {
				var type = OmlRead.getMemberByIri(((Instance)instance).getOntology(), typeIri);
				return type instanceof Classifier && OmlSearch.findIsOfKind((Instance) instance, (Classifier) type);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(javaType, typeIri);
		}

		@Override
		public boolean equals(Object other) {
			return other != null
					&& other.getClass() == getClass()
					&& javaType.equals(((TypeIriPredicate)other).javaType)
					&& typeIri.equals(((TypeIriPredicate)other).typeIri);
		}
	}

	/**
	 * Type predicate for parameters annotated by {@link AbbreviatedTypeIri} to filter by the abbreviated OML {@link Instance} type IRI.
	 */
	private static class AbbreviatedTypeIriPredicate implements Predicate<EObject> {
		private Class<?> javaType;
		private String abbreviatedTypeIri;

		public AbbreviatedTypeIriPredicate(Class<?> javaType, String abbreviatedTypeIri) {
			if (!Instance.class.isAssignableFrom(javaType)) {
				throw new IllegalArgumentException("Not an instance type: " + javaType);
			}
			this.javaType = javaType;
			this.abbreviatedTypeIri = abbreviatedTypeIri;
		}

		@Override
		public boolean test(EObject instance) {
			if (javaType.isInstance(instance)) {
				var type = OmlRead.getMemberByAbbreviatedIri(((Instance)instance).getOntology(), abbreviatedTypeIri);
				return type instanceof Classifier && OmlSearch.findIsOfKind((Instance) instance, (Classifier) type);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(javaType, abbreviatedTypeIri);
		}

		@Override
		public boolean equals(Object other) {
			return other != null
					&& other.getClass() == getClass()
					&& javaType.equals(((AbbreviatedTypeIriPredicate)other).javaType)
					&& abbreviatedTypeIri.equals(((AbbreviatedTypeIriPredicate)other).abbreviatedTypeIri);
		}
	}

}
