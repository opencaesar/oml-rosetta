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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.Monitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.CompareFactory;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.MatchResource;
import org.eclipse.emf.compare.match.DefaultComparisonFactory;
import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.compare.match.IComparisonFactory;
import org.eclipse.emf.compare.match.eobject.IEObjectMatcher;
import org.eclipse.emf.compare.match.eobject.IdentifierEObjectMatcher;
import org.eclipse.emf.compare.match.resource.IResourceMatcher;
import org.eclipse.emf.compare.match.resource.LocationMatchingStrategy;
import org.eclipse.emf.compare.match.resource.StrategyResourceMatcher;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.utils.EqualityHelper;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;

import io.opencaesar.oml.Annotation;
import io.opencaesar.oml.ConceptInstanceReference;
import io.opencaesar.oml.ConceptTypeAssertion;
import io.opencaesar.oml.Element;
import io.opencaesar.oml.Import;
import io.opencaesar.oml.LinkAssertion;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.RelationInstanceReference;
import io.opencaesar.oml.ScalarPropertyValueAssertion;
import io.opencaesar.oml.StructuredPropertyValueAssertion;
import io.opencaesar.oml.util.OmlRead;

/**
 * Match Engine with a customized IEObjectMatcher, ResourceMatcher, and IComparisonFactory. Designed
 * to work with OmlModelResolver.
 */
public class OmlMatchEngine extends DefaultMatchEngine {
	
	/**
	 * Cache of references that may point to proxy objects, used to attach absolute URIs to proxy references.
	 */
	private Map<EClass, Collection<EReference>> possibleProxyReferencesByEClass = new HashMap<>();

	private static final IEObjectMatcher OBJECT_MATCHER = new IdentifierEObjectMatcher(OmlMatchEngine::getId);
	
	private static final IResourceMatcher RESOURCE_MATCHER = new StrategyResourceMatcher(Arrays.asList(
			OmlMatchEngine::matchResources,
			new LocationMatchingStrategy()
		));
	
	private static final IComparisonFactory COMPARISON_FACTORY = new DefaultComparisonFactory(AbsoluteUriComparingEqualityHelper::new);
	
	OmlMatchEngine() {
		super(OBJECT_MATCHER, RESOURCE_MATCHER, COMPARISON_FACTORY);
	}
	
	/**
	 * Tries to attach absolute URIs to all matched proxy objects to allow
	 * AbsoluteUriComparingEqualityHelper to compare objects by their full
	 * URI instead of just the URI fragment. In OML, URI fragments are only
	 * unique within the scope of their files.
	 */
	@Override
	public Comparison match(IComparisonScope scope, Monitor monitor) {
		Comparison comparison = super.match(scope, monitor);
		for (MatchResource matchedResource : comparison.getMatchedResources()) {
			attachAbsoluteUrisToProxies(matchedResource);
		}
		return comparison;
	}
	
	/**
	 * Locate all proxy objects in a matched resource and attempt to attach
	 * absolute URIs to all of them to allow for comparisons by absolute URI
	 * instead of by fragment.
	 */
	private void attachAbsoluteUrisToProxies(MatchResource matchedResource) {
		List<Resource> matched = new ArrayList<Resource>(3);
		if (matchedResource.getLeft() != null) matched.add(matchedResource.getLeft());
		if (matchedResource.getRight() != null) matched.add(matchedResource.getRight());
		if (matchedResource.getOrigin() != null) matched.add(matchedResource.getOrigin());
		
		for (Resource resource : matched) {
			URI baseUri = guessAbsoluteUri(resource, matched);
			resource.getAllContents().forEachRemaining(contained -> {
				
				Collection<EReference> possibleProxyReferences = this.possibleProxyReferencesByEClass.computeIfAbsent(contained.eClass(), eClass -> {
					Collection<EReference> references = new ArrayList<EReference>();
					for (EReference reference : eClass.getEAllReferences()) {
						if (!reference.isContainer() && !reference.isContainment()) {
							references.add(reference);
						}
					}
					return references;
				});
				
				EObject referenceSource = (EObject)contained;
				for (EReference reference : possibleProxyReferences) {
					Object value = referenceSource.eGet(reference);
					if (value == null) continue;
					if (value instanceof EObject) {
						AbsoluteUri.attachIfProxy((EObject)value, baseUri);
					} else if (value instanceof Collection<?>) {
						for (Object element : (Collection<?>)value) {
							AbsoluteUri.attachIfProxy((EObject)element, baseUri);
						}
					}
				}
			});
		}
	}
	
