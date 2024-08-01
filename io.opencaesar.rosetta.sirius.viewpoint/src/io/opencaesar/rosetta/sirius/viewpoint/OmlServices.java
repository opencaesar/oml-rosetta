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
package io.opencaesar.rosetta.sirius.viewpoint;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.sirius.diagram.DDiagram;
import org.eclipse.sirius.diagram.DDiagramElement;
import org.eclipse.sirius.diagram.DSemanticDiagram;
import org.eclipse.sirius.viewpoint.DSemanticDecorator;

import io.opencaesar.oml.AnnotationProperty;
import io.opencaesar.oml.AnonymousInstance;
import io.opencaesar.oml.ConceptInstance;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionMember;
import io.opencaesar.oml.Element;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.Instance;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.NamedInstance;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.PropertyValueAssertion;
import io.opencaesar.oml.Relation;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.SemanticProperty;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.VocabularyMember;
import io.opencaesar.oml.util.OmlBuilder;
import io.opencaesar.oml.util.OmlDelete;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;
import io.opencaesar.oml.util.OmlSwitch;
import io.opencaesar.oml.util.OmlWrite;

/**
 * Services used by Sirius viewpoints on OML models
 * 
 * <p>
 * <strong>EXPERIMENTAL</strong>. This API has been added as part of work in progress.
 * There is no guarantee that this API will work or that it will remain the same. 
 * Please use this API to prototype but consult with the openCAESAR team if you like 
 * to see this class become supported API.
 * </p>
 * 
 */
public class OmlServices {

	public static Set<Resource> getScope(Ontology ontology) {
		return OmlRead.getImportScope(ontology);
	}
	
	/**
	 * Gets the ontology that defines the target element of the given semantic decorator
	 * 
	 * @param decorator The given semantic decorator
	 * @return An ontology
	 */
	public static Ontology getOntology(DSemanticDecorator decorator) {
		return ((Element) decorator.getTarget()).getOntology();
	}

	/**
	 * Gets the description that defines the target element of the given semantic decorator
	 * 
	 * @param decorator The given semantic decorator
	 * @return A description
	 */
	public static Description getDescription(DSemanticDecorator decorator) {
		return (Description) getOntology(decorator);
	}

	/**
	 * Gets the vocabulary that defines the target element of the given semantic decorator
	 * 
	 * @param decorator The given semantic decorator
	 * @return A vocabulary
	 */
	public static Vocabulary getVocabulary(DSemanticDecorator decorator) {
		return (Vocabulary) getOntology(decorator);
	}

	/**
	 * Gets the semantic diagram that (directly or transitively) own the given diagram element
	 * 
	 * @param element The given diagram element
	 * @return A semantic diagram
	 */
	public static DSemanticDiagram getSemanticDiagram(DDiagramElement element) {
		return (DSemanticDiagram) element.getParentDiagram();
	}
	
	/**
	 * Gets the parent element that directly own the given diagram element
	 * 
	 * @param element The given diagram element
	 * @return A parent element
	 */
	public static DDiagramElement getParent(DDiagramElement element) {
		if (element.eContainer() instanceof DDiagramElement) {
			return (DDiagramElement) element.eContainer();
		}
		return null;
	}

	/**
	 * Gets a set of identified elements that are currently not visualized on the given diagram
	 * 
	 * @param diagram
	 * @return
	 */
	public static Set<IdentifiedElement> getUnvisualizedElements(DDiagram diagram) {
		var elements = new LinkedHashSet<IdentifiedElement>();
		var visualized = getVisualizedElements(diagram);
		var ontologies = OmlServices.getLoadedOntologies(diagram);
		for (Ontology o : ontologies) {
			elements.add(o); // add ontologies regardless of visibility
			elements.addAll(OmlRead.getMembers(o).stream()
					.filter(i -> !(visualized.contains(i)))
					.collect(Collectors.toList()));
		}
		return elements;
	}

	/**
	 * Gets a set of identified elements that are currently visualized on the given diagram
	 * 
	 * @param diagram The given diagram
	 * @return A set of identified elements
	 */
	public static Set<IdentifiedElement> getVisualizedElements(DDiagram diagram) {
		return diagram.getOwnedDiagramElements().stream()
				.filter(e -> e.getTarget() instanceof IdentifiedElement)
				.map(e -> (IdentifiedElement) e.getTarget())
				.collect(Collectors.toSet());
	}

