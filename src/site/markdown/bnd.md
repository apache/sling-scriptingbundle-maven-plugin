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