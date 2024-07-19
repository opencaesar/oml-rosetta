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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;

import io.opencaesar.oml.Argument;
import io.opencaesar.oml.BooleanLiteral;
import io.opencaesar.oml.CardinalityRestrictionKind;
import io.opencaesar.oml.Classifier;
import io.opencaesar.oml.DecimalLiteral;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.DifferentFromPredicate;
import io.opencaesar.oml.DoubleLiteral;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.Instance;
import io.opencaesar.oml.IntegerLiteral;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.NamedInstance;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.Predicate;
import io.opencaesar.oml.PropertyCardinalityRestrictionAxiom;
import io.opencaesar.oml.PropertyPredicate;
import io.opencaesar.oml.PropertyRangeRestrictionAxiom;
import io.opencaesar.oml.PropertyRestrictionAxiom;
import io.opencaesar.oml.PropertyValueAssertion;
import io.opencaesar.oml.PropertyValueRestrictionAxiom;
import io.opencaesar.oml.QuotedLiteral;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.Relation;
import io.opencaesar.oml.RelationBase;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationEntityPredicate;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.SameAsPredicate;
import io.opencaesar.oml.Scalar;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.SemanticProperty;
import io.opencaesar.oml.SpecializableTerm;
import io.opencaesar.oml.SpecializationAxiom;
import io.opencaesar.oml.Statement;
import io.opencaesar.oml.Structure;
import io.opencaesar.oml.StructuredProperty;
import io.opencaesar.oml.Term;
import io.opencaesar.oml.TypePredicate;
import io.opencaesar.oml.UnreifiedRelation;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlBuilder;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;
import io.opencaesar.oml.util.OmlWrite;

/**
 * Services used by the OML viewpoint
 * 
 * NOTE: This class should not be treated as API. It is only meant to be used by this project 
 * 
 * @author elaasar
 */
public final class OmlServices extends io.opencaesar.rosetta.sirius.viewpoint.OmlServices {
   