	/**
	 * Guess the absolute URI of a resource (using its own URI if it is already absolute)
	 * based on the URIs of the other resources it was matched with.
	 */
	private static URI guessAbsoluteUri(Resource resource, Collection<Resource> matched) {
		if (resource.getURI().scheme() != null) {
			// If the resource URI has a scheme, it's already absolute.
			return resource.getURI();
		}
		// IF the resource doesn't have an absolute URI, find one that that does
		// and use its ontology IRI to guess where this resource exists relative to it,
		// assuming that the file path corresponds to the ontology IRI in both cases.
		Resource baseResource = matched.stream().filter(r -> r.getURI().scheme() != null).findFirst().orElse(null);
		if (baseResource == null) {
			// If none of the resources have an absolute URI (probably because they are all git resources) then resolve relative to a fake base URI.
			return resource.getURI().resolve(URI.createURI("file:///$/"));
		}
		Ontology baseOntology = OmlRead.getOntology(baseResource);
		Ontology thisOntology = OmlRead.getOntology(resource);
		// If we can't find a correspondence, fall back to using the absolute URI of the matched resource.
		if (thisOntology == null || thisOntology.getIri() == null || baseOntology == null || baseOntology.getIri() == null || !thisOntology.getIri().contains("://") || !baseOntology.getIri().contains("://")) {
			return baseResource.getURI();
		}
		// Assume the file path contains the domain and all path segments excluding the file extension.
		// For example a file at resource://project/src/www.example.com/namespace/ontology.oml will have an ontology IRI of http://www.example.com/namespace/ontology
		// If it doesn't hold for either, fall back to the the absolute URI of the matched resource.
		String baseResourceUriWithoutExtension = baseResource.getURI().trimFileExtension().toString();
		String baseIriWithoutScheme = baseOntology.getIri().substring(baseOntology.getIri().indexOf("://"));
		if (!baseResourceUriWithoutExtension.endsWith(baseIriWithoutScheme)) {
			return baseResource.getURI();
		}
		String thisResourceUriWithoutExtension = resource.getURI().trimFileExtension().toString();
		String thisIriWithoutScheme = thisOntology.getIri().substring(thisOntology.getIri().indexOf("://"));
		if (!thisResourceUriWithoutExtension.endsWith(thisIriWithoutScheme)) {
			return baseResource.getURI();
		}
		// Get the absolute base before the ontology IRI is part of the path
		// (resource://project/src/) in the example above and replace the end
		// with thisIriWithoutScheme 
		String commonAbsoluteBase = baseResourceUriWithoutExtension.substring(0, baseResourceUriWithoutExtension.length() - baseIriWithoutScheme.length());
		return URI.createURI(commonAbsoluteBase + thisIriWithoutScheme + "." + resource.getURI().fileExtension());
	}
	
