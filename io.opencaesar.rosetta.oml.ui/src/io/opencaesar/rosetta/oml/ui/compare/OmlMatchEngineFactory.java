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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.eclipse.emf.compare.match.DefaultComparisonFactory;
import org.eclipse.emf.compare.match.DefaultEqualityHelperFactory;
import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.compare.match.IComparisonFactory;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.match.eobject.IEObjectMatcher;
import org.eclipse.emf.compare.match.eobject.IdentifierEObjectMatcher;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryImpl;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import io.opencaesar.oml.Annotation;
import io.opencaesar.oml.Element;
import io.opencaesar.oml.Import;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.PropertyValueAssertion;
import io.opencaesar.oml.TypeAssertion;

public class OmlMatchEngineFactory extends MatchEngineFactoryImpl {

	private IMatchEngine matchEngine;
	
	@Override
	public IMatchEngine getMatchEngine() {
		if (matchEngine == null) {
			IEObjectMatcher fallBackMatcher = DefaultMatchEngine.createDefaultEObjectMatcher(UseIdentifiers.WHEN_AVAILABLE);
			IEObjectMatcher customIDMatcher = new IdentifierEObjectMatcher(fallBackMatcher, OmlMatchEngineFactory::getId);
			IComparisonFactory comparisonFactory = new DefaultComparisonFactory(new DefaultEqualityHelperFactory());
			matchEngine = new DefaultMatchEngine(customIDMatcher, comparisonFactory);
		}
		return matchEngine;
	}
	
	static String getId(EObject eObject) {
		if (eObject == null || eObject instanceof Ontology) {
			// Root of the tree
			return "$";
		}
		
		String baseId = getId(eObject.eContainer()) + " ";
		
		if (eObject instanceof Member) {
			Member member = (Member)eObject;
			if (member.getName() != null) {
				return baseId + "#" + escapeForIdString(member.getName());
			} else {
				return baseId + getIdInScope(Member.class, eObject, Member::resolve);
			}
		}
		if (eObject instanceof Literal) {
			// There is always one literal within a container (e.g. a ScalarPropertyValueAssertion)
			return baseId + "Literal";
		}

		// Objects identified by things they reference
		if (eObject instanceof Import) {
			return baseId + "Import " + escapeForIdString(((Import)eObject).getIri());
		}
		if (eObject instanceof Annotation) {
			return baseId + getIdInScope(Annotation.class, eObject, Annotation::getProperty);
		}
		if (eObject instanceof PropertyValueAssertion) {
			return baseId + getIdInScope(PropertyValueAssertion.class, eObject, PropertyValueAssertion::getProperty);
		}
		if (eObject instanceof TypeAssertion) {
			return baseId + getIdInScope(TypeAssertion.class, eObject, TypeAssertion::getType);
		}
		return baseId + "EObject " + eObject.eContainer().eContents().indexOf(eObject);
	}	

	/**
	 * Create a distinct ID for an object that is identified by a Member it references,
	 * for example a scalar property value assertion which is identified by the scalar
	 * property it references. If there are multiple sibling objects referencing the
	 * same identifying object, distinguish them by index.
	 * 
	 * Note we identify the referenced object only by URI fragment, not by the full URI.
	 * This is because at this point we don't have the normalized URI available. (If the
	 * full URI changes but the fragment is the same it will still show up as a difference
	 * in the end, but as a change on the same object rather than as different objects).
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Element> String getIdInScope(Class<T> type, EObject object, Function<T, Member> getIdentifyingObject) {
		Member identifiedBy = getIdentifyingObject.apply((T)object);
		String identifiedByFragment = identifiedBy != null ? EcoreUtil.getURI(identifiedBy).fragment() : null;
		int index = 0;
		for (EObject other : object.eContainer().eContents()) {
			if (other == object) {
				break;
			}
			if (type.isInstance(other)) {
				Member otherIdentifiedBy = getIdentifyingObject.apply((T)other);
				if (identifiedByFragment == null) {
					if (otherIdentifiedBy == null) {
						index++;
					}
				} else {
					String otherIdentifiedByFragment = otherIdentifiedBy != null ? EcoreUtil.getURI(otherIdentifiedBy).fragment() : null;
					if (identifiedByFragment.equals(otherIdentifiedByFragment)) {
						index++;
					}
				}
			}
		}
		return type.getSimpleName() + " " + escapeForIdString(identifiedByFragment) + " " + index;
	}

	/**
	 * Ensure the given string does not contain any spaces, URLEncoding it if it does.
	 */
	private static String escapeForIdString(String s) {
		try {
			if (s == null) {
				return "";
			}
			if (s.indexOf(' ') != -1) {
				return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
			}
			return "^" + s;
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}
}
