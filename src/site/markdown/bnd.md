# bnd Plugin

Starting with version `0.3.0`, the JAR also provides a [bnd plugin](https://bnd.bndtools.org/chapters/870-plugins.html). The simplest configuration (where the defaults are used)
would look like:

```
<plugin>
    <groupId>biz.aQute.bnd</groupId>
    <artifactId>bnd-maven-plugin</artifactId>
    <dependencies>
        <dependency>
            <groupId>${project.groupId}</groupId>
            <artifactId>${project.artifactId}</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

and the following instructions added to your project's `bnd.bnd` file:

```
Require-Capability:             osgi.extender;filter:="(&(osgi.extender=sling.scripting)(version>=1.0.0)(!(version>=2.0.0)))"
-plugin:                        org.apache.sling.scriptingbundle.plugin.bnd.BundledScriptsScannerPlugin
```

The bnd plugin supports the same configuration options as the [Maven `metadata` goal](metadata-mojo.html), using the exact same name for its configuration
properties. Multiple values need to be provided as a quoted comma-separated values list (e.g.
`sourceDirectories="src/main/scripts,src/main/resources/javax.script"`, `scriptEngineMappings="html:htl,js:rhino"`).

For a general usage description please refer to [Usage](usage.html).

## Working with [FileVault](https://jackrabbit.apache.org/filevault/index.html) content package projects

Starting with version 0.5.0, the bnd plugin can be used to scan [FileVault](https://jackrabbit.apache.org/filevault/index.html)
content  package projects. The same concepts from the [generic usage](usage.html) instructions apply, with two differences:

  1. Since content packages contain the serialisation of resource properties, the Docview `.content.xml`
     files will be parsed and the encountered `sling:resourceSuperType` properties will be directly used to generate the appropriate
     OSGi capabilities, making the `extends` file redundant (besides, the `extends` files should not be packed in a content package
     anyways).
  2. When delegating to another resource type, you can define this relationship in your component's
     Docview `.content.xml` file, by using a new multi-value String property - `sling:requiredResourceTypes`. This property is only used 
     by the bnd plugin to generate the correct `Require-Capability` header values. An example is available
     [here](https://github.com/apache/sling-org-apache-sling-scripting-bundle-tracker-it/blob/master/examples/org-apache-sling-scripting-content-package-with-bundle-attached/src/main/content/jcr_root/apps/sling/scripting/example-cp/hello/.content.xml).
     When using the `sling:requiredResourceTypes` Docview property, the `requires` file is redundant.

A trivial example of the bnd plugin configured to extract capabilities from a content package, together with the required plugins to also
generate a `jar` file with the precompiled scripts can be seen [here](https://github.com/apache/sling-org-apache-sling-scripting-bundle-tracker-it/blob/master/examples/org-apache-sling-scripting-content-package-with-bundle-attached/pom.xml).