	//-----------

	/**
	 * Finds if the given instance is of kind the given entity by abbreviated iri
	 * 
	 * @param instance The given instance
	 * @param entityAbbreviatedIri The given entity by abbreviated iri
	 * @param context the scope
	 * @return true if the instance is of the kind; otherwise false
	 */
    public static boolean findIsKindOf(Instance instance, String entityAbbreviatedIri, Ontology context) {
    	var entity = OmlRead.getMemberByAbbreviatedIri(instance.getOntology(), entityAbbreviatedIri);
		return (entity instanceof Entity) ? OmlSearch.findIsKindOf(instance, (Entity) entity, getScope(context)) : false;
	}

    /**
	 * Finds if the given instance is of type the given entity by abbreviated iri
     * 
	 * @param instance The given instance
	 * @param entityAbbreviatedIri The given entity by abbreviated iri
     * @param context the scope
	 * @return true if the instance is of the type; otherwise false
     */
    public static boolean findIsTypeOf(Instance instance, String entityAbbreviatedIri, Ontology context) {
    	var entity = OmlRead.getMemberByAbbreviatedIri(instance.getOntology(), entityAbbreviatedIri);
		return (entity instanceof Entity) ? OmlSearch.findIsTypeOf(instance, (Entity) entity, getScope(context)) : false;
	}

    /**
	 * Finds if the given assertion references the given property by abbreviated iri or one of its sub properties
     * 
	 * @param assertion The given assertion
	 * @param propertyAbbreviatedIri The given property by abbreviated iri
     * @param context The scope
	 * @return true if the assertion references the property or a sub property; otherwise false
     */
    public static boolean findIsKindOf(PropertyValueAssertion assertion, String propertyAbbreviatedIri, Ontology context) {
    	var property = OmlRead.getMemberByAbbreviatedIri(assertion.getOntology(), propertyAbbreviatedIri);
		return (property instanceof SemanticProperty) ? OmlSearch.findIsSubTermOf(assertion.getProperty(), (SemanticProperty) property, getScope(context)) : false;
	}

    /**
	 * Finds if the given assertion references the given property by abbreviated iri
     * 
	 * @param assertion The given assertion
	 * @param propertyAbbreviatedIri The given property by abbreviated iri
	 * @return true if the assertion references the property; otherwise false
     */
    public static boolean findIsTypeOf(PropertyValueAssertion assertion, String propertyAbbreviatedIri) {
    	var property = OmlRead.getMemberByAbbreviatedIri(assertion.getOntology(), propertyAbbreviatedIri);
		return (property instanceof SemanticProperty) ? assertion.getProperty() == property : false;
	}

    //--------------

    /**
     * Finds values of an annotation property with the given abbreviated iri on the given element
     * 
     * @param element The given identified element
     * @param propertyAbbreviatedIri The given property abbreviated iri
     * @param context The context
     * @return A set of elements representing annotation values
     */
    public static Set<Element> findAnnotationValues(IdentifiedElement element, String propertyAbbreviatedIri, Ontology context) {
    	var property = OmlRead.getMemberByAbbreviatedIri(element.getOntology(), propertyAbbreviatedIri);
   		return (property instanceof AnnotationProperty) ? OmlSearch.findAnnotationValues(element, (AnnotationProperty) property, getScope(context)) : Collections.emptySet();
	}

    /**
     * Finds the first value of an annotation property with the given abbreviated iri on the given element
     * 
     * @param element The given identified element
     * @param propertyAbbreviatedIri The given property abbreviated iri
     * @param context The scope
     * @return An element representing the first annotation value
     */
    public static Element findAnnotationValue(IdentifiedElement element, String propertyAbbreviatedIri, Ontology context) {
		Set<Element> values = findAnnotationValues(element, propertyAbbreviatedIri, context);
		return values.iterator().next();
	}

