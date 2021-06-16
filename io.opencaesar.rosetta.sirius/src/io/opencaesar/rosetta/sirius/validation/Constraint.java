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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that indicates the annotated method is a validation constraint.
 * 
 * Constraint methods must take a required parameter of an EObject subclass and an optional
 * Map context parameter, and must return a {@link Result}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Constraint {
	
	/**
	 * {@link org.eclipse.emf.common.util.Diagnostic} code. Should be unique within a validation service class.
	 */
	int code();
	
	/**
	 * {@link org.eclipse.emf.common.util.Diagnostic} severity.
	 */
	int severity();
	
	/**
	 * Error message to include in generated {@link org.eclipse.emf.common.util.Diagnostic} objects. Messages
	 * are formatted with {@link java.text.MessageFormat} with the data objects returned in the @{link Result}
	 * object.
	 */
	String message();
}