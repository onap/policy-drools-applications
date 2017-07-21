This source repository contains ONAP Policy application code. The settings file only needs to support the standard Maven repositories (e.g. central = http://repo1.maven.org/maven2/), and any proxy settings needed in your environment.

To build it using Maven 3, run: mvn clean install

The Demo template rule is located in template.demo sub-project. Use that project to protoype and test the .drl demo rule. When finished update the archetype-closedloop-demo-rules project with the .drl. Be sure to remove the Setup rule and comment out any simulation/test code.

The other projects are supporting code used by the template.demo project.

