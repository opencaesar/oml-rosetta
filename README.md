# Rosetta

[![Build Status](https://app.travis-ci.com/opencaesar/oml-rosetta.svg?branch=master)](https://app.travis-ci.com/github/opencaesar/oml-rosetta)
[![Release](https://img.shields.io/github/v/release/opencaesar/oml-rosetta?label=download)](https://github.com/opencaesar/oml-rosetta/releases/latest)
[![Updatesite](https://img.shields.io/badge/p2-updatesite-yellow.svg?longCache=true)](https://github.com/opencaesar/oml-rosetta-p2)


An Eclipse RCP (and plugin) that supports working with the [Ontological Modeling Language (OML)](https://opencaesar.github.io/oml-spec).

![OML Rosetta Workbench](https://raw.githubusercontent.com/opencaesar/oml-rosetta/master/io.opencaesar.rosetta.rcp/splash.bmp)

## Clone
```
  git clone https://github.com/opencaesar/oml-rosetta.git
  cd oml-rosetta
```

## Build

Requirements: Java 11, maven 3.6+
```
  mvn verify
```

## Development

To setup a development environment for oml-rosetta:

Preparation:

- Install/use a recent version of Eclipse IDE for Java and DSL Developers (e.g., 2021-09)
- Launch the Eclipse IDE and create a new workspace (recommended)
- Switch to the Java perspective from Window -> Perspective -> Open Perspective -> Other -> Java
- Show the Project Explorer view (if hidden) by selecting Window -> Show View -> Project Explorer
- Turn off automatic build from Project -> Build automatically (uncheck)

Import:

- Import the cloned project using File -> Import -> Maven -> Existing Maven Projects -> Browse (select the root of the clone folder) -> Finish
- If you get prompted with a dialog titled "Discover m2e connectors", click Finish and follow the prompts to install "Tycho Project Configuations" and restart Eclipse IDE
- At this point, you should have all projects loaded in your workspace

Build:

- In Project Explorer, navigate to the file io.opencaesar.rosetta.parent/io.opencaesar.rosetta.target/io.opencaesar.rosetta.target.target and double click to open its editor
- Wait for the resolution of the target platform to finish (watch the percentage at the bottom-right of the IDE window)
- Once the target platform is resolved, click in th editor on the link (top-right) saying "Set as Active Target Platform" then close the editor
- Turn on automatic build from Project -> Build automatically (check)
- The build will start, wait for it to finish (watch the percentage at the bottom-right of the IDE window)
- At this point, you should have all projects building without errors

To change version:

- In Project Explorer, navigate to the file io.opencaesar.rosetta.parent/version.txt and double click to open its editor (note the current version)
- Using a terminal window, navigate to the root of the clone folder
- Execute the script ./setversion `<new-version>` (replace `<new-version>`)
- Back in Eclipse IDE, right click on the root project in Project Explorer view and select Refresh
- Wait for the build to finish

## Install Rosetta Update Site

You can install the Rosetta p2 update site into an existing `Eclipse` using (Help->Install New Software->Add->Location):

Visit https://github.com/opencaesar/oml-rosetta-p2 for URL options

## Install Rosetta RCP

You can install the Rosetta RCP by downloading a ```rosetta-<platform>.zip``` from the following URL and unzip it:

```https://github.com/opencaesar/oml-rosetta/releases/tag/<version>```

**Note**: on MacOS, the application is currently NOT signed, so you may get an error like the app `is damaged and can't be opened`. To get around that, you can run the following command in terminal:
```
xattr -cr /path/to/Rosetta.app
```

## Create or Import OML projects in Rosetta

You can import existing OML projects or create new ones using the File->New or File->Import wizards.

> When you have an OML project in the Project Explorer view, there is a filter that hides the Gradle `build` folder by default. To disable that filter, click on the view's menu (in top right corner), and select `Filters and Customization` and then make sure `Gradle Build Folder` is unselected.

## Run analysis tools in Rosetta

Show the Gradle views by selecting Window -> Show View -> Other... -> Gradle -> (select both views) -> Open

Once in Gradle Tasks view, choose from the view menu (the ... at the top right): Show All Tasks

The Gradle Tasks view should show all your projects (from project explorer). Expand your project and navigate the specific tasks (e.g., other/build) in your gradle script. Double click on them to execute them. 

The following are common tasks:

other/build: to build the project

publishing/publishToMavenLocal: to publish to maven local

other/startFuseki: to start a Fuseki server

other/owlQuery: to run the SPARQL queries

other/stopFuseki: to stop a Fuseki server

> After each Gradle operation above, right click on the relevant project in Project Explorer view and select Refresh and inspect the results in the build folder
