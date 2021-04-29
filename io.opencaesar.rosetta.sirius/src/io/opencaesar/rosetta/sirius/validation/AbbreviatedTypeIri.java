package io.opencaesar.rosetta.sirius.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation applied to a constraint method (annotated by {@link Constraint}) indicating
 * the abbreviated OML {@link Instance} type IRI a constraint applies to.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AbbreviatedTypeIri {
	String value();
}
