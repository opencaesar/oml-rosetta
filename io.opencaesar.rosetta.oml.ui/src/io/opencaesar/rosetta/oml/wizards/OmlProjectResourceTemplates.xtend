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
package io.opencaesar.rosetta.oml.wizards

import java.util.LinkedHashMap

class OmlProjectResourceTemplates {
	
	public val uriStartStringsToRewritePrefixes = new LinkedHashMap<String, String>()
	
	public var String baseIri
	public var String bundleIri
	
	public var String gradleProjectName;
	public var String gradleProjectGroup
	public var String gradleProjectTitle
	public var String gradleProjectDescription
	public var String gradleProjectVersion
		
	def String catalogXml() '''
		<?xml version="1.0"?>
		<catalog xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog" prefer="public">
			«FOR uriStartStringToRewritePrefix : uriStartStringsToRewritePrefixes.entrySet»
				<rewriteURI uriStartString="«uriStartStringToRewritePrefix.key»" rewritePrefix="«uriStartStringToRewritePrefix.value»"/>
			«ENDFOR»
		</catalog>
	'''
	
	def String fusekiTtl() '''
		@prefix fuseki:  <http://jena.apache.org/fuseki#> .
		@prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
		@prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> .
		@prefix tdb:     <http://jena.hpl.hp.com/2008/tdb#> .
		@prefix ja:      <http://jena.hpl.hp.com/2005/11/Assembler#> .
		@prefix :        <#> .
		
		[] rdf:type fuseki:Server .
		
		<#service> rdf:type fuseki:Service ;
		    rdfs:label          "«gradleProjectTitle»" ;												# Human readable label for dataset
		    fuseki:name         "«gradleProjectName»" ;												# Name of the dataset in the endpoint url
		    fuseki:serviceReadWriteGraphStore "data" ;											# SPARQL Graph store protocol (read and write)
		    fuseki:endpoint 	[ fuseki:operation fuseki:query ;	fuseki:name "sparql"  ] ;	# SPARQL query service
		    fuseki:endpoint 	[ fuseki:operation fuseki:shacl ;	fuseki:name "shacl" ] ;		# SHACL query service
		    fuseki:dataset      <#dataset> .
		
		## In memory TDB with union graph.
		<#dataset> rdf:type   tdb:DatasetTDB ;
		  tdb:location "--mem--" ;
		  # Query timeout on this dataset (1s, 1000 milliseconds)
		  ja:context [ ja:cxtName "arq:queryTimeout" ; ja:cxtValue "1000" ] ;
		  # Make the default graph be the union of all named graphs.
		  tdb:unionDefaultGraph true .
	'''

