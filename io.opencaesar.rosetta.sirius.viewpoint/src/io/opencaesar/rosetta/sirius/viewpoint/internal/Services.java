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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.sirius.diagram.DDiagram;

import io.opencaesar.oml.Argument;
import io.opencaesar.oml.Classifier;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.DifferentFromPredicate;
import io.opencaesar.oml.Element;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.Import;
import io.opencaesar.oml.ImportKind;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.NamedInstance;
import io.opencaesar.oml.OmlFactory;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.Predicate;
import io.opencaesar.oml.PropertyCardinalityRestrictionAxiom;
import io.opencaesar.oml.PropertyPredicate;
import io.opencaesar.oml.PropertyRangeRestrictionAxiom;
import io.opencaesar.oml.PropertyRestrictionAxiom;
import io.opencaesar.oml.PropertyValueAssertion;
import io.opencaesar.oml.PropertyValueRestrictionAxiom;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.Relation;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationEntityPredicate;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.Rule;
import io.opencaesar.oml.SameAsPredicate;
import io.opencaesar.oml.Scalar;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.SemanticProperty;
import io.opencaesar.oml.SpecializableTerm;
import io.opencaesar.oml.SpecializationAxiom;
import io.opencaesar.oml.Structure;
import io.opencaesar.oml.StructuredProperty;
import io.opencaesar.oml.Term;
import io.opencaesar.oml.TypePredicate;
import io.opencaesar.oml.UnreifiedRelation;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlBuilder;
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
			.map(a -> (RelationEntity) a.getType())
			.map(t -> { if (t.getForwardRelation() != null) return t.getForwardRelation(); else return t; } )
			.map(t -> getLabel(ontology, t))
			.collect(Collectors.toList());
		return String.join(", ", types);
	}

    public static Set<PropertyValueAssertion> getVisualizedLinks(Description description) {
		var links = new HashSet<PropertyValueAssertion>();
		// member links
		links.addAll(description.getOwnedStatements().stream()
				.filter(s -> s instanceof NamedInstance)
				.map(s -> (NamedInstance)s)
				.flatMap(ci -> ci.getOwnedPropertyValues().stream())
				.filter(i -> i.getProperty() instanceof Relation)
				.collect(Collectors.toSet()));
		return links;
	}

	public static Set<NamedInstance> getVisualizedNamedInstances(Description description) {
		var instances = new LinkedHashSet<NamedInstance>();
		// member instances
		instances.addAll(description.getOwnedStatements().stream()
				.map(s ->  s.isRef() ? (NamedInstance) s.resolve() : (NamedInstance)s)
				.collect(Collectors.toSet()));
		// related instances
		instances.addAll(description.getOwnedStatements().stream()
				.filter(e -> e instanceof RelationInstance)
				.map(e -> (RelationInstance)e)
				.flatMap(i -> Stream.concat(i.getSources().stream(), i.getTargets().stream()))
				.collect(Collectors.toSet()));
		// linked instances
		instances.addAll(description.getOwnedStatements().stream()
				.map(s -> (NamedInstance)s)
				.flatMap(i -> i.getOwnedPropertyValues().stream())
				.filter(i -> i.getProperty() instanceof Relation)
				.map(i -> (NamedInstance) i.getObject())
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
			.filter(a -> a.getProperty() instanceof ScalarProperty)
			.map(a -> (PropertyValueAssertion)a)
			.collect(Collectors.toList()));
		return assertions;
	}

	public static Set<Term> getVisualizedTerms(Vocabulary vocabulary) {
		var terms = new LinkedHashSet<Term>();
		// direct terms
		terms.addAll(OmlRead.getMembers(vocabulary).stream()
				.filter(s -> s instanceof Term)
				.map(s -> s.isRef() ? (Term)s.resolve() : (Term)s)
				.collect(Collectors.toSet()));
		// specialized terms
		terms.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof SpecializableTerm)
				.map(s -> (SpecializableTerm)s)
				.flatMap(e -> e.getOwnedSpecializations().stream())
				.map(s -> s.getSuperTerm())
				.collect(Collectors.toSet()));
		// range restricted entities
		terms.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof Entity)
				.map(s -> (Entity)s)
				.flatMap(e -> e.getOwnedPropertyRestrictions().stream())
				.filter(r -> r instanceof PropertyRangeRestrictionAxiom)
				.filter(r -> r.getProperty() instanceof Relation)
				.map(r -> ((PropertyRangeRestrictionAxiom)r).getRange())
				.collect(Collectors.toSet()));
		// cardinality range restricted entities
		terms.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof Entity)
				.map(s -> (Entity)s)
				.flatMap(e -> e.getOwnedPropertyRestrictions().stream())
				.filter(r -> r instanceof PropertyCardinalityRestrictionAxiom)
				.filter(r -> r.getProperty() instanceof Relation)
				.map(r -> ((PropertyCardinalityRestrictionAxiom)r).getRange())
				.collect(Collectors.toSet()));
		return terms;
	}

	public static Set<NamedInstance> getVisualizedNamedInstances(Vocabulary vocabulary) {
		var instances = new LinkedHashSet<NamedInstance>();
		// on direct entities
		instances.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof Entity)
				.map(s -> (Entity)s)
				.flatMap(e -> e.getOwnedPropertyRestrictions().stream())
				.filter(r -> r instanceof PropertyValueRestrictionAxiom)
				.filter(r -> r.getProperty() instanceof Relation)
				.map(r -> (NamedInstance) ((PropertyValueRestrictionAxiom)r).getValue())
				.collect(Collectors.toSet()));
		return instances;
	}
	
	public static List<PropertyRestrictionAxiom> getVisualizedRestrictions(Vocabulary vocabulary) {
		var restrictions = new ArrayList<PropertyRestrictionAxiom>();
		// on direct entities
		restrictions.addAll(vocabulary.getOwnedStatements().stream()
				.filter(s -> s instanceof Entity)
				.map(s -> (Entity)s)
				.flatMap(e -> e.getOwnedPropertyRestrictions().stream())
				.filter(r -> r.getProperty() instanceof Relation)
				.collect(Collectors.toSet()));
		return restrictions;
	}

	public static Set<SemanticProperty> getVisualizedProperties(Vocabulary vocabulary, Classifier classifier) {
		var properties = new LinkedHashSet<SemanticProperty>();
		properties.addAll(vocabulary.getOwnedStatements().stream()
			.filter(s -> s instanceof SemanticProperty)
			.map(s -> (SemanticProperty)s)
			.filter(p -> p.getDomainList().contains(classifier))
			.collect(Collectors.toList()));
		return properties;
	}

	public static List<Literal> getVisualizedLiterals(Vocabulary vocabulary, Scalar scalar) {
		return scalar.getOwnedEnumeration() != null ? 
				scalar.getOwnedEnumeration().getLiterals() :
				Collections.emptyList();
	}

	public static String getLabel(Ontology ontology, NamedInstance instance) {
		var types = getTypes(ontology, instance);
		return (types != null && types.length()>0 ? "«"+types+"»\n" : "") + OmlRead.getAbbreviatedIriIn(instance, ontology);
	}

	public static String getLabel(Ontology ontology, RelationInstance instance) {
		return getTypes(ontology, instance);
	}

	public static String getLabel(Ontology ontology, Member member) {
		return OmlRead.getAbbreviatedIriIn(member, ontology);
	}
	
	public static String getLabel(Ontology ontology, ScalarProperty property) {
		return OmlRead.getAbbreviatedIriIn(property, ontology)+" : " +
				property.getRanges().stream().map(i -> getLabel(ontology, i)).collect(Collectors.joining(" & ")) +
				(property.isFunctional()? " [0..1]": "");
	}

	public static String getLabel(Ontology ontology, Predicate predicate) {
		if (predicate instanceof TypePredicate)
			return getLabel(ontology, (TypePredicate)predicate);
		if (predicate instanceof RelationEntityPredicate)
			return getLabel(ontology, (RelationEntityPredicate)predicate);
		if (predicate instanceof PropertyPredicate)
			return getLabel(ontology, (PropertyPredicate)predicate);
		if (predicate instanceof SameAsPredicate)
			return getLabel(ontology, (SameAsPredicate)predicate);
		if (predicate instanceof DifferentFromPredicate)
			return getLabel(ontology, (DifferentFromPredicate)predicate);
		return "";
	}

	public static String getLabel(Ontology ontology, TypePredicate predicate) {
		String type = predicate.getType() != null ? getLabel(ontology, predicate.getType()) : "null";
		return type+"("+getLabel(ontology, predicate.getArgument())+")";
	}
	
	public static String getLabel(Ontology ontology, RelationEntityPredicate predicate) {
		String entity = predicate.getType() != null ? getLabel(ontology, predicate.getType()) : "null";
		return entity+"("+getLabel(ontology, predicate.getArgument1())+", "+getLabel(ontology, predicate.getArgument())+ ", "+getLabel(ontology, predicate.getArgument2())+")";
	}

	public static String getLabel(Ontology ontology, PropertyPredicate predicate) {
		String property = predicate.getProperty() != null ? getLabel(ontology, predicate.getProperty()) : "null";
		return property+"("+getLabel(ontology, predicate.getArgument1())+", "+getLabel(ontology, predicate.getArgument2())+")";
	}

	public static String getLabel(Ontology ontology, SameAsPredicate predicate) {
		return "sameAs("+getLabel(ontology, predicate.getArgument1())+", "+getLabel(ontology, predicate.getArgument2())+")";
	}

	public static String getLabel(Ontology ontology, DifferentFromPredicate predicate) {
		return "differentFrom("+getLabel(ontology, predicate.getArgument1())+", "+getLabel(ontology, predicate.getArgument2())+")";
	}

	public static String getLabel(Ontology ontology, Argument a) {
		if (a.getVariable() != null) {
			return a.getVariable();
		} else if (a.getLiteral() != null) {
			return a.getLiteral().getLexicalValue();
		} else {
			return getLabel(ontology, a.getInstance());
		}
	}
	
    public static String getLabel(PropertyRangeRestrictionAxiom axiom) {
    	String q = (axiom.getKind() == RangeRestrictionKind.ALL) ? "\u2200" : "\u2203";
    	return q + getLabel(axiom.getOntology(), axiom.getProperty());
    }

    public static String getLabel(PropertyCardinalityRestrictionAxiom axiom) {
    	return getLabel(axiom.getOntology(), axiom.getProperty());
    }

    public static String getLabel(PropertyValueRestrictionAxiom axiom) {
    	return getLabel(axiom.getOntology(), (NamedInstance) axiom.getValue());
    }

    public static String getLabel(PropertyValueAssertion assertion) {
		var property = OmlRead.getAbbreviatedIriIn(assertion.getProperty(), assertion.getOntology());
		if (assertion.getProperty() instanceof ScalarProperty) {
			var value = assertion.getLiteralValue().getLexicalValue();
			return property+" = "+value;
		} else if (assertion.getProperty() instanceof Relation) {
			return OmlRead.getAbbreviatedIriIn(assertion.getProperty(), assertion.getOntology());
		}
		return null;
	}

    public static String getLabel(Literal literal) {
		return literal.getLexicalValue();
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

	public static Set<Element> getVisualizedElements(DDiagram diagram) {
		return diagram.getOwnedDiagramElements().stream()
				.filter(e -> e.getTarget() != null)
				.map(e -> (Element) e.getTarget())
				.collect(Collectors.toSet());
	}

    public static List<Ontology> getCandidateOntologies(DDiagram diagram) {
		return OmlRead.getOntologies(diagram.eResource().getResourceSet());
    }

	public static Set<Element> getCandidateElements(DDiagram diagram) {
		var visualized = getVisualizedElements(diagram);
		var ontologies = getCandidateOntologies(diagram);
		var map = new HashMap<Ontology, List<Member>>();
		for (Ontology o : ontologies) {
			var members = OmlRead.getMembers(o).stream()
					.filter(i -> !(visualized.contains(i)))
					.collect(Collectors.toList());
			map.put(o, members);
		}
		var elements = new LinkedHashSet<Element>();
		for (var entry : map.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				elements.add(entry.getKey());
				elements.addAll(entry.getValue());
			}
		}
		return elements;
	}

    public static String getNewName(Member member) {
    	String base = member.getClass().getSimpleName().replace("Impl", "");
    	return getNewName(member.getOntology(), base);
    }

    public static String getNewName(Ontology ontology, String base) {
    	var names = OmlRead.getMembers(ontology).stream()
    			.map(s -> ((Member)s).getName())
    			.collect(Collectors.toSet());
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
    		addOntologyImport(ontology, member.getOntology());
    	}
    	return member;
    }

    public static void importMemberIfNeeded(Ontology ontology, Member member) {
    	Member found = OmlRead.getMemberByIri(ontology, member.getIri());
    	if (found == null) {
    		addOntologyImport(ontology, member.getOntology());
    	}
    }

    public static void addOntologyImport(Ontology ontology, Ontology importedOntology) {
    	if (!OmlRead.getImportedOntologies(ontology).contains(importedOntology)) {
			Import newImport = OmlFactory.eINSTANCE.createImport();
			if (ontology instanceof Vocabulary) {
				if (importedOntology instanceof Vocabulary) {
					newImport.setKind(ImportKind.EXTENSION);
				} else {
					newImport.setKind(ImportKind.USAGE);
				}
			} else { // if (ontology instanceof Description)
				if (importedOntology instanceof Vocabulary) {
					newImport.setKind(ImportKind.USAGE);
				} else {
					newImport.setKind(ImportKind.EXTENSION);;
				}
			}
			ontology.getOwnedImports().add((Import)newImport);
			newImport.setNamespace(importedOntology.getNamespace());
			newImport.setPrefix(importedOntology.getPrefix());
    	}
    }
    
    public static SpecializationAxiom createSpecializationAxiom(Vocabulary vocabulary, Term source, Term target) {
    	var builder = new OmlBuilder(vocabulary.eResource().getResourceSet());
    	var axiom = builder.addSpecializationAxiom(vocabulary, source.getIri(), target.getIri());
    	builder.finish();
    	return axiom;
    }

    public static RelationEntity createRelationEntity(Vocabulary vocabulary, Entity source, Entity target) {
    	var builder = new OmlBuilder(vocabulary.eResource().getResourceSet());
    	var entity = builder.addRelationEntity(
    			vocabulary, 
    			getNewName(vocabulary, "RelationEntity"), 
    			Collections.singletonList(source.getIri()),
    			Collections.singletonList(target.getIri()),
    			false, false, false, false, false, false, false);
    	builder.addForwardRelation(entity, getNewName(vocabulary, "relationEntity"));
    	builder.finish();
    	return entity;
    }

    public static UnreifiedRelation createUnreifiedRelation(Vocabulary vocabulary, Entity source, Entity target) {
    	var builder = new OmlBuilder(vocabulary.eResource().getResourceSet());
    	var relation = builder.addUnreifiedRelation(
    			vocabulary, 
    			getNewName(vocabulary, "UnreifiedRelation"), 
    			Collections.singletonList(source.getIri()),
    			Collections.singletonList(target.getIri()),
    			false, false, false, false, false, false, false); 
    	builder.finish();
    	return relation;
    }

    public static StructuredProperty createStructuredProperty(Vocabulary vocabulary, Classifier source, Structure target) {
    	var builder = new OmlBuilder(vocabulary.eResource().getResourceSet());
    	var property = builder.addStructuredProperty(
    			vocabulary, 
    			getNewName(vocabulary, "StructuredProperty"), 
    			Collections.singletonList(source.getIri()),
    			Collections.singletonList(target.getIri()),
    			false); 
    	builder.finish();
    	return property;
    }

    public static ScalarProperty createScalarProperty(Vocabulary vocabulary, Classifier classifier) {
    	var builder = new OmlBuilder(vocabulary.eResource().getResourceSet());
    	var property = builder.addScalarProperty(
    			vocabulary, 
    			getNewName(vocabulary, "ScalarProperty"), 
    			Collections.singletonList(classifier.getIri()),
    			Collections.singletonList("http://www.w3.org/2001/XMLSchema#string"),
    			false); 
    	builder.finish();
    	return property;
    }

}
