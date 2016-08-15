Unleash Maven Plugin - More Efficient and Reliable Maven Releases
=================================================================
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.itemis.maven.plugins/unleash-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.itemis.maven.plugins/unleash-maven-plugin)

The Unleash Maven Plugin provides functionality to release Maven projects as it is possible with the Maven Release Plugin. While the idea and the core feature (building release artifacts) is the similar, there are many serious differences that make the unleash plugin much more reliable and efficient than the official release plugin. It is furthermore highly extensible and adaptable to your specific requirements when building releases.


The Background
--------------
This plugin has been developed because we had some serious issues with the default release plugin in a customer project. After an excessive search for alternatives we decided to develop our own plugin and make it open-source as it was pretty clear that this will become a bigger project that can significantly add value to the community. There are quite a few very good approaches for producing Maven release Artifacts and dealing with them but none of them could easily be integrated in the project's requirements as it was necessary and most of the approaches we tried left some critical questions open.


The Advantages At A Glance
--------------------------
Using the Unleash Maven Plugin brings some significant advantages over the standard Maven Release Plugin as there are f.i.:

1. **A much higher fault tolerance**
  * The whole process of building the release can be seen as an atomic operation that can (and will) be rolled back in case of an error automatically.
  * The whole process is implemented as a set of processing steps orchestrated by a workflow. Each step that modifies resources (POMs, SCM, local repository) implements an appropriate rollback method that will be triggered automatically. This whole implementation is based on the library [Maven CDI Plugin Utils](https://github.com/shillner/maven-cdi-plugin-utils) which enables dependency injection, a workflow concept and extensibility for Maven plugins.
  * Even SCM commits are possible during the release of a Maven project using the Unleash Maven Plugin. There are only a few cases that prevent the release from being finished when SCM commits are detected.
  * Installation and deployment of the artifacts into the local and remote repository happen as the very last steps when all other possibly failing tasks are processed successfully.
  * There are a number of checks before starting the release build to fail fast in the case that some release conditions are violated (e.g. there are SNAPSHOT dependencies or the released project has been detected in one of the remote repositories).
2. **Building releases faster**
  * The Unleash Maven Plugin uses your local working copy of the project to build the release artifacts rather than checking out a copy from your SCM and building this one again.
  * It performs several checks before starting the actual build to ensure that the release will pass through. This causes the release to fail fast if a condition is not met.
3. **Providing a highly adaptable build process**
  * Additionally to configuring _how_ to perform the several steps of the release process through Mojo parameters you can also configure _what_ steps to perform in order to build the release by overriding and adapting the default workflow of the release Mojo. This allows you to adapt the release process to your exact needs and requirements to better fit your working model. 
4. **Being extendable by nature**
  * Since the Unleash Maven Plugin is built on top of the [Maven CDI Plugin Utils](https://github.com/shillner/maven-cdi-plugin-utils) library it is extendable by nature. This means that concepts such as dependency injection, processing steps and workflow-based orchestration of the steps enable you to implement your own processing steps or take pre-implemented steps of other projects and add them as dependencies to your plugin configuration. Once these steps are available on classpath you can embed them into the processing workflow to extend or redefine its functionality.
5. **Reusable SCM provider implementations**
  * The dependency injection approach and the implementation of the SCM provider registry of the Unleash Maven Plugin allows SCM providers to be implemented, released and deliverd as independent units.
  * Simply add the required scm provider to the plugin dependencies to lay it onto the classpath and it will be found.
  * SCM provider implementations can also be used in other contexts besides this plugin and this is already done. 


What Is It Built On?
--------------------
The Unleash Maven Plugin is built on the new library Maven CDI Plugin Utils which provides some concepts that increases the possibilities of implementing Maven plugins enormously. Is gives you f.i. CDI-based dependency injection and a workflow-based processing model and pushes reusability, extendability and customizablity to a higher level.

More about the library can be found here: [Maven CDI Plugin Utils](https://github.com/shillner/maven-cdi-plugin-utils)


CI Server Integration
---------------------
Releases should be built by Continuous Integration Servers only. This is much safer than building them on a local machine since your personal setup and local repository could influence the release build in an unwanted  way.

On any CI server you have the option to create a sepatate "release job" that sets all necessary optional and calls the  unleash goals manually. When you build this job a new release will be built. Althouth this is possible and the simplest solution it is quite uncomfortable and error prone since you will have to maintain two different build jobs for the same project. To face this problem there is a plugin for the [Jenkins CI server](https://jenkins.io/) which lets you trigger a release build for an existing Maven job. It is able to store plugin settings globally and locally to make release building as easy as clicking a link.

The Jenkins plugin can be found here: [Unleash Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Unleash+Plugin)
Simply install it using the Jenkins Plugin manager and you are all set ;)
