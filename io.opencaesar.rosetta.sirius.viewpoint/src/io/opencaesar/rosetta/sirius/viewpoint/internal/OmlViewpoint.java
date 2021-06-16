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
package io.opencaesar.rosetta.sirius.viewpoint.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;

import io.opencaesar.oml.AnnotatedElement;
import io.opencaesar.oml.AnnotationProperty;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.DifferentFromPredicate;
import io.opencaesar.oml.Element;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.EntityPredicate;
import io.opencaesar.oml.EntityReference;
import io.opencaesar.oml.FeatureProperty;
import io.opencaesar.oml.Instance;
import io.opencaesar.oml.LinkAssertion;
import io.opencaesar.oml.NamedInstance;
import io.opencaesar.oml.NamedInstanceReference;
import io.opencaesar.oml.Predicate;
import io.opencaesar.oml.RelationCardinalityRestrictionAxiom;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationEntityPredicate;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.RelationPredicate;
import io.opencaesar.oml.RelationRangeRestrictionAxiom;
import io.opencaesar.oml.RelationRestrictionAxiom;
import io.opencaesar.oml.RelationTargetRestrictionAxiom;
import io.opencaesar.oml.Rule;
import io.opencaesar.oml.SameAsPredicate;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.ScalarPropertyReference;
import io.opencaesar.oml.ScalarPropertyValueAssertion;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;

/**
 * Services used by the OML viewpoint
 * 
 * NOTE: This class should not be treated as API. It is only meant to be used by this project 
 * 
 * @author elaasar
 */
public final class OmlViewpoint {
    
	//-------
	// EDITOR
	//-------
	
	private static final String OML_EDITOR_ID = "io.opencaesar.oml.dsl.Oml";

