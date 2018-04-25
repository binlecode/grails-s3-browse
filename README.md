# grails-s3-browse



# grails-logtime
Grails sample application for simple S3 file browse and management.

[![Build Status](https://travis-ci.org/binlecode/grails-logtime.svg?branch=dev-g31)](https://travis-ci.org/binlecode/grails-logtime)
 
## INTRODUCTION

Grails-S3-Browse is a Grails 3 application that provides simple GSP web interface for general S3 object browsing and management.
Basic functionalities include:
- paginated object listing
- bookmark based object listing
- prefix based object filtering
- detailed view of object's metadata
- simple object download

This application also supports username/password based access control based on Spring Security plugin.
It pre-loads three ROLEs: admin, write and read, that can be associated with user management.  

This repository contains source code of Grails-logtime plugin, and a sample Grails application.

## PREREQUISITES

The Grails framework used in this application is with version 3.2.3. It works best with Oracle JDK 8+.

This application also needs a data store to save user account and security control data. By default it is assuming a local MongoDB. 
This can be changed to a common database such as H2-DB, Mysql, etc.   


## CONFIGURATION



In host Grails application grails-app/conf/application.yml

```yaml
# config MongoDB in env specific config file
---
grails:
    mongodb:
        url: "mongodb://localhost:27017/s3browse"

# config AWS SDK in env specific config file
---
grails:
    plugin:
        awssdk:
            accessKey:
            secretKey:
```

## CHANGELOG

#### 0.2
* support file download, upload and deletion
* add spring security support in classic mode: username / password
* add Travis-CI build support

#### 0.1
* support basic object browsing with common pagination and prefix filtering

## CONTRIBUTORS

Bin Le (bin.le.code@gmail.com)


## LICENSE

Apache License Version 2.0. (http://www.apache.org/licenses/)