package io.opencaesar.rosetta.sirius.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EObject;

/**
 * Base class for Sirius-driven validation services
 * 
 * Adding a Java Extension to a Sirius viewpoint that extends this class
 * allows it to function as a validator for objects in that viewpoint.
 * 
 * Validation constraints are written as methods annotated with the
 * @Constraint annotation. Constraint methods must return a Result object
 * instance and must have 1 or 2 parameters, a required EObject parameter
 * and an optional context Map parameter.
 * 
 * By default, applicable constraints are filtered by the specific EObject
 * parameter type. Subclasses may expand this by overriding the getTypePredicate()
 * method.
 * 
 * Registration occurs when a ValidationService instance is instantiated
 * by the ValidationService constructor. Only one ValidationService is used
 * for each ValidationService class name; when a new ValidationService with
 * the same class name is created it replaces the old one.
 */
public abstract class ValidationService {
	
	/**
	 * Return type for all Constraint methods.
	 */
	public static class Result {
		
		public static final Result SUCCESS = new Result(true);
		
		/**
		 * Returns a "Success" result to indicate the constraint passed.
		 */
		public static Result success() {
			return SUCCESS;
		}
		
		/**
		 * Returns a failure result to indicate the constraint failed. The
		 * data value parameters are used to construct the error message for
		 * the constraint using a message format specified in the @Constraint
		 * annotation.
		 */
		public static Result failure(Object... data) {
			return new Result(false, data);
		}
		
		private boolean ok;
		private List<Object> data;
		
		private Result(boolean ok, Object... data) {
			this.ok = ok;
			this.data = Collections.unmodifiableList(Arrays.asList(data));
		}
		
		public boolean isSuccess() {
			return ok;
		}
		
		public List<Object> getData() {
			return data;
		}
		
		public String toString() {
			return (ok ? "Success":"Failure") + " " + data.stream().map(Object::toString).collect(Collectors.joining(", "));
		}
	}

	/**
	 * Annotation that indicates the annotated method is a validation constraint.
	 * 
	 * Constraint methods must additionally return a Result object and accept 1 or 2
	 * parameters, with 1 required EObject parameter and an optional Map context parameter.
	 * 
	 * The code and severity have the same meaning as in EMF Diagnostics. The message
	 * parameter is a MessageFormat pattern for constructing error messages if the constraint
	 * fails.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	protected @interface Constraint {
		int code();
		int severity();
		String message();
	}

	/**
	 * If a ValidationService exists for a given class name, return it; otherwise,
	 * return null.
	 */
	public static ValidationService getValidationService(String className) {
		var reference = instanceRegistry.get(className);
		if (reference != null) {
			return reference.get();
		} else {
			return null;
		}
	}

	/**
	 * Global mapping of ValidationService class names to instances. Set whenever a ValidationService
	 * is constructed.
	 */
	private static final Map<String, WeakReference<ValidationService>> instanceRegistry =
			new ConcurrentHashMap<>();
	
	/**
	 * Collect warnings about constraints that don't follow the required signature
	 * for display when a validation is run.
	 */
	private List<String> invalidConstraintWarnings = new ArrayList<>();
	
	/**
	 * Map of predicates to verify an EObject is of a given type to all
	 * the Constraint methods to run if the predicate is true.
	 */
	private Map<Predicate<EObject>, List<Method>> constraintsByTypePredicate = new HashMap<>();
	
	/**
	 * Constructor. Registers the constructed instance as the ValidationService
	 * instance for the ValidationService class name and identifies all constraint
	 * methods in the class.
	 */
	protected ValidationService() {
		instanceRegistry.put(getClass().getName(), new WeakReference<>(this));
		findMethods: for (var method : getClass().getDeclaredMethods()) {
			var constraintAnnotation = method.getAnnotation(Constraint.class);
			if (constraintAnnotation == null) {
				continue;
			}
			if (method.getReturnType() != Result.class) {
				invalidConstraintWarnings.add("Ignoring constraint " + getClass().getSimpleName() + "." + method.getName() + " because the method doesn't return a Result object.");
				continue;
			}
			Parameter objectParameter = null;
			Parameter contextParameter = null;
			for (var parameter : method.getParameters()) {
				if (EObject.class.isAssignableFrom(parameter.getType())) {
					if (objectParameter != null) {
						invalidConstraintWarnings.add("Ignoring constraint " + getClass().getSimpleName() + "." + method.getName() + " because because the method accepts more than one EObject parameter.");
						continue findMethods;
					}
					objectParameter = parameter;
				} else if (Map.class.equals(parameter.getType())) {
					if (contextParameter != null) {
						invalidConstraintWarnings.add("Ignoring constraint " + getClass().getSimpleName() + "." + method.getName() + " because the method has more than one context Map parameter.");
						continue findMethods;
					}
					contextParameter = parameter;
				} else {
					invalidConstraintWarnings.add("Ignoring constraint " + getClass().getSimpleName() + "." + method.getName() + " because the method accepts an unrecognized parameter.");
					continue findMethods;
				}
			}
			if (objectParameter == null) {
				invalidConstraintWarnings.add("Ignoring constraint " + getClass().getSimpleName() + "." + method.getName() + " because the method does not accept an EObject paramter.");
			}
			method.setAccessible(true);
			constraintsByTypePredicate.computeIfAbsent(getTypePredicate(objectParameter), k -> new ArrayList<>()).add(method);
		}
	}