	/**
	 * Opens the Oml editor for the given Oml element and highlights it in the editor
	 *  
	 * @param element
	 * @return
	 */
	public static EObject openInOmlEditor(Element element) {
		if (element != null && element.eResource() instanceof XtextResource && element.eResource().getURI() != null) {

			String fileURI = element.eResource().getURI().toPlatformString(true);
			IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(fileURI));
			if (workspaceFile != null) {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IEditorPart openEditor = IDE.openEditor(page, workspaceFile, OML_EDITOR_ID, true);
					if (openEditor instanceof AbstractTextEditor) {
						ICompositeNode node = NodeModelUtils.findActualNodeFor(element);
						if (node != null) {
							int offset = node.getOffset();
							int length = node.getTotalEndOffset() - offset;
							((AbstractTextEditor) openEditor).selectAndReveal(offset, length);
						}
					}
					// editorInput.
				} catch (PartInitException e) {
					System.err.println(e);
				}
			}
		}
		return element;
	}

	//-------
	// COMMON
	//-------
	
	public static Object findAnnotationPropertyValue(AnnotatedElement element, String abbreviatedPropertyIri) {
		var property = (AnnotationProperty) OmlRead.getMemberByAbbreviatedIri(element, abbreviatedPropertyIri);
		return OmlSearch.findAnnotationValue(element, property);
	}

	//-------------
	// DESCRIPTIONS
	//-------------

	public static Object findFeaturePropertyValue(Instance instance, String abbreviatedPropertyIri) {
		var property = (FeatureProperty) OmlRead.getMemberByAbbreviatedIri(instance, abbreviatedPropertyIri);
		return OmlSearch.findPropertyValue(instance, property);
	}

	public static Set<LinkAssertion> getVisualizedLinks(Description description) {
		var links = new HashSet<LinkAssertion>();
		// member links
		links.addAll(description.getOwnedStatements().stream()
				.filter(s -> s instanceof NamedInstance)
				.map(s -> (NamedInstance)s)
				.flatMap(ci -> ci.getOwnedLinks().stream())
				.collect(Collectors.toSet()));
		// reference links
		links.addAll(description.getOwnedStatements().stream()
				.filter(s -> s instanceof NamedInstanceReference)
				.map(s -> (NamedInstanceReference)s)
				.flatMap(ci -> ci.getOwnedLinks().stream())
				.collect(Collectors.toSet()));
		return links;
	}

	public static Set<NamedInstance> getVisualizedNamedInstances(Description description) {
		var instances = new LinkedHashSet<NamedInstance>();
		// member instances
		instances.addAll(description.getOwnedStatements().stream()
				.filter(s -> s instanceof NamedInstance)
				.map(s -> (NamedInstance)s)
				.collect(Collectors.toSet()));
		// referenced instances
		instances.addAll(description.getOwnedStatements().stream()
				.filter(s -> s instanceof NamedInstanceReference)
				.map(s -> (NamedInstanceReference) s)
				.map(r -> (NamedInstance) OmlRead.resolve(r))
				.collect(Collectors.toSet()));
		// related instances
		instances.addAll(description.getOwnedStatements().stream()
				.filter(e -> e instanceof RelationInstance)
				.map(e -> (RelationInstance)e)
				.flatMap(i -> Stream.concat(i.getSources().stream(), i.getTargets().stream()))
				.collect(Collectors.toSet()));
		// linked instances
		instances.addAll(description.getOwnedStatements().stream()
				.filter(s -> s instanceof NamedInstance)
				.map(s -> (NamedInstance)s)
				.flatMap(i -> i.getOwnedLinks().stream())
				.map(l -> l.getTarget())
				.flatMap(i -> {
					var list = new ArrayList<NamedInstance>();
					list.add(i);
					if (i instanceof RelationEntity) {
						list.addAll(((RelationInstance)i).getSources());
						list.addAll(((RelationInstance)i).getTargets());
					}
					return list.stream();
				}).collect(Collectors.toSet()));
		// linked instances
		instances.addAll(description.getOwnedStatements().stream()
				.filter(s -> s instanceof NamedInstanceReference)
				.map(s -> (NamedInstanceReference) s)
				.flatMap(i -> i.getOwnedLinks().stream())
				.map(l -> l.getTarget())
				.flatMap(i -> {
					var list = new ArrayList<NamedInstance>();
					list.add(i);
					if (i instanceof RelationEntity) {
						list.addAll(((RelationInstance)i).getSources());
						list.addAll(((RelationInstance)i).getTargets());
					}
					return list.stream();
				}).collect(Collectors.toSet()));
		return instances;
	}

	public static List<ScalarPropertyValueAssertion> getVisualizedScalarPropertyValues(Description description, NamedInstance instance) {
		var assertions = new ArrayList<ScalarPropertyValueAssertion>();
		// scalar property values on members
		assertions.addAll(description.getOwnedStatements().stream()
			.filter(s -> s == instance)
			.map(s -> (NamedInstance)s)
			.flatMap(i -> i.getOwnedPropertyValues().stream())
			.filter(a -> a instanceof ScalarPropertyValueAssertion)
			.map(a -> (ScalarPropertyValueAssertion)a)
			.collect(Collectors.toList()));
		// scalar property values on references
		assertions.addAll(description.getOwnedStatements().stream()
			.filter(s -> s instanceof NamedInstanceReference)
			.map(s -> (NamedInstanceReference)s)
			.filter(r -> OmlRead.resolve(r) == instance)
			.flatMap(i -> i.getOwnedPropertyValues().stream())
			.filter(a -> a instanceof ScalarPropertyValueAssertion)
			.map(a -> (ScalarPropertyValueAssertion)a)
			.collect(Collectors.toList()));
		return assertions;
	}

    public static String getTypes(Description description, NamedInstance instance) {
		var types = OmlSearch.findTypeAssertions(instance).stream()
			.map(a -> a.getType().getName())
			.collect(Collectors.toList());
		return String.join(",", types);
	}

    public static String getLabel(ScalarPropertyValueAssertion assertion) {
		var value = OmlRead.getLexicalValue(assertion.getValue());
		var property = assertion.getProperty().getAbbreviatedIri();
		return property+" = "+value;
	}

	//-------------
	// VOCABULARIES
	//-------------

	public static Set<Entity> getVisualizedEntities(Vocabulary vocabulary) {
		var entities = new LinkedHashSet<Entity>();
		// direct entities
		entities.addAll(vocabulary.getOwnedStatements().stream().
				filter(s -> s instanceof Entity).
				map(s -> (Entity)s).
				collect(Collectors.toSet()));
		// reference entities
		entities.addAll(vocabulary.getOwnedStatements().stream().
				filter(s -> s instanceof EntityReference).
				map(s -> (Entity) OmlRead.resolve((EntityReference)s)).
				collect(Collectors.toSet()));
		// specialized entities
		entities.addAll(vocabulary.getOwnedStatements().stream().
				filter(s -> s instanceof Entity).
				map(s -> (Entity)s).
				flatMap(e -> e.getOwnedSpecializations().stream()).
				map(s -> (Entity) s.getSpecializedTerm()).
				collect(Collectors.toSet()));
		entities.addAll(vocabulary.getOwnedStatements().stream().
				filter(s -> s instanceof EntityReference).
				map(s -> (EntityReference)s).
				flatMap(e -> e.getOwnedSpecializations().stream()).
				map(s -> (Entity) s.getSpecializedTerm()).
				collect(Collectors.toSet()));
		// related entities
		entities.addAll(vocabulary.getOwnedStatements().stream().
				filter(e -> e instanceof RelationEntity).
				map(e -> (RelationEntity)e).
				flatMap(r -> Stream.of(r.getSource(), r.getTarget())).
				collect(Collectors.toSet()));
		// range restricted entities
		entities.addAll(vocabulary.getOwnedStatements().stream().
				filter(s -> s instanceof Entity).
				map(s -> (Entity)s).
				flatMap(e -> e.getOwnedRelationRestrictions().stream()).
				filter(r -> r instanceof RelationRangeRestrictionAxiom).
				map(r -> ((RelationRangeRestrictionAxiom)r).getRange()).
				collect(Collectors.toSet()));
		entities.addAll(vocabulary.getOwnedStatements().stream().
				filter(s -> s instanceof EntityReference).
				map(s -> (EntityReference)s).
				flatMap(e -> e.getOwnedRelationRestrictions().stream()).
				filter(r -> r instanceof RelationRangeRestrictionAxiom).
				map(r -> ((RelationRangeRestrictionAxiom)r).getRange()).
				collect(Collectors.toSet()));
		// cardinality range restricted entities
		entities.addAll(vocabulary.getOwnedStatements().stream().
				filter(s -> s instanceof Entity).
				map(s -> (Entity)s).
				flatMap(e -> e.getOwnedRelationRestrictions().stream()).
				filter(r -> r instanceof RelationCardinalityRestrictionAxiom).
				map(r -> ((RelationCardinalityRestrictionAxiom)r).getRange()).
				collect(Collectors.toSet()));
		entities.addAll(vocabulary.getOwnedStatements().stream().
				filter(s -> s instanceof EntityReference).
				map(s -> (EntityReference)s).
				flatMap(e -> e.getOwnedRelationRestrictions().stream()).
				filter(r -> r instanceof RelationCardinalityRestrictionAxiom).
				map(r -> ((RelationCardinalityRestrictionAxiom)r).getRange()).
				collect(Collectors.toSet()));
		return entities;
	}

	public static Set<NamedInstance> getVisualizedNamedInstances(Vocabulary vocabulary) {
		var instances = new LinkedHashSet<NamedInstance>();
		// on direct entities
		instances.addAll(vocabulary.getOwnedStatements().stream().
				filter(s -> s instanceof Entity).
				map(s -> (Entity)s).
				flatMap(e -> e.getOwnedRelationRestrictions().stream()).
				filter(r -> r instanceof RelationTargetRestrictionAxiom).
				map(r -> ((RelationTargetRestrictionAxiom)r).getTarget()).
				collect(Collectors.toSet()));
		// on referenced entities
		instances.addAll(vocabulary.getOwnedStatements().stream().
				filter(s -> s instanceof EntityReference).
				map(s -> (EntityReference)s).
				flatMap(e -> e.getOwnedRelationRestrictions().stream()).
				filter(r -> r instanceof RelationTargetRestrictionAxiom).
				map(r -> ((RelationTargetRestrictionAxiom)r).getTarget()).
				collect(Collectors.toSet()));
		return instances;
	}
	
	public static List<RelationRestrictionAxiom> getVisualizedRestrictions(Vocabulary vocabulary) {
		var restrictions = new ArrayList<RelationRestrictionAxiom>();
		// on direct entities
		restrictions.addAll(vocabulary.getOwnedStatements().stream().
				filter(s -> s instanceof Entity).
				map(s -> (Entity)s).
				flatMap(e -> e.getOwnedRelationRestrictions().stream()).
				collect(Collectors.toSet()));
		// on referenced entities
		restrictions.addAll(vocabulary.getOwnedStatements().stream().
				filter(s -> s instanceof EntityReference).
				map(s -> (EntityReference) s).
				flatMap(e -> e.getOwnedRelationRestrictions().stream()).
				collect(Collectors.toSet()));
		return restrictions;
	}

	public static Set<ScalarProperty> getVisualizedScalarProperties(Vocabulary vocabulary, Entity entity) {
		var properties = new LinkedHashSet<ScalarProperty>();
		properties.addAll(vocabulary.getOwnedStatements().stream()
			.filter(s -> s instanceof ScalarProperty)
			.map(s -> (ScalarProperty)s)
			.filter(p -> p.getDomain() == entity)
			.collect(Collectors.toList()));
		properties.addAll(vocabulary.getOwnedStatements().stream()
			.filter(s -> s instanceof ScalarPropertyReference)
			.map(s -> (ScalarPropertyReference) s)
			.map(r -> (ScalarProperty) OmlRead.resolve(r))
			.filter(p -> p.getDomain() == entity)
			.collect(Collectors.toList()));
		return properties;
	}

	public static String getLabel(Rule rule) {
		String antecedeant = rule.getAntecedent().stream().
				map(p -> getLabel(p)).
				collect(Collectors.joining(" ^ "));
		String consequent = rule.getConsequent().stream().
				map(p -> getLabel(p)).
				collect(Collectors.joining(" ^ "));
		return antecedeant + " -> " + consequent;
	}

	public static String getLabel(Predicate predicate) {
		if (predicate instanceof EntityPredicate)
			return getLabel((EntityPredicate)predicate);
		if (predicate instanceof RelationEntityPredicate)
			return getLabel((RelationEntityPredicate)predicate);
		if (predicate instanceof RelationPredicate)
			return getLabel((RelationPredicate)predicate);
		if (predicate instanceof SameAsPredicate)
			return getLabel((SameAsPredicate)predicate);
		if (predicate instanceof DifferentFromPredicate)
			return getLabel((DifferentFromPredicate)predicate);
		return "";
	}

	public static String getLabel(EntityPredicate predicate) {
		return predicate.getEntity().getName()+"("+predicate.getVariable()+")";
	}
	
	public static String getLabel(RelationEntityPredicate predicate) {
		return predicate.getEntity().getName()+"("+predicate.getVariable1()+", "+predicate.getEntityVariable()+ ", "+predicate.getVariable2()+")";
	}

	public static String getLabel(RelationPredicate predicate) {
		return predicate.getRelation().getName()+"("+predicate.getVariable1()+", "+predicate.getVariable2()+")";
	}

	public static String getLabel(SameAsPredicate predicate) {
		return "SameAs("+predicate.getVariable1()+", "+predicate.getVariable2()+")";
	}

	public static String getLabel(DifferentFromPredicate predicate) {
		return "DifferentFrom("+predicate.getVariable1()+", "+predicate.getVariable2()+")";
	}

	public static String getLabel(ScalarProperty property, Entity entity) {
		var isKey = entity.getOwnedKeys().stream().anyMatch(k -> k.getProperties().contains(property));
		return property.getName()+" : "+property.getOntology().getPrefix()+":"+property.getRange().getName()+(property.isFunctional()? " [0..1]": "")+(isKey ? " (key)" : "");
	}

}