    /**
     * Finds if the annotation property with the given abbreviated iri is set on the given element
     * 
     * @param element The given identified element
     * @param propertyAbbreviatedIri The given property abbreviated iri
     * @param context The scope
     * @return An element representing the first annotation value
     */
    public static boolean findIsAnnotatedBy(IdentifiedElement element, String propertyAbbreviatedIri, Ontology context) {
    	var property = OmlRead.getMemberByAbbreviatedIri(element.getOntology(), propertyAbbreviatedIri);
		return (property instanceof AnnotationProperty) ? OmlSearch.findIsAnnotatedBy(element, (AnnotationProperty) property, getScope(context)) : false;
	}

    /**
     * Finds the values of a property with the given abbreviated iri set on the given instance
     * 
     * @param instance The given instance
     * @param propertyAbbreviatedIri The given property abbreviated iri
     * @param context The scope
     * @return A set of elements representing the property value
     */
    public static Set<Element> findPropertyValues(Instance instance, String propertyAbbreviatedIri, Ontology context) {
    	var property = OmlRead.getMemberByAbbreviatedIri(instance.getOntology(), propertyAbbreviatedIri);
		return (property instanceof SemanticProperty) ? OmlSearch.findPropertyValues(instance, (SemanticProperty) property, getScope(context)) : Collections.emptySet();
	}

    /**
     * Finds the value of a property with the given abbreviated iri set on the given instance
     * 
     * @param instance The given instance
     * @param propertyAbbreviatedIri The given property abbreviated iri
     * @param context The scope
     * @return An element representing the first property value
     */
    public static Element findPropertyValue(Instance instance, String propertyAbbreviatedIri, Ontology context) {
		Set<Element> values = findPropertyValues(instance, propertyAbbreviatedIri, context);
		return values.iterator().next();
	}

    //------------------
    
    public static Set<NamedInstance> findTargetInstances(Instance source, String relationyAbbreviatedIri, Ontology context) {
    	var relation = OmlRead.getMemberByAbbreviatedIri(source.getOntology(), relationyAbbreviatedIri);
		return (relation instanceof Relation) ? OmlSearch.findInstancesRelatedAsTargetTo(source, (Relation) relation, getScope(context)) : Collections.emptySet();
	}

    public static Collection<NamedInstance> findTargetInstancesRecursively(Instance source, String relationyAbbreviatedIri, boolean includeRoot, Ontology context) {
		return OmlRead.closure(source, includeRoot, i -> findTargetInstances(i, relationyAbbreviatedIri, context));
    }

    public static NamedInstance findTargetInstance(Instance source, String relationyAbbreviatedIri, Ontology context) {
		Set<NamedInstance> instances = findTargetInstances(source, relationyAbbreviatedIri, context);
		return instances.iterator().next();
	}

    public static Set<Instance> findSourceInstances(Instance target, String relationyAbbreviatedIri, Ontology context) {
    	var relation = OmlRead.getMemberByAbbreviatedIri(target.eResource().getResourceSet(), relationyAbbreviatedIri);
		if (relation instanceof Relation && target instanceof NamedInstance) {
			return OmlSearch.findInstancesRelatedAsSourceTo((NamedInstance)target, (Relation) relation, getScope(context));
		}
		return Collections.emptySet();
	}

    public static Collection<Instance> findSourceInstancesRecursively(NamedInstance target, String relationyAbbreviatedIri, boolean includeRoot, Ontology context) {
		return OmlRead.closure(target, includeRoot, i -> findSourceInstances(i, relationyAbbreviatedIri, context));
    }

    public static Instance findSourceInstance(NamedInstance taget, String relationyAbbreviatedIri, Ontology context) {
		Set<Instance> instances = findSourceInstances(taget, relationyAbbreviatedIri, context);
		return instances.iterator().next();
	}

    public static Set<RelationInstance> findOutgoingRelationInstances(NamedInstance source, String relationyEntityAbbreviatedIri, Ontology context) {
    	final var relationEntity = (RelationEntity) OmlRead.getMemberByAbbreviatedIri(source.getOntology(), relationyEntityAbbreviatedIri);
    	if (relationEntity instanceof RelationEntity) {
			var relationInstances = OmlSearch.findRelationInstancesWithSource(source, getScope(context));
			relationInstances.removeIf(i -> !OmlSearch.findTypes(i, getScope(context)).contains(relationEntity));
			return relationInstances;
    	}
    	return Collections.emptySet();
	}

