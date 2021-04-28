The Apache Sling Scripting Bundle Maven Plugin helps with generating the OSGi bundle
headers that bundles which provide embedded or precompiled scripts for server-side
rendering in an Apache Sling application require in order to have themselves wired up to the
Apache Sling Servlets Resolver.  When executed, the plugin will define two project properties
(`org.apache.sling.scriptingbundle.maven.plugin.Require-Capability` and
`org.apache.sling.scriptingbundle.maven.plugin.Provide-Capability`) which can be used to
populate the corresponding bundle headers.

Bundles that get extended by the
[`org.apache.sling.servlets.resolver`](https://github.com/apache/sling-org-apache-sling-servlets-resolver)
with these `Requirements` and`Capabilities` will have their scripts made available automatically with added
versioning and dependency support.

Manually defining the `Require-Capability` and `Provide-Capability` bundle headers is error-prone and unnecessary,
as they can be derived from the file-system layout required for scripts by the resolver (for the most part).

Starting with version `0.3.0`, the JAR also provides a BND plugin.

## Usage
General instructions on how to use the Scripting Bundle Maven Plugin can be found on the
[usage](usage.html) page.

In case you still have questions regarding the plugin's usage feel free to contact the
Apache Sling Development List. The posts to the mailing list are archived and could
already contain the answer to your question as part of an older thread. Hence, it is also
worth browsing/searching the mail archive.

If you feel like the plugin is missing a feature or has a defect, you can fill a feature
request or bug report in our issue tracker. When creating a new issue, please provide a
comprehensive description of your concern. Especially for fixing bugs it is crucial that the
developers can reproduce your problem. For this reason, entire debug logs, POMs or most
preferably little demo projects attached to the issue are very much appreciated. Of course,
patches are welcome, too. Contributors can check out the project from our source repository
and will find supplementary information in the Maven guide.

