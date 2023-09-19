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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.diagram.DDiagram;
import org.eclipse.sirius.diagram.DDiagramElement;
import org.eclipse.sirius.diagram.DSemanticDiagram;
import org.eclipse.sirius.viewpoint.DSemanticDecorator;

import io.opencaesar.oml.AnnotationProperty;
import io.opencaesar.oml.Classifier;
import io.opencaesar.oml.Concept;
import io.opencaesar.oml.ConceptInstance;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionMember;
import io.opencaesar.oml.Element;
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
import io.opencaesar.oml.StructureInstance;
import io.opencaesar.oml.StructuredProperty;
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

    public static boolean findIsKindOf(NamedInstance instance, String classifierAbbreviatedIri) {
    	var classifier = (Classifier) OmlRead.getMemberByAbbreviatedIri(instance.getOntology(), classifierAbbreviatedIri);
		return (classifier != null) ? OmlSearch.findIsKindOf(instance, classifier) : false;
	}

    public static boolean findIsTypeOf(NamedInstance instance, String classifierAbbreviatedIri) {
    	var classifier = (Classifier) OmlRead.getMemberByAbbreviatedIri(instance.getOntology(), classifierAbbreviatedIri);
		return (classifier != null) ? OmlSearch.findIsTypeOf(instance, classifier) : false;
	}

    public static boolean findIsKindOf(PropertyValueAssertion assertion, String propertyAbbreviatedIri) {
    	var property = (SemanticProperty) OmlRead.getMemberByAbbreviatedIri(assertion.getOntology(), propertyAbbreviatedIri);
		return (property != null) ? OmlSearch.findIsSubTermOf(assertion.getProperty(), property) : false;
	}

    public static boolean findIsTypeOf(PropertyValueAssertion assertion, String propertyAbbreviatedIri) {
    	var property = (SemanticProperty) OmlRead.getMemberByAbbreviatedIri(assertion.getOntology(), propertyAbbreviatedIri);
		return (property != null) ? assertion.getProperty() == property : false;
	}

    //--------------
    
    public static Set<Element> findAnnotationValues(NamedInstance instance, String propertyAbbreviatedIri) {
    	var property = (AnnotationProperty) OmlRead.getMemberByAbbreviatedIri(instance.getOntology(), propertyAbbreviatedIri);
		Set<Element> values = (property != null) ? OmlSearch.findAnnotationValues(instance, property) : Collections.emptySet();
		return values;
	}

    public static Element findAnnotationValue(NamedInstance instance, String propertyAbbreviatedIri) {
		Set<Element> values = findAnnotationValues(instance, propertyAbbreviatedIri);
		return values.iterator().next();
	}

    public static boolean findIsAnnotatedBy(NamedInstance instance, String propertyAbbreviatedIri) {
    	var property = (AnnotationProperty) OmlRead.getMemberByAbbreviatedIri(instance.getOntology(), propertyAbbreviatedIri);
		return (property != null) ? OmlSearch.findIsAnnotatedBy(instance, property) : false;
	}

    public static Set<Element> findPropertyValues(NamedInstance instance, String propertyAbbreviatedIri) {
    	var property = (ScalarProperty) OmlRead.getMemberByAbbreviatedIri(instance.getOntology(), propertyAbbreviatedIri);
		Set<Element> values = (property != null) ? OmlSearch.findPropertyValues(instance, property) : Collections.emptySet();
		return values;
	}

    public static Element findPropertyValue(NamedInstance instance, String propertyAbbreviatedIri) {
		Set<Element> values = findPropertyValues(instance, propertyAbbreviatedIri);
		return values.iterator().next();
	}

    //------------------
    
    public static Collection<NamedInstance> findTargetInstancesRecursively(NamedInstance instance, String relationyAbbreviatedIri, boolean includeRoot) {
		return OmlRead.closure(instance, includeRoot, i -> findTargetInstances(i, relationyAbbreviatedIri));
    }

    public static Collection<NamedInstance> getSourceInstancesRecursively(NamedInstance instance, String relationyAbbreviatedIri, boolean includeRoot) {
		return OmlRead.closure(instance, includeRoot, i -> findSourceInstances(i, relationyAbbreviatedIri));
    }

    public static NamedInstance findTargetInstance(NamedInstance instance, String relationyAbbreviatedIri) {
		List<NamedInstance> instances = findTargetInstances(instance, relationyAbbreviatedIri);
		return !instances.isEmpty() ? instances.get(0) : null;
	}

    public static NamedInstance findSourceInstance(NamedInstance instance, String relationyAbbreviatedIri) {
		List<NamedInstance> instances = findSourceInstances(instance, relationyAbbreviatedIri);
		return !instances.isEmpty() ? instances.get(0) : null;
	}


    public static List<NamedInstance> findTargetInstances(NamedInstance instance, String relationyAbbreviatedIri) {
    	List<NamedInstance> instances = new ArrayList<>();
    	var relation = (Relation) OmlRead.getMemberByAbbreviatedIri(instance.getOntology(), relationyAbbreviatedIri);
		if (relation != null) {
			instances.addAll(OmlSearch.findInstancesRelatedAsTargetTo(instance, relation));
			if (relation.getInverse() != null) {
				instances.addAll(OmlSearch.findInstancesRelatedAsSourceTo(instance, relation.getInverse()));
			}
		}
		return instances;
	}

    public static List<NamedInstance> findSourceInstances(NamedInstance instance, String relationyAbbreviatedIri) {
    	List<NamedInstance> instances = new ArrayList<>();
    	var relation = (Relation) OmlRead.getMemberByAbbreviatedIri(instance.eResource().getResourceSet(), relationyAbbreviatedIri);
		if (relation != null) {
			instances.addAll(OmlSearch.findInstancesRelatedAsSourceTo(instance, relation));
			if (relation.getInverse() != null) {
				instances.addAll(OmlSearch.findInstancesRelatedAsTargetTo(instance, relation.getInverse()));
			}
		}
		return instances;
	}

    public static Set<RelationInstance> findOutgoingRelationInstances(NamedInstance instance, String relationyEntityAbbreviatedIri) {
    	final var relationEntity = (RelationEntity) OmlRead.getMemberByAbbreviatedIri(instance.getOntology(), relationyEntityAbbreviatedIri);
		Set<RelationInstance> instances = OmlSearch.findRelationInstancesWithSource(instance);
		instances.removeIf(i -> !OmlSearch.findTypes(i).contains(relationEntity));
		return instances;
	}

    public static Set<RelationInstance> findIncomingRelationInstances(NamedInstance instance, String relationyEntityAbbreviatedIri) {
    	final var relationEntity = (RelationEntity) OmlRead.getMemberByAbbreviatedIri(instance.getOntology(), relationyEntityAbbreviatedIri);
		Set<RelationInstance> instances = OmlSearch.findRelationInstancesWithTarget(instance);
		instances.removeIf(i -> !OmlSearch.findTypes(i).contains(relationEntity));
		return instances;
	}

    //----------------
    
    public static ConceptInstance createConceptInstance(Description context, String typeAbbreviatedIri) {
    	var concept = (Concept) OmlRead.getMemberByAbbreviatedIri(context, typeAbbreviatedIri);
    	makeMemberAccessibleByAbbreiatedIri(context, typeAbbreviatedIri);
    	var builder = new OmlBuilder(context.eResource().getResourceSet());
    	var instance = builder.addConceptInstance(context, getNewMemberName(context, "ConceptInstance"));
    	builder.addTypeAssertion(context, instance.getIri(), concept.getIri());
    	builder.finish();
    	return instance;
    }

    public static ConceptInstance createConceptInstance(Description context, String typeAbbreviatedIri, NamedInstance subject, String predicateAbbreviatedIri) {
    	var type = (Concept) OmlRead.getMemberByAbbreviatedIri(context, typeAbbreviatedIri);
    	makeMemberAccessibleByAbbreiatedIri(context, typeAbbreviatedIri);
    	var builder = new OmlBuilder(context.eResource().getResourceSet());
    	var instance = builder.addConceptInstance(
    			context, 
    			getNewMemberName(context, "ConceptInstance"));
    	builder.addTypeAssertion(context, instance.getIri(), type.getIri());
    	builder.finish();
    	addPropertyValue(context, subject, predicateAbbreviatedIri, (ConceptInstance) instance);
    	return instance;
    }

    public static RelationInstance createRelationInstance(Description context, NamedInstance source, NamedInstance target, String typeAbbreviatedIri) {
    	var type = (RelationEntity) OmlRead.getMemberByAbbreviatedIri(context, typeAbbreviatedIri);
    	makeMemberAccessibleByAbbreiatedIri(context, typeAbbreviatedIri);
    	var builder = new OmlBuilder(context.eResource().getResourceSet());
    	var instance = builder.addRelationInstance(
    			context, 
    			getNewMemberName(context, "RelationInstance"),
    			Collections.singletonList(source.getIri()),
    			Collections.singletonList(target.getIri()));
    	builder.addTypeAssertion(context, instance.getIri(), type.getIri());
    	builder.finish();
    	return instance;
    }

    public static void addPropertyValue(Ontology context, Instance subject, String predicateAbbreviatedIri, NamedInstance object) {
    	var property = (Relation) OmlRead.getMemberByAbbreviatedIri(context, predicateAbbreviatedIri);
    	makeMemberAccessibleByAbbreiatedIri(context, predicateAbbreviatedIri);
    	OmlWrite.addPropertyValueAssertion(
    			context, 
    			subject,
    			property,
    			object);
    }

    public static void addPropertyValue(Ontology context, Instance subject, String predicateAbbreviatedIri, StructureInstance object) {
    	var property = (StructuredProperty) OmlRead.getMemberByAbbreviatedIri(context, predicateAbbreviatedIri);
    	makeMemberAccessibleByAbbreiatedIri(context, predicateAbbreviatedIri);
    	OmlWrite.addPropertyValueAssertion(
    			context, 
    			subject,
    			property,
    			object);
    }

    public static void addPropertyValue(Ontology context, Instance subject, String predicateAbbreviatedIri, Literal object) {
    	var property = (ScalarProperty) OmlRead.getMemberByAbbreviatedIri(context, predicateAbbreviatedIri);
    	makeMemberAccessibleByAbbreiatedIri(context, predicateAbbreviatedIri);
    	OmlWrite.addPropertyValueAssertion(
    			context, 
    			subject,
    			property,
    			object);
    }

    public static void removePropertyValue(Ontology context, Instance subject, String predicateAbbreviatedIri) {
    	var property = (ScalarProperty) OmlRead.getMemberByAbbreviatedIri(context, predicateAbbreviatedIri);
        var oldAssertions = subject.getOwnedPropertyValues().stream()
                .filter(a -> a.getProperty() == property)
                .collect(Collectors.toList());
        for (var oldAssertion : oldAssertions) {
			OmlDelete.delete(oldAssertion);
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
        
    /**
     * Make a member with a given abbreviated iri accessible from a given ontology context by adding an import statement if needed
     * 
     * @param context The given context ontology
     * @param abbreviatedIri the given abbreviated iri of a member
     * @return The member with the given abbreviated iri
     */
    public static Member makeMemberAccessibleByAbbreiatedIri(Ontology context, String abbreviatedIri) {
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