	/**
	 * Determine an ID for every object in an OML file.
	 */
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
			}
		}
		if (eObject instanceof Literal) {
			// There is always one literal within a container (e.g. a ScalarPropertyValueAssertion)
			return baseId + "Literal";
		}

		// Objects identified by things they reference
		if (eObject instanceof Import) {
			return "Import " + escapeForIdString(((Import)eObject).getIri());
		}
		if (eObject instanceof Annotation) {
			return baseId + getIdInScope(Annotation.class, eObject, Annotation::getProperty);
		}
		if (eObject instanceof ScalarPropertyValueAssertion) {
			return baseId + getIdInScope(ScalarPropertyValueAssertion.class, eObject, ScalarPropertyValueAssertion::getProperty);
		}
		if (eObject instanceof StructuredPropertyValueAssertion) {
			return baseId + getIdInScope(StructuredPropertyValueAssertion.class, eObject, StructuredPropertyValueAssertion::getProperty);
		}
		if (eObject instanceof LinkAssertion) {
			return baseId + getIdInScope(LinkAssertion.class, eObject, LinkAssertion::getRelation);
		}
		if (eObject instanceof ConceptTypeAssertion) {
			return baseId + getIdInScope(ConceptTypeAssertion.class, eObject, ConceptTypeAssertion::getType);
		}
		if (eObject instanceof ConceptInstanceReference) {
			return baseId + getIdInScope(ConceptInstanceReference.class, eObject, ConceptInstanceReference::getInstance);
		}
		if (eObject instanceof RelationInstanceReference) {
			return baseId + getIdInScope(RelationInstanceReference.class, eObject, RelationInstanceReference::getInstance);
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
	
	/**
	 * Adapter that attaches an absolute URI to a proxy EObject
	 */
	static class AbsoluteUri extends AdapterImpl {
		URI uri;
		
		static void attachIfProxy(EObject proxy, URI baseUri) {
			if (!proxy.eIsProxy()) {
				return;
			}
			AbsoluteUri attachment = new AbsoluteUri();
			URI targetUri = EcoreUtil.getURI(proxy);
			if (targetUri.hasRelativePath()) {
				attachment.uri = targetUri.resolve(baseUri);
			} else {
				attachment.uri = targetUri;
			}
			proxy.eAdapters().add(attachment);
		}
		
		static URI get(EObject object) {
			if (object.eIsProxy()) {
				for (Adapter adapter : object.eAdapters()) {
					if (adapter instanceof AbsoluteUri) {
						return ((AbsoluteUri)adapter).uri;
					}
				}
			}
			return EcoreUtil.getURI(object);
		}
		
		@Override
		public boolean isAdapterForType(Object type) {
			return type == AbsoluteUri.class;
		}
	}
	
	/**
	 * EqualityHelper that compares proxy objects by absolute URI instead
	 * of just the URI fragment
	 */
	static class AbsoluteUriComparingEqualityHelper extends EqualityHelper {
	
		private LoadingCache<EObject, URI> cache = CacheBuilder
				.newBuilder()
				.maximumSize(DefaultMatchEngine.DEFAULT_EOBJECT_URI_CACHE_MAX_SIZE)
				.build(CacheLoader.from(AbsoluteUri::get));
		
		@SuppressWarnings("deprecation")
		public AbsoluteUriComparingEqualityHelper() {
			super();
		}
				
		@Override
		public boolean matchingEObjects(EObject object1, EObject object2) {
			if (object1.eIsProxy() || object2.eIsProxy()) {
				return matchingURIs(object1, object2);
			} else {
				return super.matchingEObjects(object1, object2);
			}
		}
		
		@Override
		public boolean matchingURIs(EObject object1, EObject object2) {
			if (!object1.eIsProxy() && isUncontained(object1) || !object2.eIsProxy() && isUncontained(object2)) {
				return false;
			}
			URI uri1 = cache.getUnchecked(object1);
			URI uri2 = cache.getUnchecked(object2);
			return uri1.equals(uri2);
		}
		
		private static boolean isUncontained(EObject o) {
			return o.eContainer() == null && o.eResource() == null;
		}
		
	}
	
	/**
	 * Match resources. If all sides have just one resource then the single resource on each side
	 * is matched, otherwise resources are matched by their ontology IRI.
	 */
	private static List<MatchResource> matchResources(Iterable<? extends Resource> left, Iterable<? extends Resource> right, Iterable<? extends Resource> origin) {
		Resource[] leftArray = Iterables.toArray(left, Resource.class);
		Resource[] rightArray = Iterables.toArray(right, Resource.class);
		Resource[] originArray = Iterables.toArray(origin, Resource.class);
		
		// If only one resource is on each side, directly match them
		boolean hasSingleMatch =
				leftArray.length == 1 && rightArray.length == 1 ||
				leftArray.length == 1 && originArray.length == 1 ||
				rightArray.length == 1 && originArray.length == 1;
		if (hasSingleMatch) {
			MatchResource match = CompareFactory.eINSTANCE.createMatchResource();
			if (leftArray.length == 1) match.setLeft(leftArray[0]);
			if (rightArray.length == 1) match.setRight(rightArray[0]);
			if (originArray.length == 1) match.setOrigin(originArray[0]);
			return Collections.singletonList(match);
		}
		
		// Otherwise attempt to match by ontology IRI
		Set<String> allOntologyIris = new HashSet<>();
		Map<String, Resource> leftResourcesByOntologyIri = createOntologyIriToResourceMap(allOntologyIris, leftArray);
		Map<String, Resource> rightResourcesByOntologyIri = createOntologyIriToResourceMap(allOntologyIris, rightArray);
		Map<String, Resource> originResourcesByOntologyIri = createOntologyIriToResourceMap(allOntologyIris, originArray);
		List<MatchResource> matches = new ArrayList<MatchResource>();
		for (String ontologyIri : allOntologyIris) {
			MatchResource match = CompareFactory.eINSTANCE.createMatchResource();
			match.setLeft(leftResourcesByOntologyIri.get(ontologyIri));
			match.setRight(rightResourcesByOntologyIri.get(ontologyIri));
			match.setOrigin(originResourcesByOntologyIri.get(ontologyIri));
			boolean isMatch =
					match.getLeft() != null && match.getRight() != null ||
					match.getLeft() != null && match.getOrigin() != null ||
					match.getRight() != null && match.getOrigin() != null;
			if (isMatch) {
				matches.add(match);
			}
		}
		return matches;
	}
	
	
	private static Map<String, Resource> createOntologyIriToResourceMap(Set<String> ontologyIriCollector, Resource[] resources) {
		Map<String, Resource> ontologyIriToResource = new HashMap<String, Resource>();
		for (Resource resource : resources) {
			Ontology ontology = OmlRead.getOntology(resource);
			if (ontology != null && ontology.getIri() != null) {
				ontologyIriCollector.add(ontology.getIri());
				ontologyIriToResource.put(ontology.getIri(), resource);
			}
		}
		return ontologyIriToResource;
	}
}
