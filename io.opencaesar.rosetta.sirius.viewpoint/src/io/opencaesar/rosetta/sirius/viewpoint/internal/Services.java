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

import org.eclipse.sirius.diagram.DDiagram;

import io.opencaesar.oml.Classifier;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionImport;
import io.opencaesar.oml.DifferentFromPredicate;
import io.opencaesar.oml.Element;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.EntityReference;
import io.opencaesar.oml.EnumeratedScalar;
import io.opencaesar.oml.FeaturePredicate;
import io.opencaesar.oml.Import;
import io.opencaesar.oml.LinkAssertion;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.NamedInstance;
import io.opencaesar.oml.NamedInstanceReference;
import io.opencaesar.oml.OmlFactory;
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
import io.opencaesar.oml.Rule;
import io.opencaesar.oml.SameAsPredicate;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.ScalarPropertyReference;
import io.opencaesar.oml.ScalarPropertyValueAssertion;
import io.opencaesar.oml.SemanticProperty;
import io.opencaesar.oml.SpecializableTerm;
import io.opencaesar.oml.SpecializableTermReference;
import io.opencaesar.oml.StructuredProperty;
import io.opencaesar.oml.StructuredPropertyReference;
import io.opencaesar.oml.Type;
import io.opencaesar.oml.TypePredicate;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.VocabularyImport;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;

/**
 * Services used by the OML viewpoint
 * 
 * NOTE: This class should not be treated as API. It is only meant to be used by this project 
 * 
 * @author elaasar
 */
public final class Services {
    
    public static String getTypes(Ontology ontology, NamedInstance instance) {
		var types = OmlSearch.findTypeAssertions(instance).stream()
			.map(a -> getLabel(ontology, a.getType()))
			.collect(Collectors.toList());
		return String.join(", ", types);
	}

    public static String getTypes(Ontology ontology, RelationInstance instance) {
		var types = OmlSearch.findTypeAssertions(instance).stream()
			.map(a -> a.getType())
			.map(t -> { if (t.getForwardRelation() != null) return t.getForwardRelation(); else return t; } )
			.map(t -> getLabel(ontology, t))
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
		var types = getTypes(ontology, instance);
		return (types != null && types.length()>0 ? "«"+types+"»\n" : "") + OmlRead.getAbbreviatedIriIn(instance, ontology);
	}

	public static String getLabel(Ontology ontology, RelationInstance instance) {
		return getTypes(ontology, instance);
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
		String type = predicate.getType() != null ? getLabel(ontology, predicate.getType()) : "null";
		return type+"("+predicate.getVariable()+")";
	}
	
	public static String getLabel(Ontology ontology, RelationEntityPredicate predicate) {
		String entity = predicate.getEntity() != null ? getLabel(ontology, predicate.getEntity()) : "null";
		return entity+"("+predicate.getVariable1()+", "+predicate.getEntityVariable()+ ", "+predicate.getVariable2()+")";
	}

	public static String getLabel(Ontology ontology, FeaturePredicate predicate) {
		String feature = predicate.getFeature() != null ? getLabel(ontology, predicate.getFeature()) : "null";
		return feature+"("+predicate.getVariable1()+", "+predicate.getVariable2()+")";
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

	public static Set<Member> getVisualizedMembers(Vocabulary vocabulary) {
		var members = new LinkedHashSet<Member>();
		// terms
		members.addAll(getVisualizedTerms(vocabulary));
		// named instances
		members.addAll(getVisualizedNamedInstances(vocabulary));
		// rules
		members.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof Rule)
				.map(s -> (Rule)s)
				.collect(Collectors.toSet()));
		return members;
	}

	public static List<Element> getVisualizedElements(DDiagram diagram) {
		return diagram.getOwnedDiagramElements().stream()
				.filter(e -> e.getTarget() != null)
				.map(e -> (Element) e.getTarget())
				.collect(Collectors.toList());
	}

	public static List<Element> getExistingElements(Vocabulary vocabulary) {
		var ontologies = new ArrayList<Ontology>();
		ontologies.add(vocabulary);
		ontologies.addAll(OmlRead.getImports(vocabulary).stream()
				.filter(i -> i.getPrefix() != null)
				.map(i -> OmlRead.getImportedOntology(i))
				.collect(Collectors.toList()));
		var elements = new ArrayList<Element>();
		elements.addAll(ontologies);
		elements.addAll(ontologies.stream()
				.flatMap(o -> OmlRead.getStatements(o).stream())
				.filter(i -> (i instanceof Type) && !(i instanceof RelationEntity))
				.map(i -> (Member)i)
				.collect(Collectors.toList()));
		return elements;
	}

    public static String getNewName(Member member) {
    	var names = OmlRead.getMembers(member.getOntology()).stream()
    			.map(s -> ((Member)s).getName())
    			.collect(Collectors.toSet());
    	String base = member.getClass().getSimpleName().replace("Impl", "");
    	String name = base;
    	int i = 0;
    	while (names.contains(name)) {
    		name = base + ++i;
    	}
    	return name;	
    }

    public static Member getOrImportMemberByAbbreviatedIri(Ontology ontology, String abbreviatedIri) {
    	Member member = OmlRead.getMemberByAbbreviatedIri(ontology, abbreviatedIri);
    	if (member == null) {
    		member = OmlRead.getMemberByAbbreviatedIri(ontology.eResource().getResourceSet(), abbreviatedIri);
    		Ontology importedOntology = member.getOntology();
    		Import newImport = null;
    		if (ontology instanceof Vocabulary) {
    			if (importedOntology instanceof Vocabulary) {
    				newImport = OmlFactory.eINSTANCE.createVocabularyExtension();
    			} else {
    				newImport = OmlFactory.eINSTANCE.createVocabularyUsage();
    			}
        		((Vocabulary)ontology).getOwnedImports().add((VocabularyImport)newImport);
    		} else { // if (ontology instanceof Description)
    			if (importedOntology instanceof Vocabulary) {
    				newImport = OmlFactory.eINSTANCE.createDescriptionBundleUsage();
    			} else {
    				newImport = OmlFactory.eINSTANCE.createDescriptionExtension();
    			}
        		((Description)ontology).getOwnedImports().add((DescriptionImport)newImport);
    		}
    		newImport.setNamespace(importedOntology.getNamespace());
    		newImport.setPrefix(importedOntology.getPrefix());
    	}
    	return member;
    }
	
}