	public static Set<ScalarProperty> allScalarProperties(Instance instance) {
		var types = OmlSearch.findAllTypes(instance, getScope(instance));
		return types.stream()
				.flatMap(t -> OmlSearch.findScalarPropertiesWithDomain(t, getScope(instance)).stream())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public static boolean isStringProperty(ScalarProperty property) {
		var string = (Scalar) OmlRead.getMemberByAbbreviatedIri(property.eResource().getResourceSet(), "xsd:string");
		return property.getRanges().stream()
				.anyMatch(r -> OmlSearch.findIsSubTermOf(r, string, getScope(property)));
	}
	
	public static boolean isBooleanProperty(ScalarProperty property) {
		var string = (Scalar) OmlRead.getMemberByAbbreviatedIri(property.eResource().getResourceSet(), "xsd:boolean");
		return property.getRanges().stream()
				.anyMatch(r -> OmlSearch.findIsSubTermOf(r, string, getScope(property)));
	}

	public static void setPropertyValue(Instance instance, ScalarProperty property, Object newValue) {
		removePropertyValue(instance.getOntology(), instance, property.getAbbreviatedIri());
		if (newValue != null && newValue.toString().length() > 0) {
			var newLiteral = OmlWrite.createLiteral(newValue);
			addPropertyValue(instance.getOntology(), instance, property.getAbbreviatedIri(), newLiteral);
		}
	}

	public static Set<Object> getEnumeratiomLiteralValues(ScalarProperty property) {
		return OmlSearch.findRanges(property, getScope(property)).stream()
				.flatMap(r -> OmlSearch.findEnumerationLiterals((Scalar)r, getScope(property)).stream())
				.map(l -> l.getValue())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}
	
	//----------------
	
    public static String getLabel(Literal literal) {
		return literal.getLexicalValue();
	}

    private static String getAbbreviatedIriIn(Member member, Ontology ontology) {
    	var iri = OmlRead.getAbbreviatedIriIn(member.resolve(), ontology);
    	if (iri == null) {
    		iri = member.getAbbreviatedIri();
    	}
    	return iri;
    }
    
	public static String getLabel(Ontology ontology, Member member) {
		return getAbbreviatedIriIn(member, ontology);
	}
	
	public static String getForwardLabel(Ontology ontology, RelationEntity entity) {
		if (entity.getForwardRelation() != null) {
			var name = getAbbreviatedIriIn(entity.getForwardRelation(), ontology);
			var cardinality = getCardinality(entity.getSources().iterator().next(), entity.getForwardRelation());
			return String.join(" ", cardinality, name).trim();
		} else {
			return entity.isFunctional() ? getCardinality("0", "1") : "";
		}
	}

	public static String getReverseLabel(Ontology ontology, RelationBase base) {
		if (base.getReverseRelation() != null) {
			var name = getLabel(ontology, base.getReverseRelation());
			var cardinality = getCardinality(base.getTargets().iterator().next(), base.getReverseRelation());
			return String.join(" ", cardinality, name).trim();
		} else {
			return base.isInverseFunctional() ? getCardinality("0", "1") : "";
		}
	}

	public static String getLabel(Ontology ontology, UnreifiedRelation relation) {
		var name = getAbbreviatedIriIn(relation, ontology);
		var cardinality = getCardinality(relation.getSources().iterator().next(), relation);
		var supers = OmlSearch.findSuperTerms(relation, getScope(ontology)).stream().map(r -> getLabel(ontology, r)).collect(Collectors.joining(","));
		return String.join(" ", cardinality, name, supers.length()>0 ? "\n{subsets "+supers+"}" : "").trim();
	}

	public static String getLabel(Ontology ontology, StructuredProperty property) {
		var name = getAbbreviatedIriIn(property, ontology);
		var cardinality = getCardinality(property.getDomains().iterator().next(), property);
		var supers = OmlSearch.findSuperTerms(property, getScope(ontology)).stream().map(r -> getLabel(ontology, r)).collect(Collectors.joining(","));
		return String.join(" ", cardinality, name, supers.length()>0 ? "\n{subsets "+supers+"}" : "").trim();
	}

	public static String getLabel(Ontology ontology, Classifier classifier, ScalarProperty property) {
		var name = getAbbreviatedIriIn(property, ontology);
		var ranges = property.getRangeList().stream().map(i -> getLabel(ontology, i)).collect(Collectors.joining(" & "));
		var cardinality = getCardinality(classifier, property);
		var supers = OmlSearch.findSuperTerms(property, getScope(ontology)).stream().map(r -> getLabel(ontology, r)).collect(Collectors.joining(","));
		return String.join(" ", name, ":", ranges, (cardinality.length()>0 ? cardinality : ""), supers.length()>0 ? "\n{subsets "+supers+"}" : "").trim();
	}
	
	public static String getLabel(Ontology ontology, NamedInstance instance) {
		var types = OmlSearch.findTypeAssertions(instance, getScope(ontology)).stream()
				.map(a -> getLabel(ontology, a.getType()))
                .collect(Collectors.joining(", "));
		
		var iri = getAbbreviatedIriIn(instance, ontology);
		var stereotypes = (types != null && types.length()>0) ? "«"+types+"»" : "";
		
		// HACK: a poorman's attempt at making the label look centered without access to font info
		var len1 = stereotypes.length();
		var len2 = iri.length();
	    var max_len = Math.max(len1, len2);
	    var spaces1 = (max_len - len1);
	    var spaces2 = (max_len - len2);
	    var centered_stereotypes = (len1>0) ? (" ".repeat(spaces1) + stereotypes + "\n") : "";
	    var centered_iri = " ".repeat(spaces2) + iri;
		
		return centered_stereotypes + centered_iri;
	}

    public static String getForwardLabel(Ontology ontology, RelationInstance instance) {
		return OmlSearch.findTypeAssertions(instance, getScope(ontology)).stream()
				.map(a -> a.getType())
				.filter(t -> t instanceof RelationEntity)
				.map(r -> ((RelationEntity)r).getForwardRelation())
				.map(r -> getLabel(ontology, r))
                .collect(Collectors.joining(", "));
	}

    public static String getLabel(Ontology ontology, PropertyRangeRestrictionAxiom axiom) {
    	String q = (axiom.getKind() == RangeRestrictionKind.ALL) ? "\u2200" : "\u2203";
    	return q + getLabel(ontology, axiom.getProperty());
    }

    public static String getLabel(Ontology ontology, PropertyCardinalityRestrictionAxiom axiom) {
    	var min = (axiom.getKind() == CardinalityRestrictionKind.MAX) ? "0" : String.valueOf(axiom.getCardinality());
    	var max = (axiom.getKind() == CardinalityRestrictionKind.MIN) ? (axiom.getProperty().isFunctional() ? "1" : "*") : String.valueOf(axiom.getCardinality());
    	return getCardinality(min, max) + " " + getLabel(ontology, axiom.getProperty());
    }

    public static String getLabel(Ontology ontology, PropertyValueRestrictionAxiom axiom) {
    	return getLabel(ontology, axiom.getProperty());
    }

    public static String getLabel(Ontology ontology, PropertyValueAssertion assertion) {
		var property = getAbbreviatedIriIn(assertion.getProperty(), ontology);
		if (assertion.getProperty() instanceof ScalarProperty) {
			var value = assertion.getLiteralValue().stream().map(v -> v.getLexicalValue()).collect(Collectors.joining(", "));
			return property+" = "+value;
		} else if (assertion.getProperty() instanceof Relation) {
			return getAbbreviatedIriIn(assertion.getProperty(), ontology);
		}
		return null;
	}

	public static String getLabel(Vocabulary vocabulary, Predicate predicate) {
		if (predicate instanceof TypePredicate)
			return getLabel(vocabulary, (TypePredicate)predicate);
		if (predicate instanceof RelationEntityPredicate)
			return getLabel(vocabulary, (RelationEntityPredicate)predicate);
		if (predicate instanceof PropertyPredicate)
			return getLabel(vocabulary, (PropertyPredicate)predicate);
		if (predicate instanceof SameAsPredicate)
			return getLabel(vocabulary, (SameAsPredicate)predicate);
		if (predicate instanceof DifferentFromPredicate)
			return getLabel(vocabulary, (DifferentFromPredicate)predicate);
		return "";
	}

	public static String getLabel(Vocabulary vocabulary, TypePredicate predicate) {
		String type = predicate.getType() != null ? getLabel(vocabulary, predicate.getType()) : "null";
		return type+"("+getLabel(vocabulary, predicate.getArgument())+")";
	}
	
	public static String getLabel(Vocabulary vocabulary, RelationEntityPredicate predicate) {
		String entity = predicate.getType() != null ? getLabel(vocabulary, predicate.getType()) : "null";
		return entity+"("+getLabel(vocabulary, predicate.getArgument1())+", "+getLabel(vocabulary, predicate.getArgument())+ ", "+getLabel(vocabulary, predicate.getArgument2())+")";
	}

	public static String getLabel(Vocabulary vocabulary, PropertyPredicate predicate) {
		String property = predicate.getProperty() != null ? getLabel(vocabulary, predicate.getProperty()) : "null";
		return property+"("+getLabel(vocabulary, predicate.getArgument1())+", "+getLabel(vocabulary, predicate.getArgument2())+")";
	}

	public static String getLabel(Vocabulary vocabulary, SameAsPredicate predicate) {
		return "sameAs("+getLabel(vocabulary, predicate.getArgument1())+", "+getLabel(vocabulary, predicate.getArgument2())+")";
	}

	public static String getLabel(Vocabulary vocabulary, DifferentFromPredicate predicate) {
		return "differentFrom("+getLabel(vocabulary, predicate.getArgument1())+", "+getLabel(vocabulary, predicate.getArgument2())+")";
	}

	public static String getLabel(Vocabulary vocabulary, Argument arg) {
		if (arg.getVariable() != null) {
			return arg.getVariable();
		} else if (arg.getLiteral() != null) {
			return arg.getLiteral().getLexicalValue();
		} else {
			return getLabel(vocabulary, arg.getInstance());
		}
	}
	
	public static String getCardinality(Classifier context, SemanticProperty property) {
		var min = getMinCardinality(context, property);
		var max = getMaxCardinality(context, property);
		return getCardinality(min, max);
	}

	public static String getCardinality(String min, String max) {
		if (min.equals(max)) {
			return "["+min+"]";
		} else if (min.equals("0") && max.equals("*")) {
			return "";
		} else {
			return "["+min+".."+max+"]";
		}
	}

	public static String getMinCardinality(Classifier context, SemanticProperty property) {
		var exactAxiom = context.getOwnedPropertyRestrictions().stream()
			.filter(a -> a instanceof PropertyCardinalityRestrictionAxiom)
			.map(a -> (PropertyCardinalityRestrictionAxiom)a)
			.filter(a -> a.getProperty() == property)
			.filter(a -> a.getRange() == null)
			.filter(a -> a.getKind() == CardinalityRestrictionKind.EXACTLY)
			.findFirst().orElse(null);
		
		if (exactAxiom != null) {
			return String.valueOf(exactAxiom.getCardinality());
		} 
		
		var minAxiom = context.getOwnedPropertyRestrictions().stream()
				.filter(a -> a instanceof PropertyCardinalityRestrictionAxiom)
				.map(a -> (PropertyCardinalityRestrictionAxiom)a)
				.filter(a -> a.getProperty() == property)
				.filter(a -> a.getRange() == null)
				.filter(a -> a.getKind() == CardinalityRestrictionKind.MIN)
				.findFirst().orElse(null);
		
		if (minAxiom != null) {
			return String.valueOf(minAxiom.getCardinality());
		}
		
		return "0";
	}
	
	public static String getMaxCardinality(Classifier context, SemanticProperty property) {
		var exactAxiom = context.getOwnedPropertyRestrictions().stream()
			.filter(a -> a instanceof PropertyCardinalityRestrictionAxiom)
			.map(a -> (PropertyCardinalityRestrictionAxiom)a)
			.filter(a -> a.getProperty() == property)
			.filter(a -> a.getRange() == null)
			.filter(a -> a.getKind() == CardinalityRestrictionKind.EXACTLY)
			.findFirst().orElse(null);
		
		if (exactAxiom != null) {
			return String.valueOf(exactAxiom.getCardinality());
		} 
		
		var maxAxiom = context.getOwnedPropertyRestrictions().stream()
				.filter(a -> a instanceof PropertyCardinalityRestrictionAxiom)
				.map(a -> (PropertyCardinalityRestrictionAxiom)a)
				.filter(a -> a.getProperty() == property)
				.filter(a -> a.getRange() == null)
				.filter(a -> a.getKind() == CardinalityRestrictionKind.MAX)
				.findFirst().orElse(null);
		
		if (maxAxiom != null) {
			return String.valueOf(maxAxiom.getCardinality());
		}

		if (property.isFunctional()) {
			return "1";
		}

		return "*";
	}
	
    //--------------

	private static Set<Member> getAllMembers(Ontology ontology) {
		return OmlRead.getImportedOntologyClosure(ontology, true).stream()
				.flatMap(i -> OmlRead.getMembers(i).stream())
				.collect(Collectors.toSet());
	}

	private static Set<Statement> getAllStatements(Ontology ontology) {
		return OmlRead.getImportedOntologyClosure(ontology, true).stream()
				.flatMap(i -> OmlRead.getStatements(i).stream())
				.collect(Collectors.toSet());
	}

	public static Set<Member> getVisualizableMembers(Ontology ontology) {
		return getAllMembers(ontology);
	}

	public static Set<Member> getLocalVisualizableMembers(Vocabulary vocabulary) {
		var statements = new HashSet<Member>();
		statements.addAll(OmlRead.getStatements(vocabulary));
		return statements;
	}

	public static Set<PropertyRestrictionAxiom> getVisualizablePropertyRestrictions(Vocabulary vocabulary) {
		return getAllStatements(vocabulary).stream()
				.filter(m -> m instanceof Classifier)
				.map(e -> (Classifier)e)
				.flatMap(e -> e.getOwnedPropertyRestrictions().stream())
				.filter(r -> !(r.getProperty() instanceof ScalarProperty))
				.collect(Collectors.toSet());
	}

	public static Set<SpecializationAxiom> getVisualizableSpecializations(Vocabulary vocabulary) {
		return getAllStatements(vocabulary).stream()
				.filter(s -> s instanceof SpecializableTerm)
				.map(s -> (SpecializableTerm)s)
				.flatMap(e -> e.getOwnedSpecializations().stream())
				.collect(Collectors.toSet());
	}

	public static List<SemanticProperty> getVisualizableProperties(Vocabulary vocabulary, Classifier classifier) {
		return getAllMembers(vocabulary).stream()
			.filter(s -> s instanceof SemanticProperty)
			.map(s -> (SemanticProperty)s)
			.filter(p -> p.getDomainList().contains(classifier))
			.sorted((p1, p2) -> p1.getName().compareTo(p2.getName()))
			.collect(Collectors.toList());
	}

	public static List<Literal> getVisualizableLiterals(Vocabulary vocabulary, Scalar scalar) {
		return scalar.getOwnedEnumeration() != null ? 
				scalar.getOwnedEnumeration().getLiterals() :
				Collections.emptyList();
	}

	public static Set<NamedInstance> getVisualizableNamedInstances(Description description) {
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
				.flatMap(i -> i.getObject().stream())
				.map(i -> (NamedInstance) i)
				.flatMap(i -> {
					var list = new ArrayList<NamedInstance>();
					list.add(i);
					if (i instanceof RelationInstance) {
						list.addAll(((RelationInstance)i).getSources());
						list.addAll(((RelationInstance)i).getTargets());
					}
					return list.stream();
				}).collect(Collectors.toSet()));
		return instances;
	}

