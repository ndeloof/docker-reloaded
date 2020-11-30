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



