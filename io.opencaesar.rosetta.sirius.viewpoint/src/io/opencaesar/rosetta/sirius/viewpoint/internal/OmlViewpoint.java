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

import io.opencaesar.oml.Classifier;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.DifferentFromPredicate;
import io.opencaesar.oml.Element;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.EntityReference;
import io.opencaesar.oml.EnumeratedScalar;
import io.opencaesar.oml.FeaturePredicate;
import io.opencaesar.oml.LinkAssertion;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.NamedInstance;
import io.opencaesar.oml.NamedInstanceReference;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.Predicate;
import io.opencaesar.oml.PropertyValueAssertion;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.RelationCardinalityRestrictionAxiom;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationEntityPredicate;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.RelationRangeRestrictionAxiom;
import io.opencaesar.oml.RelationRestrictionAxiom;
import io.opencaesar.oml.RelationTargetRestrictionAxiom;
import io.opencaesar.oml.SameAsPredicate;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.ScalarPropertyReference;
import io.opencaesar.oml.ScalarPropertyValueAssertion;
import io.opencaesar.oml.SemanticProperty;
import io.opencaesar.oml.SpecializableTerm;
import io.opencaesar.oml.SpecializableTermReference;
import io.opencaesar.oml.StructuredProperty;
import io.opencaesar.oml.StructuredPropertyReference;
import io.opencaesar.oml.TypePredicate;
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

    public static String getTypes(Ontology ontology, NamedInstance instance) {
		var types = OmlSearch.findTypeAssertions(instance).stream()
			.map(a -> getLabel(ontology, a.getType()))
			.collect(Collectors.toList());
		return String.join(", ", types);
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

	public static List<PropertyValueAssertion> getVisualizedPropertyValues(Description description, NamedInstance instance) {
		var assertions = new ArrayList<PropertyValueAssertion>();
		// scalar property values on members
		assertions.addAll(description.getOwnedStatements().stream()
			.filter(s -> s == instance)
			.map(s -> (NamedInstance)s)
			.flatMap(i -> i.getOwnedPropertyValues().stream())
			.filter(a -> a instanceof PropertyValueAssertion)
			.map(a -> (PropertyValueAssertion)a)
			.collect(Collectors.toList()));
		// scalar property values on references
		assertions.addAll(description.getOwnedStatements().stream()
			.filter(s -> s instanceof NamedInstanceReference)
			.map(s -> (NamedInstanceReference)s)
			.filter(r -> OmlRead.resolve(r) == instance)
			.flatMap(i -> i.getOwnedPropertyValues().stream())
			.filter(a -> a instanceof PropertyValueAssertion)
			.map(a -> (PropertyValueAssertion)a)
			.collect(Collectors.toList()));
		return assertions;
	}

	public static Set<SpecializableTerm> getVisualizedTerms(Vocabulary vocabulary) {
		var terms = new LinkedHashSet<SpecializableTerm>();
		// direct terms
		terms.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof SpecializableTerm)
				.map(s -> (SpecializableTerm)s)
				.collect(Collectors.toSet()));
		// referenced terms
		terms.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof SpecializableTermReference)
				.map(s -> (SpecializableTerm) OmlRead.resolve((SpecializableTermReference)s))
				.collect(Collectors.toSet()));
		// specialized terms
		terms.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof SpecializableTerm)
				.map(s -> (SpecializableTerm)s)
				.flatMap(e -> e.getOwnedSpecializations().stream())
				.map(s -> s.getSpecializedTerm())
				.collect(Collectors.toSet()));
		terms.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof SpecializableTermReference)
				.map(s -> (SpecializableTermReference)s)
				.flatMap(e -> e.getOwnedSpecializations().stream())
				.map(s -> s.getSpecializedTerm())
				.collect(Collectors.toSet()));
		// related terms
		terms.addAll(vocabulary.getOwnedStatements().stream()
				.filter(e -> e instanceof RelationEntity)
				.map(e -> (RelationEntity)e)
				.flatMap(r -> Stream.of(r.getSource(), r.getTarget()))
				.collect(Collectors.toSet()));
		// range restricted entities
		terms.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof Entity)
				.map(s -> (Entity)s)
				.flatMap(e -> e.getOwnedRelationRestrictions().stream())
				.filter(r -> r instanceof RelationRangeRestrictionAxiom)
				.map(r -> ((RelationRangeRestrictionAxiom)r).getRange())
				.collect(Collectors.toSet()));
		terms.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof EntityReference)
				.map(s -> (EntityReference)s)
				.flatMap(e -> e.getOwnedRelationRestrictions().stream())
				.filter(r -> r instanceof RelationRangeRestrictionAxiom)
				.map(r -> ((RelationRangeRestrictionAxiom)r).getRange())
				.collect(Collectors.toSet()));
		// cardinality range restricted entities
		terms.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof Entity)
				.map(s -> (Entity)s)
				.flatMap(e -> e.getOwnedRelationRestrictions().stream())
				.filter(r -> r instanceof RelationCardinalityRestrictionAxiom)
				.map(r -> ((RelationCardinalityRestrictionAxiom)r).getRange())
				.collect(Collectors.toSet()));
		terms.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof EntityReference)
				.map(s -> (EntityReference)s)
				.flatMap(e -> e.getOwnedRelationRestrictions().stream())
				.filter(r -> r instanceof RelationCardinalityRestrictionAxiom)
				.map(r -> ((RelationCardinalityRestrictionAxiom)r).getRange())
				.collect(Collectors.toSet()));
		return terms;
	}

	public static Set<NamedInstance> getVisualizedNamedInstances(Vocabulary vocabulary) {
		var instances = new LinkedHashSet<NamedInstance>();
		// on direct entities
		instances.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof Entity)
				.map(s -> (Entity)s)
				.flatMap(e -> e.getOwnedRelationRestrictions().stream())
				.filter(r -> r instanceof RelationTargetRestrictionAxiom)
				.map(r -> ((RelationTargetRestrictionAxiom)r).getTarget())
				.collect(Collectors.toSet()));
		// on referenced entities
		instances.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof EntityReference)
				.map(s -> (EntityReference)s)
				.flatMap(e -> e.getOwnedRelationRestrictions().stream())
				.filter(r -> r instanceof RelationTargetRestrictionAxiom)
				.map(r -> ((RelationTargetRestrictionAxiom)r).getTarget())
				.collect(Collectors.toSet()));
		return instances;
	}
	
	public static List<RelationRestrictionAxiom> getVisualizedRestrictions(Vocabulary vocabulary) {
		var restrictions = new ArrayList<RelationRestrictionAxiom>();
		// on direct entities
		restrictions.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof Entity)
				.map(s -> (Entity)s)
				.flatMap(e -> e.getOwnedRelationRestrictions().stream())
				.collect(Collectors.toSet()));
		// on referenced entities
		restrictions.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof EntityReference)
				.map(s -> (EntityReference) s)
				.flatMap(e -> e.getOwnedRelationRestrictions().stream())
				.collect(Collectors.toSet()));
		return restrictions;
	}

	public static Set<SemanticProperty> getVisualizedProperties(Vocabulary vocabulary, Classifier classifier) {
		var properties = new LinkedHashSet<SemanticProperty>();
		properties.addAll(vocabulary.getOwnedStatements().stream()
			.filter(s -> s instanceof SemanticProperty)
			.map(s -> (SemanticProperty)s)
			.filter(p -> p.getDomain() == classifier)
			.collect(Collectors.toList()));
		properties.addAll(vocabulary.getOwnedStatements().stream()
			.filter(s -> s instanceof ScalarPropertyReference)
			.map(s -> (ScalarPropertyReference) s)
			.map(r -> (ScalarProperty) OmlRead.resolve(r))
			.filter(p -> p.getDomain() == classifier)
			.collect(Collectors.toList()));
		properties.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof StructuredPropertyReference)
				.map(s -> (StructuredPropertyReference) s)
				.map(r -> (StructuredProperty) OmlRead.resolve(r))
				.filter(p -> p.getDomain() == classifier)
				.collect(Collectors.toList()));
		return properties;
	}

	public static List<Literal> getVisualizedLiterals(Vocabulary vocabulary, EnumeratedScalar scalar) {
		return scalar.getLiterals();
	}

	public static String getLabel(Ontology ontology, Member member) {
		return OmlRead.getAbbreviatedIriIn(member, ontology);
	}
	
	public static String getLabel(Ontology ontology, RelationEntity entity) {
		var forward = entity.getForwardRelation();
		if (forward != null) {
			return OmlRead.getAbbreviatedIriIn(forward, ontology);
		} else {
			return OmlRead.getAbbreviatedIriIn(entity, ontology);
		}
	}

	public static String getLabel(Ontology ontology, NamedInstance instance) {
		return OmlRead.getAbbreviatedIriIn(instance, ontology) + " : "+getTypes(ontology, instance);
	}

	public static String getLabel(Ontology ontology, ScalarProperty property) {
		return OmlRead.getAbbreviatedIriIn(property, ontology)+" : "+getLabel(ontology, property.getRange())+(property.isFunctional()? " [0..1]": "");
	}

	public static String getLabel(Ontology ontology, Predicate predicate) {
		if (predicate instanceof TypePredicate)
			return getLabel(ontology, (TypePredicate)predicate);
		if (predicate instanceof RelationEntityPredicate)
			return getLabel(ontology, (RelationEntityPredicate)predicate);
		if (predicate instanceof FeaturePredicate)
			return getLabel(ontology, (FeaturePredicate)predicate);
		if (predicate instanceof SameAsPredicate)
			return getLabel(ontology, (SameAsPredicate)predicate);
		if (predicate instanceof DifferentFromPredicate)
			return getLabel(ontology, (DifferentFromPredicate)predicate);
		return "";
	}

	public static String getLabel(Ontology ontology, TypePredicate predicate) {
		return getLabel(ontology, predicate.getType())+"("+predicate.getVariable()+")";
	}
	
	public static String getLabel(Ontology ontology, RelationEntityPredicate predicate) {
		return getLabel(ontology, predicate.getEntity())+"("+predicate.getVariable1()+", "+predicate.getEntityVariable()+ ", "+predicate.getVariable2()+")";
	}

	public static String getLabel(Ontology ontology, FeaturePredicate predicate) {
		return getLabel(ontology, predicate.getFeature())+"("+predicate.getVariable1()+", "+predicate.getVariable2()+")";
	}

	public static String getLabel(Ontology ontology, SameAsPredicate predicate) {
		return "SameAs("+predicate.getVariable1()+", "+predicate.getVariable2()+")";
	}

	public static String getLabel(Ontology ontology, DifferentFromPredicate predicate) {
		return "DifferentFrom("+predicate.getVariable1()+", "+predicate.getVariable2()+")";
	}

    public static String getLabel(RelationRangeRestrictionAxiom axiom) {
    	String q = (axiom.getKind() == RangeRestrictionKind.ALL) ? "\u2200" : "\u2203";
    	return q + getLabel(axiom.getOntology(), axiom.getRelation());
    }

    public static String getLabel(RelationCardinalityRestrictionAxiom axiom) {
    	return getLabel(axiom.getOntology(), axiom.getRelation());
    }

    public static String getLabel(RelationTargetRestrictionAxiom axiom) {
    	return getLabel(axiom.getOntology(), axiom.getRelation());
    }

    public static String getLabel(ScalarPropertyValueAssertion assertion) {
		var property = OmlRead.getAbbreviatedIriIn(assertion.getProperty(), assertion.getOntology());
		var value = OmlRead.getLexicalValue(assertion.getValue());
		return property+" = "+value;
	}

    public static String getLabel(LinkAssertion assertion) {
		return OmlRead.getAbbreviatedIriIn(assertion.getRelation(), assertion.getOntology());
	}
    
    public static String getLabel(Literal literal) {
		return OmlRead.getLexicalValue(literal);
	}

}
