# Rosetta

[![Build Status](https://travis-ci.com/opencaesar/oml-rosetta.svg?branch=master)](https://travis-ci.com/opencaesar/oml-rosetta)
[![Release](https://img.shields.io/github/v/release/opencaesar/oml-rosetta?label=download)](https://github.com/opencaesar/oml-rosetta/releases/latest)
[![Updatesite](https://img.shields.io/badge/p2-updatesite-yellow.svg?longCache=true)](https://github.com/opencaesar/oml-rosetta-p2)


An Eclipse extension that supports [OML](https://opencaesar.github.io/oml-spec) natively and is published as a p2 update site and an RCP.


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

## Install Update Site

You can install the Rosetta p2 update site into an existing ```Eclipse``` using (Help->Install New Software->Add->Location):

Visit https://github.com/opencaesar/oml-rosetta-p2 for URL options

## Install RCP

You can install the Rosetta RCP by downloading a ```rosetta-<platform>.zip``` from the following URL and unzip it:

```https://github.com/opencaesar/oml-rosetta/releases/tag/<version>```

**Note**: on MacOS, the application is currently NOT signed, so you may get an error like the app `is damaged and can't be opened`. To get around that, you can run the following command in terminal:
```
xattr -cr /path/to/Rosetta.app
```

## Create or Import OML projects

You can import existing OML projects or create new ones using the File->New or File->Import wizards.

> When you have an OML project in the Project Explorer view, there is a filter that hides the Gradle `build` folder by default. To disable that filter, click on the view's menu (in top right corner), and select `Filters and Customization` and then make sure `Gradle Build Folder` is unselected.

## Run analysis tools

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

## OML Version
For a list of available Rosetta versions, click [here](https://github.com/opencaesar/oml-rosetta/releases)
| Rosetta | OML   |
|---------|-------|
| 0.9.1   | 0.9.1 |
| 0.9.0   | 0.9.0 |
| 0.8.x   | 0.8.8 |
| 0.7.x   | 0.8.8 |
| 0.6.x   | 0.8.1 |
| 0.5.x   | 0.7.7 |
| 0.4.x   | 0.7.5 |
| 0.3.x   | 0.7.3 |
| 0.2.x   | 0.7.0 |
| 0.1.x   | 0.6.2 |
