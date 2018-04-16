Copyright 2018 AT&T Intellectual Property. All rights reserved.
This file is licensed under the CREATIVE COMMONS ATTRIBUTION 4.0 INTERNATIONAL LICENSE
Full license text at https://creativecommons.org/licenses/by/4.0/legalcode

This source repository contains ONAP Policy application code. To build it:
1. using Maven 3
2. git clone http://gerrit.onap.org/r/oparent and copy
oparent/settings.xml to ~/.m2
3. mvn clean install

The Demo template rule is located in template.demo sub-project. Use that project to protoype and test the .drl demo rule. When finished update the archetype-closedloop-demo-rules project with the .drl. Be sure to remove the Setup rule and comment out any simulation/test code.

The other projects are supporting code used by the template.demo project.