    public static Set<RelationInstance> findIncomingRelationInstances(NamedInstance target, String relationyEntityAbbreviatedIri, Ontology context) {
    	final var relationEntity = (RelationEntity) OmlRead.getMemberByAbbreviatedIri(target.getOntology(), relationyEntityAbbreviatedIri);
    	if (relationEntity instanceof RelationEntity) {
			var relationInstances = OmlSearch.findRelationInstancesWithTarget(target, getScope(context));
			relationInstances.removeIf(i -> !OmlSearch.findTypes(i, getScope(context)).contains(relationEntity));
			return relationInstances;
    	}
    	return Collections.emptySet();
	}

    //----------------
    
    public static List<PropertyValueAssertion> getPropertyValueAssertions(Instance instance, String propertyAbbreviatedIri) {
		return instance.getOwnedPropertyValues().stream()
    		.filter(a -> a.getProperty().getAbbreviatedIri().equals(propertyAbbreviatedIri))
            .collect(Collectors.toList());
	}

	public static List<Element> getPropertyValues(Instance instance, String propertyAbbreviatedIri) {
		return instance.getOwnedPropertyValues().stream()
    		.filter(a -> a.getProperty().getAbbreviatedIri().equals(propertyAbbreviatedIri))
    		.flatMap(a -> a.getValue().stream())
            .collect(Collectors.toList());
	}

	public static ConceptInstance createConceptInstance(Description context, String typeAbbreviatedIri) {
    	var type = OmlRead.getMemberByAbbreviatedIri(context.eResource().getResourceSet(), typeAbbreviatedIri);
    	if (type instanceof Entity) {
	    	var builder = new OmlBuilder(context.eResource().getResourceSet());
	    	var instance = builder.addConceptInstance(context, getNewMemberName(context, ((Entity)type).getName()));
	    	builder.addTypeAssertion(context, instance.getIri(), type.getIri());
	    	builder.finish();
	    	return instance;
    	}
    	return null;
    }

    public static ConceptInstance createConceptInstance(Description context, String typeAbbreviatedIri, Instance source, String relationAbbreviatedIri) {
    	var type = OmlRead.getMemberByAbbreviatedIri(context, typeAbbreviatedIri);
    	var relation = OmlRead.getMemberByAbbreviatedIri(context, relationAbbreviatedIri);
    	if (type instanceof Entity && relation instanceof Relation) {
	    	var builder = new OmlBuilder(context.eResource().getResourceSet());
	    	var instance = builder.addConceptInstance(context, getNewMemberName(context, ((Entity)type).getName()));
	    	builder.addTypeAssertion(context, instance.getIri(), type.getIri());
	    	var owner = (source instanceof NamedInstance) ? ((NamedInstance)source).getIri() : source;
	    	builder.addPropertyValueAssertion(context, owner, relation.getIri(), instance.getIri());
	    	builder.finish();
	    	return instance;
    	}
    	return null;
    }

    public static RelationInstance createRelationInstance(Description context, NamedInstance source, NamedInstance target, String typeAbbreviatedIri) {
    	var type = OmlRead.getMemberByAbbreviatedIri(context, typeAbbreviatedIri);
    	if (type instanceof Entity) {
	    	var builder = new OmlBuilder(context.eResource().getResourceSet());
	    	var instance = builder.addRelationInstance(
	    			context, 
	    			getNewMemberName(context, ((Entity)type).getName()),
	    			Collections.singletonList(source.getIri()),
	    			Collections.singletonList(target.getIri()));
	    	builder.addTypeAssertion(context, instance.getIri(), type.getIri());
	    	builder.finish();
	    	return instance;
    	}
    	return null;
    }

    public static PropertyValueAssertion createPropertyValueAssertion(Instance instance, Element value, String propertyAbbreviatedIri, Ontology context) {
    	var property = OmlRead.getMemberByAbbreviatedIri(context, propertyAbbreviatedIri);
    	if (property instanceof SemanticProperty) {
	    	var builder = new OmlBuilder(context.eResource().getResourceSet());
	    	var owner = (instance instanceof NamedInstance) ? ((NamedInstance)instance).getIri() : instance;
	    	PropertyValueAssertion assertion = null;
	    	if (value instanceof Literal) {
	    		assertion = builder.addPropertyValueAssertion(context, owner, property.getIri(), (Literal)value);
	    	} else if (value instanceof AnonymousInstance) {
	    		assertion = builder.addPropertyValueAssertion(context, owner, property.getIri(), (AnonymousInstance)value);
	    	} else if (value instanceof NamedInstance) {
	    		assertion = builder.addPropertyValueAssertion(context, owner, property.getIri(), ((NamedInstance)value).getIri());
	    	}
	    	builder.finish();
	    	return assertion;
    	}
    	return null;
    }