	/**
	 * For a given constraint method parameter, return a Predicate that determines if
	 * a the constraint method is applicable to the EObject type.
	 * 
	 * The base implementation compares the object type with the formal
	 * parameter type; subclasses can implement this for alternate
	 * behaviors.
	 * 
	 * Predicates should have hashCode and equals methods to allow determining
	 * if two predicates are logically equivalent.
	 */
	protected Predicate<EObject> getTypePredicate(Parameter parameter) {
		return new JavaClassPredicate(parameter.getType());
	}
	
	/**
	 * Default type predicate that compares Java classes
	 */
	private static class JavaClassPredicate implements Predicate<EObject> {
		private Class<?> type;
		private JavaClassPredicate(Class<?> type) {
			this.type = type;
		}
		
		@Override
		public boolean test(EObject t) {
			return type.isInstance(t);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			JavaClassPredicate other = (JavaClassPredicate) obj;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}
	}
	
	/**
	 * Validate an object using this instance's Constraint methods.
	 */
	@SuppressWarnings("unchecked")
	public boolean validate(EObject object, DiagnosticChain chain, Map<Object, Object> context) {
		// Add invalid constraint warnings once each time a validation is run.
		if (context.put(ValidationService.class.getName() + ".invalidConstraintWarningsAdded", true) == null) {
			for (var warning : invalidConstraintWarnings) {
				chain.add(new BasicDiagnostic(Diagnostic.WARNING, ValidationService.class.getName(), 1, warning, new Object[] { object }));
			}
		}
		// Identify applicable constraint methods by applying the typePredicate to the validated object.
		var applicableMethods = new LinkedHashSet<Method>();
		for (var e : constraintsByTypePredicate.entrySet()) {
			if (e.getKey().test(object)) {
				applicableMethods.addAll(e.getValue());
			}
		}
		// Run validation for each constraint method.
		boolean allOk = true;
		for (var method : applicableMethods) {
			try {
				// Bind parameters and invoke method
				var thisValue = Modifier.isStatic(method.getModifiers()) ? null : this;
				var params = method.getParameters();
				var paramValues = new Object[params.length];
				for (var i = 0; i < params.length; i++) {
					if (params[i].getType() == Map.class) {
						paramValues[i] = context;
					} else if (EObject.class.isAssignableFrom(params[i].getType())) {
						paramValues[i] = object;
					} else {
						throw new IllegalStateException("Unrecognized parameter type " + i);
					}
				}
				var result = (Result) method.invoke(thisValue, paramValues);
				// Only create a Diagnostic if the result is failed.
				if (!result.ok) {
					allOk = false;
					var annotation = method.getAnnotation(Constraint.class);
					chain.add(new BasicDiagnostic(annotation.severity(), method.getDeclaringClass().getName(), annotation.code(), MessageFormat.format(annotation.message(), result.data.toArray()), new Object[] { object }));
				}
			} catch (Throwable ex) {
				// In the event of an Exception, treat it as an Error status.
				ex.printStackTrace();
				allOk = false;
				Set<Object> throwing = (Set<Object>) context.get(ValidationService.class.getName() + ".throwingMethods");
				if (throwing == null) {
					context.put(ValidationService.class.getName() + ".throwingMethods", throwing = new HashSet<>());
				}
				if (ex instanceof InvocationTargetException && ex.getCause() != null) {
					ex = ex.getCause();
				}
				if (throwing.add(method)) {
					chain.add(new BasicDiagnostic(Diagnostic.ERROR, ValidationService.class.getName(), 1, "Constraint " + method.getDeclaringClass().getSimpleName() + "." + method.getName() + " threw " + ex.getClass().getName() + ": " + ex.getMessage(), new Object[] { object }));
				}
				continue;
			}
		}
		return allOk;
	}
	
}
