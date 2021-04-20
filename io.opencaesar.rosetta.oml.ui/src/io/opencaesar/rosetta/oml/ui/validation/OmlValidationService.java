package io.opencaesar.rosetta.oml.ui.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;
import java.util.function.Predicate;

import org.eclipse.emf.ecore.EObject;

import io.opencaesar.oml.Instance;
import io.opencaesar.oml.util.OmlSearch;
import io.opencaesar.rosetta.sirius.validation.ValidationService;

/**
 * ValidationService subclass with type predicates for OML Instances.
 * 
 * Applying the @OmlType or @OmlTypeIri annotations to a constraint
 * method's EObject parameter allows filtering the constraint by the abbreviated
 * or full type IRIs of the validated object respectively.
 */
public abstract class OmlValidationService extends ValidationService {

	/**
	 * Specifies an abbreviated type IRI to filter OML instances with
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface OmlType {
		String value();
	}

	/**
	 * Specifies a full type IRI to filter OML instances with
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	public @interface OmlTypeIri {
		String value();
	}
	
	/**
	 * Returns an appropriate predicate for a given Method parameter.
	 */
	@Override
	protected Predicate<EObject> getTypePredicate(Parameter parameter) {
		var typeAnnotation = parameter.getAnnotation(OmlType.class);
		var typeIriAnnotation = parameter.getAnnotation(OmlTypeIri.class);
		if (typeAnnotation != null && typeIriAnnotation != null) {
			throw new IllegalStateException("Parameter has both @OmlType and @OmlTypeIri annotations");
		}
		if (typeAnnotation != null) {
			return new OmlTypePredicate(typeAnnotation);
		}
		if (typeIriAnnotation != null) {
			return new OmlTypeIriPredicate(typeIriAnnotation);
		}
		return super.getTypePredicate(parameter);
	}

	/**
	 * Type predicate for filtering by full OML instance type IRIs
	 */
	private static class OmlTypeIriPredicate implements Predicate<EObject> {

		private String typeIri;

		public OmlTypeIriPredicate(OmlTypeIri annotation) {
			typeIri = annotation.value();
		}

		@Override
		public boolean test(EObject instance) {
			return instance instanceof Instance && OmlSearch.hasTypeIri((Instance) instance, typeIri);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((typeIri == null) ? 0 : typeIri.hashCode());
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
			OmlTypeIriPredicate other = (OmlTypeIriPredicate) obj;
			if (typeIri == null) {
				if (other.typeIri != null)
					return false;
			} else if (!typeIri.equals(other.typeIri))
				return false;
			return true;
		}

	}

	/**
	 * Type predicate for filtering by abbreviated OML instance type IRIs
	 */
	private static class OmlTypePredicate implements Predicate<EObject> {
		private String abbreviatedTypeIri;

		public OmlTypePredicate(OmlType annotation) {
			abbreviatedTypeIri = annotation.value();
		}

		@Override
		public boolean test(EObject instance) {
			return instance instanceof Instance
					&& OmlSearch.hasAbbreviatedTypeIri((Instance) instance, abbreviatedTypeIri);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((abbreviatedTypeIri == null) ? 0 : abbreviatedTypeIri.hashCode());
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
			OmlTypePredicate other = (OmlTypePredicate) obj;
			if (abbreviatedTypeIri == null) {
				if (other.abbreviatedTypeIri != null)
					return false;
			} else if (!abbreviatedTypeIri.equals(other.abbreviatedTypeIri))
				return false;
			return true;
		}

	}
}
