# React Frontend for Spring Boot PetClinic demo
[![Build Status](https://travis-ci.org/spring-petclinic/spring-petclinic-reactjs.svg?branch=master)](https://travis-ci.org/spring-petclinic/spring-petclinic-reactjs)

This project is a port of the [Spring (Boot) PetClinic demo](https://github.com/spring-projects/spring-petclinic) with a frontend built using [ReactJS](https://facebook.github.io/react/) and
[TypeScript](https://www.typescriptlang.org/). 

I have tried to make as few modifications to the backend code as necessary to the [spring-boot branch](https://github.com/spring-projects/spring-petclinic/tree/springboot) of the original sample project.
Mainly I've added the new package `org.springframework.samples.petclinic.web.api`
that contains the REST Api that is used by the React frontend. In this package most of the classes are taken 
from the [angularjs version](https://github.com/spring-projects/spring-petclinic/tree/angularjs) of the demo.

## Related projects

Note there is another Spring PetClinic example that uses React: [spring-petclinic-graphql](https://github.com/spring-petclinic/spring-petclinic-graphql). Beside React that example uses **GraphQL** for API queries instead of the REST API.

## Contribution

If you like to help and contribute (there's lot root for improvements! I've collected a list of ideas [here: TODO.md](TODO.md)) you're more than welcome! Please open an issue or contact me on [Twitter](https://twitter.com/nilshartmann) so we can discuss together!


## Prerequisites

The following items should be installed in your system:
* **Java 17 or higher** - Required for Spring Boot 3.x
* **Node.js** (v18 or higher recommended)
* **npm** (comes with Node.js)

## Install and run

### Quick Reference

| Application | URL |
|-------------|-----|
| Frontend (React) | http://localhost:3000/ |
| Backend API | http://localhost:9966/petclinic/api/ |
| Swagger API Docs | http://localhost:9966/petclinic/swagger-ui.html |

### Step 1: Start the Backend Server

Note: Spring Boot Server App must be running before starting the client!

Launch a Terminal and run from the project's root folder:
```bash
# On Linux/Mac:
./mvnw spring-boot:run

# On Windows (PowerShell):
.\mvnw.cmd spring-boot:run
```

When the server is running you can verify the API is accessible:
```bash
# On Linux/Mac:
curl http://localhost:9966/petclinic/api/pettypes
```

You can also access the Swagger API documentation at:
```
http://localhost:9966/petclinic/swagger-ui.html
```

### Step 2: Start the Frontend Client

Open a **new terminal** and navigate to the `client` folder:

```bash
cd client
```

Install dependencies:
```bash
npm install
```

Start the development server:
```bash
# On Linux/Mac:
PORT=3000 npm start

# On Windows (PowerShell):
$env:PORT=3000; npm start

# On Windows (CMD):
set PORT=3000 && npm start
```

Open http://localhost:3000 in your browser.

### Backend Configuration

| Setting | Value |
|---------|-------|
| Server Port | 9966 |
| Context Path | /petclinic/ |
| Database | HSQLDB (in-memory, auto-populated) |
| Security | Disabled by default |

### Running Unit Tests

Run all backend unit tests:
```bash
# On Linux/Mac:
./mvnw test

# On Windows (PowerShell):
.\mvnw.cmd test
```

Run a specific test class:
```bash
# On Linux/Mac:
./mvnw test -Dtest=OwnerRestControllerTests

# On Windows (PowerShell):
.\mvnw.cmd test -Dtest=OwnerRestControllerTests
```

Run a specific test method:
```bash
# On Linux/Mac:
./mvnw test -Dtest=OwnerRestControllerTests#testGetOwnerSuccess

# On Windows (PowerShell):
.\mvnw.cmd test -Dtest=OwnerRestControllerTests#testGetOwnerSuccess
```

(Why not use the same server for backend and frontend? Because Webpack does a great job for serving JavaScript-based SPAs and I think it's not too uncommon to run this kind of apps using two dedicated server, one for backend, one for frontend)

## Feedback

In case you have any comments, questions, bugs, enhancements feel free to open an issue in this repository.
If you you want to follow me on twitter, my handle is [@nilshartmann](https://twitter.com/nilshartmann).
 
------
 
# From the original sample README file:

## Understanding the Spring Petclinic application with a few diagrams
<a href="https://speakerdeck.com/michaelisvy/spring-petclinic-sample-application">See the presentation here</a>


## Running petclinic locally
```
	git clone https://github.com/spring-projects/spring-petclinic.git
	cd spring-petclinic
	git checkout springboot
	./mvnw spring-boot:run
```

You can then access petclinic here: http://localhost:9966/petclinic/

## In case you find a bug/suggested improvement for Spring Petclinic
Our issue tracker is available here: https://github.com/spring-projects/spring-petclinic/issues


## Database configuration

In its default configuration, Petclinic uses an in-memory database (HSQLDB) which
gets populated at startup with data. A similar setup is provided for MySql in case a persistent database configuration is needed.
Note that whenever the database type is changed, the data-access.properties file needs to be updated and the mysql-connector-java artifact from the pom.xml needs to be uncommented.

You may start a MySql database with docker:

```
docker run -e MYSQL_ROOT_PASSWORD=petclinic -e MYSQL_DATABASE=petclinic -p 3306:3306 mysql:5.7.8
```

## Working with Petclinic in Eclipse/STS

### prerequisites
The following items should be installed in your system:
* Java 17 or higher (required for Spring Boot 3.x)
* Maven 3 (optional - Maven Wrapper is included: `mvnw` / `mvnw.cmd`)
* Node.js v18+ and npm (for the React frontend)
* git command line tool (https://help.github.com/articles/set-up-git)
* Eclipse/IntelliJ with Maven support (optional, for IDE development)


### Steps:

1) In the command line
```
git clone https://github.com/spring-projects/spring-petclinic.git
```
2) Inside Eclipse
```
File -> Import -> Maven -> Existing Maven project
```


## Looking for something in particular?

<table>
  <tr>
    <th width="300px">Spring Boot Configuration</th><th width="300px"></th>
  </tr>
  <tr>
    <td>The Main Class</td>
    <td><a href="/src/main/java/org/springframework/samples/petclinic/PetClinicApplication.java">PetClinicApplication.java</a></td>
  </tr>
  <tr>
    <td>Properties Files</td>
    <td>
      <a href="/src/main/resources/application.properties">application.properties</a>
    </td>
  </tr>
  <tr>
    <td>Caching</td>
    <td>Use of EhCache <a href="/src/main/java/org/springframework/samples/petclinic/config/CacheConfig.java">CacheConfig.java</a> <a href="/src/main/resources/ehcache.xml">ehcache.xml</a></td>
  </tr>
  <tr>
    <td>Dandelion</td>
    <td>DatatablesFilter, DandelionFilter and DandelionServlet registration <a href="/src/main/java/org/springframework/samples/petclinic/config/DandelionConfig.java">DandelionConfig.java</a></td>
  </tr>
  <tr>
    <td>Spring MVC - XML integration</td>
    <td><a href="/src/main/java/org/springframework/samples/petclinic/config/CustomViewsConfiguration.java">CustomViewsConfiguration.java</a></td>
  </tr>
</table>


<table>
  <tr>
    <th width="300px">Others</th><th width="300px">Files</th>
  </tr>
 <tr>
    <td>JSP custom tags</td>
    <td>
      <a href="/src/main/webapp/WEB-INF/tags">WEB-INF/tags</a>
      <a href="/src/main/webapp/WEB-INF/jsp/owners/createOrUpdateOwnerForm.jsp">createOrUpdateOwnerForm.jsp</a></td>
  </tr>
  <tr>
    <td>Bower</td>
    <td>
      <a href="/pom.xml">bower-install maven profile declaration inside pom.xml</a> <br />
      <a href="/bower.json">JavaScript libraries are defined by the manifest file bower.json</a> <br />
      <a href="/.bowerrc">Bower configuration using JSON</a> <br />
      <a href="/src/main/resources/spring/mvc-core-config.xml#L30">Resource mapping in Spring configuration</a> <br />
      <a href="/src/main/webapp/WEB-INF/jsp/fragments/staticFiles.jsp#L12">sample usage in JSP</a></td>
    </td>
  </tr>
  <tr>
    <td>Dandelion-datatables</td>
    <td>
      <a href="/src/main/webapp/WEB-INF/jsp/owners/ownersList.jsp">ownersList.jsp</a>
      <a href="/src/main/webapp/WEB-INF/jsp/vets/vetList.jsp">vetList.jsp</a>
      <a href="/src/main/webapp/WEB-INF/web.xml">web.xml</a>
      <a href="/src/main/resources/dandelion/datatables/datatables.properties">datatables.properties</a>
   </td>
  </tr>
</table>


## Interaction with other open source projects

One of the best parts about working on the Spring Petclinic application is that we have the opportunity to work in direct contact with many Open Source projects. We found some bugs/suggested improvements on various topics such as Spring, Spring Data, Bean Validation and even Eclipse! In many cases, they've been fixed/implemented in just a few days.
Here is a list of them:

<table>
  <tr>
    <th width="300px">Name</th>
    <th width="300px"> Issue </th>
  </tr>

  <tr>
    <td>Spring JDBC: simplify usage of NamedParameterJdbcTemplate</td>
    <td> <a href="https://jira.springsource.org/browse/SPR-10256"> SPR-10256</a> and <a href="https://jira.springsource.org/browse/SPR-10257"> SPR-10257</a> </td>
  </tr>
  <tr>
    <td>Bean Validation / Hibernate Validator: simplify Maven dependencies and backward compatibility</td>
    <td>
      <a href="https://hibernate.atlassian.net/browse/HV-790"> HV-790</a> and <a href="https://hibernate.atlassian.net/browse/HV-792"> HV-792</a>
      </td>
  </tr>
  <tr>
    <td>Spring Data: provide more flexibility when working with JPQL queries</td>
    <td>
      <a href="https://jira.springsource.org/browse/DATAJPA-292"> DATAJPA-292</a>
      </td>
  </tr>  
  <tr>
    <td>Eclipse: validation bug when working with .tag/.tagx files (has only been fixed for Eclipse 4.3 (Kepler)). <a href="https://github.com/spring-projects/spring-petclinic/issues/14">See here for more details.</a></td>
    <td>
      <a href="https://issuetracker.springsource.com/browse/STS-3294"> STS-3294</a>
    </td>
  </tr>    
</table>


# Contributing

The [issue tracker](https://github.com/spring-projects/spring-petclinic/issues) is the preferred channel for bug reports, features requests and submitting pull requests.

For pull requests, editor preferences are available in the [editor config](https://github.com/spring-projects/spring-petclinic/blob/master/.editorconfig) for easy use in common text editors. Read more and download plugins at <http://editorconfig.org>.