	def String buildGradle() '''
		/* 
		 * The Maven coordinates for the project artifact
		 */
		ext.title = '«gradleProjectTitle»'
		description = '«gradleProjectDescription»'
		group = '«gradleProjectGroup»'
		version = '«gradleProjectVersion»'
		
		/* 
		 * The Gradle plugins 
		 */
		apply plugin: 'maven-publish'
		
		/* 
		 * The Gradle task dependencies 
		 */
		buildscript {
			repositories {
				mavenLocal()
				mavenCentral()
			}
			dependencies {
		        classpath 'io.opencaesar.owl:owl-fuseki-gradle:+'
		        classpath 'io.opencaesar.owl:owl-query-gradle:+'
		        classpath 'io.opencaesar.owl:owl-load-gradle:+'
		        classpath 'io.opencaesar.owl:owl-reason-gradle:+'
		        classpath 'io.opencaesar.oml:oml-merge-gradle:+'
		        classpath 'io.opencaesar.adapters:oml2owl-gradle:+'
			}
		}
		
		/*
		 * Dataset-specific variables
		 */
		ext.dataset = [
		    // Name of dataset (matches one used in .fuseki.ttl file)
		    name: '«gradleProjectName»',
		    // Root ontology IRI of the dataset
		    rootOntologyIri: '«bundleIri»',
		]
		
		/*
		 * The repositories to look up OML dependencies in
		 */
		repositories {
		    mavenLocal()
		    mavenCentral()
		}

		/*
		 * The configuration of OML dependencies
		 */
		configurations {
		    oml
		}
		
		/*
		 * Dependency versions
		 */
		ext { 
		    coreVersion = '+'
		}
		
		/*
		 * The OML dependencies
		 */
		dependencies {
		    oml "io.opencaesar.ontologies:core-vocabularies:$coreVersion"
		}
		
		/*
		 * A task to extract and merge the OML dependencies
		 */
		task omlDependencies(type:io.opencaesar.oml.merge.OmlMergeTask, group:"oml") {
		    inputZipPaths = configurations.oml.files
		    outputCatalogFolder = file('build/oml')
		}
		
		/*
		 * A task to convert the OML catalog to OWL catalog
		 */
		task omlToOwl(type:io.opencaesar.oml2owl.Oml2OwlTask, group:"oml", dependsOn: omlDependencies) {
		    // OML catalog
		    inputCatalogPath = file('catalog.xml')
		    // OWL catalog
		    outputCatalogPath = file('build/owl/catalog.xml')
		}
		
		/*
		 * A task to run the Openllet reasoner on the OWL catalog
		 */
		task owlReason(type:io.opencaesar.owl.reason.OwlReasonTask, group:"oml", dependsOn: omlToOwl) {
		    // OWL catalog
		    catalogPath = file('build/owl/catalog.xml')
		    // Input ontology IRI to reason on
		    inputOntologyIri = "$dataset.rootOntologyIri".toString()
		    // Entailment statements to generate and the ontologies to persist them in
		    specs = [
		        "$dataset.rootOntologyIri/classes = ALL_SUBCLASS".toString(),
		        "$dataset.rootOntologyIri/properties = INVERSE_PROPERTY | ALL_SUBPROPERTY".toString(),
		        "$dataset.rootOntologyIri/individuals = ALL_INSTANCE | DATA_PROPERTY_VALUE | OBJECT_PROPERTY_VALUE | SAME_AS".toString()
		    ]
		    // Junit error report
		    reportPath = file('build/reports/reasoning.xml')
		}
		
		/*
		 * Start the headless Fuseki server
		 */
		task startFuseki(type: io.opencaesar.owl.fuseki.StartFusekiTask, group:"oml") {
		    configurationPath = file('.fuseki.ttl')
		    outputFolderPath = file('.fuseki')
		}
		
		/*
		 * Stop the headless Fuseki server
		 */
		task stopFuseki(type: io.opencaesar.owl.fuseki.StopFusekiTask, group:"oml") {
		    outputFolderPath = file('.fuseki')
		}
		
		/*
		 * A task to load an OWL catalog to a Fuseki dataset endpoint
		 */
		task owlLoad(type:io.opencaesar.owl.load.OwlLoadTask, group:"oml", dependsOn: owlReason) {
		    catalogPath = file('build/owl/catalog.xml')
		    endpointURL = "http://localhost:3030/$dataset.name".toString()
		    fileExtensions = ['owl', 'ttl']
		    iris = [
		        "$dataset.rootOntologyIri/classes".toString(),
		        "$dataset.rootOntologyIri/properties".toString(),
		        "$dataset.rootOntologyIri/individuals".toString()
		    ]
		}
		
		/*
		 * A task to run a set of SPARQL queries on a Fuseki dataset endpoint
		 */
		task owlQuery(type:io.opencaesar.owl.query.OwlQueryTask, group:"oml", dependsOn: owlLoad) {
		    endpointURL = "http://localhost:3030/$dataset.name".toString()
		    queryPath = file('src/sparql')
		    resultPath = file('build/results')
		    format = 'json'
		}

		/*
		 * A task to build the project, which executes several tasks together
		 */
		task build(group: "oml") {
		    dependsOn owlReason
		}
		
		/*
		 * A task to delete the build artifacts
		 */
		task clean(type: Delete, group: "oml") {
			delete 'build'
		}
		
		/*
		 * Publish artifact to maven
		 */
		task omlZip(type: Zip, group:"oml") {
		    from file('src/oml')
		    include "**/*.oml"
		    destinationDirectory = file('build/libs')
		    archiveBaseName = project.name
		    archiveVersion = project.version
		}
		
		def pomConfig = {
		    licenses {
		        license {
		            name "The Apache Software License, Version 2.0"
		            url "http://www.apache.org/licenses/LICENSE-2.0.txt"
		            distribution "repo"
		        }
		    }
		    developers {
		        developer {
		            id "melaasar"
		            name "Maged Elaasar"
		            email "melaasar@gmail.com"
		        }
		    }
		    scm {
		        url 'https://github.com/opencaesar/'+rootProject.name
		    }
		}
		
		publishing {
		    publications {
		        maven(MavenPublication) {
		            groupId project.group
		            artifactId project.name
		            version project.version
		            artifact omlZip
		            pom {
		                packaging = 'zip'
		                withXml {
		                    def root = asNode()
		                    if (configurations.find { it.name == 'oml' }) {
		                        def dependencies = root.appendNode('dependencies')
		                        configurations.oml.resolvedConfiguration.resolvedArtifacts.each {
		                            def dependency = dependencies.appendNode('dependency')
		                            dependency.appendNode('groupId', it.moduleVersion.id.group)
		                            dependency.appendNode('artifactId', it.moduleVersion.id.name)
		                            dependency.appendNode('version', it.moduleVersion.id.version)
		                            if (it.classifier != null) {
		                                dependency.appendNode('classifier', it.classifier)
		                                dependency.appendNode('type', it.extension)
		                            }
		                        }
		                    }
		                    root.appendNode('name', project.ext.title)
		                    root.appendNode('description', project.description)
		                    root.appendNode('url', 'https://github.com/opencaesar/'+rootProject.name)
		                    root.children().last() + pomConfig
		                }
		            }
		        }
		    }
		}
		
		/*
		 * Integration with the Eclipse IDE
		 */ 
		apply plugin: 'eclipse'
		
		eclipse {
		    synchronizationTasks omlDependencies
		}
	'''
	
}
