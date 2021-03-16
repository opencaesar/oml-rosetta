package io.opencaesar.rosetta.oml.wizards

import java.util.LinkedHashMap

class OmlProjectResourceTemplates {
	
	public val uriStartStringsToRewritePrefixes = new LinkedHashMap<String, String>()
	
	public var String bundleIri
	
	public var String gradleProjectGroup
	public var String gradleProjectDescription
	public var String gradleProjectVersion
	
	public var boolean addVocabularyDependency = true
	
	def String catalogXml() '''
		<?xml version="1.0"?>
		<catalog xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog" prefer="public">
			«FOR uriStartStringToRewritePrefix : uriStartStringsToRewritePrefixes.entrySet»
				<rewriteURI uriStartString="«uriStartStringToRewritePrefix.key»" rewritePrefix="«uriStartStringToRewritePrefix.value»"/>
			«ENDFOR»
		</catalog>
	'''
	
	def String buildGradle() '''
		/* 
		 * The Maven coordinates for the project artifact
		 */
		description = '«gradleProjectDescription»'
		group = '«gradleProjectGroup»'
		version = '«gradleProjectVersion»'
		
		
		/* 
		 * The Gradle task dependencies 
		 */
		buildscript {
			repositories {
				mavenLocal()
				maven { url 'https://dl.bintray.com/opencaesar/owl-adapter' }
				maven { url 'https://dl.bintray.com/opencaesar/owl-tools' }
				maven { url 'https://dl.bintray.com/opencaesar/oml-tools' }
				maven { url 'https://dl.bintray.com/opencaesar/oml' }
				maven { url 'http://dl.bintray.com/vermeulen-mp/gradle-plugins' }
				mavenCentral()
			}
			dependencies {
				classpath 'io.opencaesar.owl:owl-query-gradle:+'
				classpath 'io.opencaesar.owl:owl-load-gradle:+'
				classpath 'io.opencaesar.owl:owl-reason-gradle:+'
				classpath 'io.opencaesar.owl:oml2owl-gradle:+'
				classpath 'io.opencaesar.oml:oml-bikeshed-gradle:+'
				classpath 'com.jfrog.bintray.gradle:gradle-bintray-plugin:+'
				classpath 'com.wiredforcode:gradle-spawn-plugin:+'
				// needed since gradle bintray brings an old version of xerces
				configurations.classpath.exclude group: 'xerces', module: 'xercesImpl'
			}
		}
		
		/*
		 * Dependency versions
		 */
		ext {
			«IF addVocabularyDependency»
				vocabulariesVersion = '1.0.+'
			«ENDIF»
			fusekiVersion = '3.16.0' 
		}
		
		/*
		 * The configuration of OML dependencies
		 */
		configurations {
			oml // Include the oml dependencies only
		    fuseki //Include fuseki server
		}
		
		/*
		 * The repositories to look up OML dependencies in
		 */
		repositories {
			mavenLocal()
			«IF addVocabularyDependency»
				maven { url 'https://dl.bintray.com/opencaesar/vocabularies' }
			«ENDIF»
			mavenCentral()
		}
		
		/*
		 * The OML dependencies
		 */
		dependencies {
			«IF addVocabularyDependency»
				oml "io.opencaesar.ontologies:vocabularies:$vocabulariesVersion"
			«ENDIF»
		    fuseki "org.apache.jena:jena-fuseki-server:$fusekiVersion"
		}
		
		/*
		 * A task to extract and merge the OML dependencies
		 */
		task omldependencies(type: Copy) {
		    from configurations.oml.files.collect { zipTree(it) }
		    into file('build/oml')
		}
		
		/*
		 * A task to convert the OML catalog to OWL catalog
		 */
		task oml2owl(type:io.opencaesar.oml2owl.Oml2OwlTask, dependsOn: omldependencies) {
			// OML catalog
			inputCatalogPath = file('catalog.xml')
			// OWL catalog
			outputCatalogPath = file('build/owl/catalog.xml')
		}
		
		/*
		 * A task to run the Openllet reasoner on the OWL catalog
		 */
		task owlreason(type:io.opencaesar.owl.reason.OwlReasonTask, dependsOn: oml2owl) {
			// OWL catalog
			catalogPath = file('build/owl/catalog.xml')
			// Input ontology IRI to reason on
			inputOntologyIri = '«bundleIri»'
			// Entailment statements to generate and the ontologies to persist them in
			specs = [
				'«bundleIri»/classes = ALL_SUBCLASS',
				'«bundleIri»/properties = INVERSE_PROPERTY | ALL_SUBPROPERTY',
				'«bundleIri»/individuals = ALL_INSTANCE | DATA_PROPERTY_VALUE | OBJECT_PROPERTY_VALUE | SAME_AS'
			]
			// Junit error report
			reportPath = file('build/reports/reasoning.xml')
		}
		
		
		task owlload(type:io.opencaesar.owl.load.OwlLoadTask, dependsOn: owlreason) {
			catalogPath = file('build/owl/catalog.xml')
			endpointURL = 'http://localhost:3030/firesat'
		    fileExtensions = ['owl', 'ttl']
		}
		
		task owlquery(type:io.opencaesar.owl.query.OwlQueryTask, dependsOn: owlload) {
			endpointURL = 'http://localhost:3030/firesat'
			queryPath = file('src/sparql')
			resultPath = file('build/frames')
		}

		/*
		 * A task to generate Bikeshed specification for the OML catalog
		 */
		task oml2bikeshed(type: io.opencaesar.oml.bikeshed.Oml2BikeshedTask, dependsOn: omldependencies) {
			// OML catalog
			inputCatalogPath = file('catalog.xml')
			// OWL folder
			outputFolderPath = file('build/bikeshed')
			// Input Ontology Iri
			rootOntologyIri = '«bundleIri»'
			// Publish URL
			publishUrl = 'https://opencaesar.github.io/vocabularies/'
		}
		
		/*
		 * A task to render the Bikeshed specification to HTML
		 */
		task bikeshed2html(dependsOn: oml2bikeshed) {
			doLast {
				exec { commandLine 'chmod', '+x', 'build/bikeshed/publish.sh' }
				exec { commandLine 'build/bikeshed/publish.sh' }
			}
		}

		/*
		 * A task to generate a publishable Zip archive for the OML sources of this project
		 */
		task omlzip(type: Zip) {
			from file('src/oml')
			destinationDir(file('build/libs'))
			archiveBaseName = project.name
			archiveVersion = project.version
		}
		
		/*
		 * A task to build the project, which executes several tasks together
		 */
		task build() {
			dependsOn omlzip
			dependsOn owlreason
			dependsOn bikeshed2html
		}
		
		/*
		 * A task to delete the build artifacts
		 */
		task clean(type: Delete) {
			delete 'build'
		}
		
		/*
		 * Publish to Maven spec
		 */
		apply plugin: 'maven-publish'
		
		publishToMavenLocal.dependsOn omlzip
		
		publishing {
		    publications {
		        maven(MavenPublication) {
		            artifact omlzip
		        }
		    }
		}
		
		/*
		 * Start and stop the Fuseki server
		 */
		apply plugin: 'com.wiredforcode.spawn'
		
		task downloadFuseki(type: Copy) {
		    from configurations.fuseki
		    into file("build/libs")
		}
		
		task startFuseki(type: SpawnProcessTask, dependsOn: downloadFuseki) {
			command "java -jar ${projectDir}/build/libs/jena-fuseki-server-${fusekiVersion}.jar --mem firesat"
			ready "Apache Jena Fuseki ${fusekiVersion}"
		}
		
		task stopFuseki(type: KillProcessTask)
		
	'''
	
}