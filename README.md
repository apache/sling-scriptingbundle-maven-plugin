[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/scriptingbundle-maven-plugin/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22scriptingbundle-maven-plugin%22)
[![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/scriptingbundle-maven-plugin.svg)](https://www.javadoc.io/doc/org.apache.sling/scriptingbundle-maven-plugin)
[![Build Status](https://builds.apache.org/buildStatus/icon?job=Sling/sling-scriptingbundle-maven-plugin/master)](https://builds.apache.org/job/Sling/job/sling-scriptingbundle-maven-plugin/job/master)
[![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/job/Sling/job/sling-scriptingbundle-maven-plugin/job/master.svg)](https://builds.apache.org/job/Sling/job/sling-scriptingbundle-maven-plugin/job/master/test_results_analyzer/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-scriptingbundle-maven-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-scriptingbundle-maven-plugin)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-scriptingbundle-maven-plugin&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-scriptingbundle-maven-plugin)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-scriptingbundle-maven-plugin&metric=bugs)](https://sonarcloud.io/dashboard?id=apache_sling-scriptingbundle-maven-plugin)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-scriptingbundle-maven-plugin&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=apache_sling-scriptingbundle-maven-plugin)

Apache Sling Scripting Bundle Maven Plugin
====

The Apache Sling Scripting Bundle Maven Plugin provides support for generating OSGi bundles
that provide embedded or precompiled scripts to perform server-side rendering in an Apache
Sling application. When executed, the plugin will define two project properties
(`org.apache.sling.scriptingbundle.maven.plugin.Require-Capability` and
`org.apache.sling.scriptingbundle.maven.plugin.Provide-Capability`) which can be used to
populate the corresponding bundle headers.

For more details head over to the documentation page from https://sling.apache.org/components/scriptingbundle-maven-plugin/.