    public static Set<PropertyValueAssertion> getVisualizableLinks(Description description) {
		return description.getOwnedStatements().stream()
				.filter(s -> s instanceof NamedInstance)
				.map(s -> (NamedInstance)s)
				.flatMap(ci -> ci.getOwnedPropertyValues().stream())
				.filter(i -> i.getProperty() instanceof Relation)
				.collect(Collectors.toSet());
	}

	public static List<PropertyValueAssertion> getVisualizableScalarPropertyValues(Description description, NamedInstance instance) {
		return description.getOwnedStatements().stream()
			.filter(s -> s.resolve() == instance)
			.map(s -> (NamedInstance)s)
			.flatMap(i -> i.getOwnedPropertyValues().stream())
			.filter(a -> a.getProperty() instanceof ScalarProperty)
			.map(a -> (PropertyValueAssertion)a)
			.collect(Collectors.toList());
	}

    //---------

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
    			getNewMemberName(vocabulary, "RelationEntity"), 
    			Collections.singletonList(source.getIri()),
    			Collections.singletonList(target.getIri()),
    			false, false, false, false, false, false, false);
    	builder.addForwardRelation(entity, getNewMemberName(vocabulary, "relationEntity"));
    	builder.finish();
    	return entity;
    }

    public static UnreifiedRelation createUnreifiedRelation(Vocabulary vocabulary, Entity source, Entity target) {
    	var builder = new OmlBuilder(vocabulary.eResource().getResourceSet());
    	var relation = builder.addUnreifiedRelation(
    			vocabulary, 
    			getNewMemberName(vocabulary, "UnreifiedRelation"), 
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
    			getNewMemberName(vocabulary, "StructuredProperty"), 
    			Collections.singletonList(source.getIri()),
    			Collections.singletonList(target.getIri()),
    			false); 
    	builder.finish();
    	return property;
    }

    public static ScalarProperty createScalarProperty(Vocabulary vocabulary, Classifier classifier) {
    	var stringIri = "http://www.w3.org/2001/XMLSchema#string";
    	makeMemberAccessibleByIri(vocabulary, stringIri);
    	var builder = new OmlBuilder(vocabulary.eResource().getResourceSet());
    	var property = builder.addScalarProperty(
    			vocabulary, 
    			getNewMemberName(vocabulary, "ScalarProperty"), 
    			Collections.singletonList(classifier.getIri()),
    			Collections.singletonList(stringIri),
    			false); 
    	builder.finish();
    	return property;
    }

    public static Literal createQuotedEnumerationLiteral(Vocabulary vocabulary, Scalar scalar) {
    	var literal = OmlWrite.createQuotedLiteral(vocabulary, "literal", null, null);
    	addEnumerationLiteral(vocabulary, scalar, literal);
    	return literal;
    }
    
    public static Literal createBooleanEnumerationLiteral(Vocabulary vocabulary, Scalar scalar) {
    	var literal = OmlWrite.createBooleanLiteral(false);
    	addEnumerationLiteral(vocabulary, scalar, literal);
    	return literal;
    }

    public static Literal createIntegerEnumerationLiteral(Vocabulary vocabulary, Scalar scalar) {
    	var literal = OmlWrite.createIntegerLiteral(0);
    	addEnumerationLiteral(vocabulary, scalar, literal);
    	return literal;
    }

    public static Literal createDecimalEnumerationLiteral(Vocabulary vocabulary, Scalar scalar) {
    	var literal = OmlWrite.createDecimalLiteral(new BigDecimal(0));
    	addEnumerationLiteral(vocabulary, scalar, literal);
    	return literal;
    }

    public static Literal createDoubleEnumerationLiteral(Vocabulary vocabulary, Scalar scalar) {
    	var literal = OmlWrite.createDoubleLiteral(0);
    	addEnumerationLiteral(vocabulary, scalar, literal);
    	return literal;
    }

    private static void addEnumerationLiteral(Vocabulary vocabulary, Scalar scalar, Literal literal) {
		var axiom = scalar.getOwnedEnumeration();
		if (axiom != null) {
			axiom.getLiterals().add(literal);
		} else {
	    	var builder = new OmlBuilder(vocabulary.eResource().getResourceSet());
			axiom = builder.addLiteralEnumerationAxiom(vocabulary, scalar.getIri(), literal);
			builder.finish();
		}
   }
    
    public static List<RangeRestrictionKind> getRangeRestrictionKindLiterals(EObject axiom) {
    	return RangeRestrictionKind.VALUES;
    }

    public static List<CardinalityRestrictionKind> getCardinalityRestrictionKindLiterals(EObject axiom) {
    	return CardinalityRestrictionKind.VALUES;
    }

    public static List<SemanticProperty> getCandidateSemanticProperties(Classifier classifier) {
    	return OmlRead.getOntologies(classifier.eResource().getResourceSet()).stream()
    		.flatMap(o -> OmlRead.getMembers(o).stream())
    		.filter(m -> m instanceof SemanticProperty)
    		.map(m -> (SemanticProperty)m)
    		.collect(Collectors.toList());
    }

    public static void parseLiteralValue(Literal literal, String newValue) {
    	try {
	    	if (literal instanceof QuotedLiteral) {
	    		var aLiteral = (QuotedLiteral)literal;
	    		Pattern pattern = Pattern.compile("\"(.*)\"(?:\\^\\^(.+))?(?:\\$(.+))?");
	    		Matcher matcher = pattern.matcher(newValue);
	    		if (matcher.matches()) {
		    		var v = matcher.group(1);
		    		aLiteral.setValue(v);
		    		var t = matcher.group(2);
		    		if (t != null) {
		    			var type = (Scalar) OmlRead.getMemberByAbbreviatedIri(literal.getOntology(), t);
		    			if (type == null) {
		    				type = (Scalar) OmlRead.getMemberByAbbreviatedIri(literal.eResource().getResourceSet(), t);
		    				OmlWrite.addImport(literal.getOntology(), type.getOntology());
		    			}
		    			((QuotedLiteral) literal).setType(type);
		    		} else {
		    			((QuotedLiteral) literal).setType(null);
		    		}
		    		var l = matcher.group(3);
		    		if (l != null) {
		    			((QuotedLiteral) literal).setLangTag(l);
		    		} else {
		    			((QuotedLiteral) literal).setLangTag(null);
		    		}
	    		}
	    	} else if (literal instanceof BooleanLiteral) {
	    		var v = Boolean.valueOf(newValue);
	    		((BooleanLiteral)literal).setValue(v);
	    	} else if (literal instanceof IntegerLiteral) {
	    		var v = Integer.valueOf(newValue);
	    		((IntegerLiteral)literal).setValue(v);
	    	} else if (literal instanceof DoubleLiteral) {
	    		var v = Double.valueOf(newValue);
	    		((DoubleLiteral)literal).setValue(v);
	    	} else if (literal instanceof DecimalLiteral) {
	    		var v = new BigDecimal(newValue);
	    		((DecimalLiteral)literal).setValue(v);
	    	}
    	} catch(Exception e) {
    		// silently ignore
    	}
    }
}