    public static void addPropertyValue(Instance subject, String relationAbbreviatedIri, NamedInstance object, Ontology context) {
    	var relation = OmlRead.getMemberByAbbreviatedIri(context, relationAbbreviatedIri);
    	if (relation instanceof Relation) {
	    	var builder = new OmlBuilder(context.eResource().getResourceSet());
	    	var owner = (subject instanceof NamedInstance) ? ((NamedInstance)subject).getIri() : subject;
	    	builder.addPropertyValueAssertion(
	    			context, 
	    			owner,
	    			relation.getIri(),
	    			object.getIri());
	    	builder.finish();
    	}
    }

    public static void addPropertyValue(Instance subject, String propertyAbbreviatedIri, AnonymousInstance object, Ontology context) {
    	var property = OmlRead.getMemberByAbbreviatedIri(context, propertyAbbreviatedIri);
    	if (property instanceof Relation) {
	    	var builder = new OmlBuilder(context.eResource().getResourceSet());
	    	var owner = (subject instanceof NamedInstance) ? ((NamedInstance)subject).getIri() : subject;
	    	builder.addPropertyValueAssertion(
	    			context, 
	    			owner,
	    			property.getIri(),
	    			object);
	    	builder.finish();
    	}
    }

    public static void addPropertyValue(Instance subject, String propertyAbbreviatedIri, Literal object, Ontology context) {
    	var property = OmlRead.getMemberByAbbreviatedIri(context, propertyAbbreviatedIri);
    	if (property instanceof ScalarProperty) {
	    	var builder = new OmlBuilder(context.eResource().getResourceSet());
	    	var owner = (subject instanceof NamedInstance) ? ((NamedInstance)subject).getIri() : subject;
	    	builder.addPropertyValueAssertion(
	    			context, 
	    			owner,
	    			property.getIri(),
	    			object);
	    	builder.finish();
    	}
    }

    public static void removePropertyValue(Instance subject, String propertyAbbreviatedIri, Ontology context) {
    	var property = OmlRead.getMemberByAbbreviatedIri(context, propertyAbbreviatedIri);
    	if (property instanceof SemanticProperty) {
	        subject.getOwnedPropertyValues().stream()
                .filter(a -> a.getProperty() == property)
                .forEach(a -> OmlDelete.delete(a));
    	}
    }
    
	public static void setPropertyValue(Instance instance, ScalarProperty property, Object newValue) {
		removePropertyValue(instance, property.getAbbreviatedIri(), instance.getOntology());
		if (newValue != null && newValue.toString().length() > 0) {
			var newLiteral = OmlWrite.createLiteral(newValue);
			addPropertyValue(instance, property.getAbbreviatedIri(), newLiteral, instance.getOntology());
		}
	}
    
    // ---------
    
    public static List<NamedInstance> getOwnedNamedInstances(Description description) {
    	return description.getOwnedStatements().stream()
    			.filter(i -> i instanceof NamedInstance)
    			.map(i -> (NamedInstance)i)
    			.collect(Collectors.toList());
    }
    
    public static Set<NamedInstance> getNamedInstancesInContext(Description context, boolean includeImports) {
    	var instances = new LinkedHashSet<NamedInstance>();
    	
    	var visitor = new OmlSwitch<Void>() {
			@Override
			public Void caseConceptInstance(ConceptInstance object) {
				instances.add((ConceptInstance)object.resolve());
				return null;
			}
			@Override
			public Void caseRelationInstance(RelationInstance object) {
				instances.add((RelationInstance)object.resolve());
				instances.addAll(object.getSources());
				instances.addAll(object.getTargets());
				return null;
			}
			@Override
			public Void casePropertyValueAssertion(PropertyValueAssertion object) {
				if (object.getProperty() instanceof Relation) {
					instances.add((NamedInstance)object.getObject());
				}
				return null;
			}
    	};

		var allDescriptions = new HashSet<Ontology>();
		allDescriptions.add(context);
		if (includeImports) {
    		allDescriptions.addAll(
    			OmlRead.getImportedOntologyClosure(context, false).stream()
    				.filter(o -> o instanceof Description)
    				.collect(Collectors.toSet()));
    	}
    	allDescriptions.forEach(d -> d.eAllContents().forEachRemaining(i -> visitor.doSwitch(i)));

    	return instances;
	}
	
