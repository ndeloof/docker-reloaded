# docker-reloaded

![docker-reloaded.png](docker-reloaded.png)

## Introduction

This plugin is an attempt to adopt a Docker-first approach in Jenkins

Which basically means:
 - each Jenkins build get a dedicated Docker Agent allocated, fully isolated from
 other builds.
 - Docker agents get created immediately on build schedule
 - Docker agents get stopped as the build complete


## Getting started

This plugin relies on Docker API client [Jocker](https://github.com/ndeloof/jocker)
that is still under development. You'll need to build it from sources as it is
not (yet) available on Maven Central.

## Architecture

Jenkins uses a Java agent on build nodes to control build execution. This
agent is connected to the Jenkins master using a custom 
[remoting protocol](https://github.com/jenkinsci/remoting) (sort of 
revisited [RMI](https://docs.oracle.com/javase/7/docs/platform/rmi/spec/rmiTOC.html)) 
on top of any transport protocol.

This remoting channel is used for multiple purpose, but mainly to control
remote processes running the build from plugin's code executed on master.

```
+------------+                                          +---------(build node)-----+
| Jenkins    | ---- transport (tcp, ssh, jnlp, ...) --- |                          |
|  Master   ============< jenkins remoting >============== java -jar agent.jar     | 
|            |                                          |      |                   |
| build step | . . . . . . . . . . . . . . . . . . . . .| . . .+-> build process   |
|            |                                          |                          |
+------------+                                          +--------------------------+ 
```


Docker-Reloaded plugin uses a Docker container to run the agent, and relies
on Docker's `interactive` (attached) mode to offer a natural transport 
protocol between the container launcher (Jenkins master) and the agent 
(ran as a container).

Docker-Reloaded plugin also uses `docker exec` to create and control build processes
ran inside containers, bypassing the remoting layer.

```
+------------+                                          +---------(docker host)----+
| Jenkins    | ---- docker run --interactive ------------> +--------(container)--+ |
|  Master  ============< jenkins remoting >================= java -jar agent.jar | | 
|            |                                          |  |                     | |
| build step | ---- docker exec --------------------------------> build process  | |
|            |                                          |  +---------------------+ |
+------------+                                          +--------------------------+ 
```


