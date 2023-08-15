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
		apply plugin: 'base'
		apply plugin: 'oml'
		
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
		ext {
		    // Name of dataset (matches one used in .fuseki.ttl file)
		    dataset = '«gradleProjectName»'
		    // Root ontology IRI of the dataset
		    rootIri = '«bundleIri»'
		}
		
		/*
		 * The repositories to look up OML dependencies in
		 */
		repositories {
		    mavenLocal()
		    mavenCentral()
		}

		/*
		 * The OML dependencies
		 */
		dependencies {
		    oml "io.opencaesar.ontologies:core-vocabularies:5.+"
		}
		
		/*
		 * A task to extract and merge the OML dependencies
		 * @seeAlso https://github.com/opencaesar/oml-tools/blob/master/oml-merge/README.md
		 */
		task downloadDependencies(type:io.opencaesar.oml.merge.OmlMergeTask, group:"oml", dependsOn: configurations.oml) {
		    inputZipPaths = configurations.oml.files
		    outputCatalogFolder = file('build/oml')
		}
		
		/*
		 * A task to convert the OML catalog to OWL catalog
		 * @seeAlso https://github.com/opencaesar/owl-adapter/blob/master/oml2owl/README.md
		 */
		task omlToOwl(type:io.opencaesar.oml2owl.Oml2OwlTask, group:"oml", dependsOn: downloadDependencies) {
		    // OML catalog
		    inputCatalogPath = file('catalog.xml')
		    // OWL catalog
		    outputCatalogPath = file('build/owl/catalog.xml')
		}
		
		/*
		 * A task to run the Openllet reasoner on the OWL catalog
		 * @seeAlso https://github.com/opencaesar/owl-tools/blob/master/owl-reason/README.md
		 */
		task owlReason(type:io.opencaesar.owl.reason.OwlReasonTask, group:"oml", dependsOn: omlToOwl) {
		    // OWL catalog
		    catalogPath = file('build/owl/catalog.xml')
		    // Input ontology IRI to reason on
		    inputOntologyIri = "$rootIri".toString()
		    // Entailment statements to generate and the ontologies to persist them in
		    specs = [
		        "$rootIri/classes = ALL_SUBCLASS".toString(),
		        "$rootIri/properties = INVERSE_PROPERTY | ALL_SUBPROPERTY".toString(),
		        "$rootIri/individuals = ALL_INSTANCE | DATA_PROPERTY_VALUE | OBJECT_PROPERTY_VALUE | SAME_AS".toString()
		    ]
		    // Junit error report
		    reportPath = file('build/reports/reasoning.xml')
		}
		
		/*
		 * Start the headless Fuseki server
		 * @seeAlso https://github.com/opencaesar/owl-tools/blob/master/owl-doc/README.md
		 */
		task startFuseki(type: io.opencaesar.owl.fuseki.StartFusekiTask, group:"oml") {
		    configurationPath = file('.fuseki.ttl')
		    outputFolderPath = file('.fuseki')
		    //webUI = true
		}
		
		/*
		 * Stop the headless Fuseki server
		 * @seeAlso https://github.com/opencaesar/owl-tools/blob/master/owl-fuseki/README.md
		 */
		task stopFuseki(type: io.opencaesar.owl.fuseki.StopFusekiTask, group:"oml") {
		    outputFolderPath = file('.fuseki')
		}
		
		/*
		 * A task to load an OWL catalog to a Fuseki dataset endpoint
		 * @seeAlso https://github.com/opencaesar/owl-tools/blob/master/owl-load/README.md
		 */
		task owlLoad(type:io.opencaesar.owl.load.OwlLoadTask, group:"oml", dependsOn: owlReason) {
		    inputs.files(startFuseki.outputFolderPath) // rerun when fuseki restarts
		    catalogPath = file('build/owl/catalog.xml')
		    endpointURL = "http://localhost:3030/$dataset".toString()
		    fileExtensions = ['owl', 'ttl']
		    iris = [
		        "$rootIri/classes".toString(),
		        "$rootIri/properties".toString(),
		        "$rootIri/individuals".toString()
		    ]
		}
		
		/*
		 * A task to run a set of SPARQL queries on a Fuseki dataset endpoint
		 * @seeAlso https://github.com/opencaesar/owl-tools/blob/master/owl-query/README.md
		 */
		task owlQuery(type:io.opencaesar.owl.query.OwlQueryTask, group:"oml", dependsOn: owlLoad) {
		    endpointURL = "http://localhost:3030/$dataset".toString()
		    queryPath = file('src/sparql')
		    resultPath = file('build/results')
		    format = 'json'
		}

		/*
		 * A task to check the project's build artifacts
		 * @seeAlso https://docs.gradle.org/current/userguide/base_plugin.html
		 */
		tasks.named('check') {
		    dependsOn owlReason
		}
		
		/*
		 * Define project's artifacts
		 */
		task omlZip(type: Zip, group:"oml") {
		    from file('src/oml')
		    include "**/*.oml"
		    destinationDirectory = file('build/libs')
		    archiveBaseName = project.name
		    archiveVersion = project.version
		}

		artifacts.default omlZip
		
		/*
		 * Publish project artifacts to maven
		 */
		apply plugin: 'maven-publish'
		apply plugin: 'signing'
		
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
		
		signing {
		    def pgpSigningKey = project.findProperty('pgpSigningKey')
		    if (pgpSigningKey != null) { pgpSigningKey = new String(pgpSigningKey.decodeBase64()) }
		    def pgpSigningPassword = project.findProperty('pgpSigningPassword')
		    useInMemoryPgpKeys(pgpSigningKey, pgpSigningPassword)
		    sign publishing.publications.maven
		}
		
		gradle.taskGraph.whenReady { taskGraph ->
		    signMavenPublication.onlyIf { taskGraph.allTasks.any{it.name == 'publishMavenPublicationToSonatypeRepository'} }
		}
		
		/*
		 * Integration with the Eclipse IDE
		 */ 
		apply plugin: 'eclipse'
		
		eclipse {
		    synchronizationTasks downloadDependencies
		}
	'''
	
}