	/**
	 * Gets a list of ontologies loaded in the resource set of the given context object
	 * 
	 * @param context A given object context
	 * @return A list of ontologies
	 */
    public static List<Ontology> getLoadedOntologies(EObject context) {
		return OmlRead.getOntologies(context.eResource().getResourceSet());
    }

	/**
	 * Gets the description that defines the given member
	 * 
	 * @param member The given member
	 * @return A description
	 */
	public static Description getDescription(DescriptionMember member) {
		return (Description) member.getOntology();
	}

	/**
	 * Gets the vocabulary that defines the given member
	 * 
	 * @param member The given member
	 * @return A vocabulary
	 */
	public static Vocabulary getVocabulary(VocabularyMember member) {
		return (Vocabulary) member.getOntology();
	}

   /**
     * Gets a new name for a member of a context ontology derived from a given base name
     * 
     * @param context The given context ontology
     * @param base The given base name of a member
     * @return A valid new name for a member derived from the given base name
     */
    public static String getNewMemberName(Ontology context, String base) {
    	base = base.substring(0, 1).toLowerCase()+base.substring(1);
    	var names = OmlRead.getMembers(context).stream()
    			.map(s -> ((Member)s).getName())
    			.collect(Collectors.toSet());
    	String name = base;
    	int i = 0;
    	while (names.contains(name)) {
    		name = base + ++i;
    	}
    	return name;	
    }
        
    //--------------
    
    /**
     * Make a member with a given abbreviated iri accessible from a given ontology context by adding an import statement if needed
     * 
     * @param context The given context ontology
     * @param abbreviatedIri the given abbreviated iri of a member
     * @return The member with the given abbreviated iri
     */
    public static Member makeMemberAccessibleByAbbreviatedIri(Ontology context, String abbreviatedIri) {
    	abbreviatedIri = abbreviatedIri.trim();
    	Member member = OmlRead.getMemberByAbbreviatedIri(context, abbreviatedIri);
    	if (member == null) {
    		member = OmlRead.getMemberByAbbreviatedIri(context.eResource().getResourceSet(), abbreviatedIri);
    		if (member != null) {
    			OmlWrite.addImport(context, member.getOntology());
    		}
    	}
		if (member == null) {
			Activator.getDefault().getLog().error("Could not resolve "+abbreviatedIri+" in the context of "+context.getIri());
		}
    	return member;
    }

    /**
     * Make a member with a given iri accessible from a given ontology context by adding an import statement if needed
     * 
     * @param context The given context ontology
     * @param iri the given iri of a member
     * @return The member with the given abbreviated iri
     */
    public static Member makeMemberAccessibleByIri(Ontology context, String iri) {
    	Member member = OmlRead.getMemberByIri(context, iri);
    	if (member == null) {
    		member = OmlRead.getMemberByIri(context.eResource().getResourceSet(), iri);
    		if (member == null) {
        		member = OmlRead.getMemberByResolvingIri(context.eResource(), iri);
    		}
    		if (member != null) {
    			OmlWrite.addImport(context, member.getOntology());
    		}
		} 
    	if (member == null) {
			Activator.getDefault().getLog().error("Could not resolve "+iri+" in the context of "+context.getIri());
		}
    	return member;
    }

    /**
     * Make a given member accessible from a given ontology context by adding an import statement if needed
     * 
     * @param context The given context ontology
     * @param member the given member
     */
    public static void makeMemberAccessible(Ontology context, Member member) {
    	if (OmlRead.getMemberByIri(context, member.getIri()) == null) {
			OmlWrite.addImport(context, member.getOntology());
    	}
    }

}
