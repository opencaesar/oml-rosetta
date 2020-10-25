# Rosetta

[![Build Status](https://travis-ci.org/opencaesar/oml-rosetta.svg?branch=master)](https://travis-ci.org/opencaesar/oml-rosetta)
[![Release](https://img.shields.io/github/v/release/opencaesar/oml-rosetta?label=Download)](https://github.com/opencaesar/oml-rosetta/releases/latest)
[![Updatesite](https://img.shields.io/badge/p2-updatesite-yellow.svg?longCache=true)](https://bintray.com/opencaesar/p2/oml-rosetta/_latestVersion)


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

You can install the Rosetta update site into an existing ```Eclipse``` using (Help->Install New Software->Add->Location):

```https://dl.bintray.com/opencaesar/p2/oml-rosetta/releases/<version>```

## Install RCP

You can install the Rosetta RCP by downloading a ```rosetta-<platform>.zip``` from the following URL and unzip it:

```https://github.com/opencaesar/oml-rosetta/releases/tag/<version>```

**Note**: on MacOS, the application is currently NOT signed, so you may get an error like the app `is damaged and can't be opened`. To get around that, you can run the following command in terminal:
```
xattr -cr /path/to/Rosetta.app
```

## Create or Import OML projects

You can import existing OML projects or create new ones using the File->New or File->Import wizards.

**Note**: When you have an OML project in the Project Explorer view, there is a filter that hides the Gradle `build` folder by default. To disable that filter, click on the view's menu (in top right corner), and select `Filters and Customization` and then make sure `Gradle Build Folder` is unselected.

## OML Version
For a list of available Rosetta versions, click [here](https://github.com/opencaesar/oml-rosetta/releases)
| Rosetta | OML   |
|---------|-------|
| 0.4.x   | 0.7.5 |
| 0.3.x   | 0.7.3 |
| 0.2.x   | 0.7.0 |
| 0.1.x   | 0.6.2 |
