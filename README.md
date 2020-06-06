# Rosetta

[![Build Status](https://travis-ci.org/opencaesar/oml-rosetta.svg?branch=master)](https://travis-ci.org/opencaesar/oml-rosetta)
[![Download](https://api.bintray.com/packages/opencaesar/rcp/oml-rosetta/images/download.svg) ](https://bintray.com/opencaesar/rcp/oml-rosetta/_latestVersion)
[![Updatesite](https://img.shields.io/badge/p2-updatesite-yellow.svg?longCache=true)](https://bintray.com/opencaesar/p2/oml-rosetta/_latestVersion)

An Eclipse extension that supports [OML](https://opencaesar.github.io/oml-spec) natively and is published as a p2 update site and an RCP.


## Clone
```
  git clone https://github.com/opencaesar/oml-rosetta.git
  cd oml-rosetta
```

## Build

Requirements: Java 8, maven 3.6+
```
  cd io.opencaesar.rosetta.parent
  mvn verify
```

## Release

Replace \<version\> by the version, e.g., 1.2
```
  ./setversion <version>
  git tag -a <version> -m "<version>"
  git push origin <version>
```

## Install Update Site

You can install the Rosetta update site into an existing ```Eclipse``` using (Help->Install New Software->Add->Location):

```https://dl.bintray.com/opencaesar/p2/oml-rosetta/releases/<version>```

## Install RCP

You can install the Rosetta RCP by downloading a ```rosetta-<platform>.zip``` from the following URL:

```https://dl.bintray.com/opencaesar/rcp/oml-rosetta/releases/<version>```

## OML Version
| Rosetta | OML   |
|---------|-------|
| 0.1     | 0.6.2 |
| 0.2     | 0.7.0 |
