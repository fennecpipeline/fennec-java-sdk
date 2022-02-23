# Fennec Pipelines Java SDK

The Java SDK to write Fennec Pipelines.

## Core

The core SDK contains features such as Pipeline design.

To import it, add following dependency:

```xml

<dependency>
    <groupId>org.fennecpipeline</groupId>
    <artifactId>java-sdk-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### How to

#### Declare a stage

```java
public class MyPipeline {
    
    public static void main(String[] args) {
        stage("Stage name", (context) -> {
            // logic here
        });
    }
}
```

#### Declare parallel stage

```java
public class MyPipeline {
    
    public static void main(String[] args) {
        parallel("Parallel Name", Map.of("Stage 1a", (context) -> {
            // Logic stage 1a
        }, "Stage 1b", (context) -> {
            // Logic stage 1b
        }));
    }
}

```

#### Declare a deployment

```java
public class MyPipeline {
    
    public static void main(String[] args) {
        deploy("Staging", (context) -> {
            // logic here
        });
    }
}

```

#### Declare parallel deployment

```java
public class MyPipeline {
    
    public static void main(String[] args) {
        deploy("Staging", "region", Map.of("eu-west-1", (context) -> {
            // Logic eu-west-1
        }, "eu-west-2", (context) -> {
            // Logic eu-west-2
        }));
    }
}

```

#### Declare a deployment with a rollback strategy

```java
public class MyPipeline {
    
    public static void main(String[] args) {
        deploy("Staging", (context) -> {
            // Load logic
        }, (context) -> {
            // Rollback logic, called in case load failed
        });
    }
}

```

#### Execute a command locally

```java
public class MyPipeline {
    
    public static void main(String[] args) {
        stage("Stage name", (context) -> {
            exec("echo", "hello world");
        });
    }
}
```

#### Execute a command locally with a timeout

```java
public class MyPipeline {
    
    public static void main(String[] args) {
        stage("Stage name", (context) -> {
            // in seconds
            exec(200L, "echo", "hello world");
        });
    }
}
```

#### Make a pipeline fail

With a message:

```java
public class MyPipeline {
    
    public static void main(String[] args) {
        stage("Stage name", (context) -> {
            fail("An error occurred");
        });
    }
}
```

With an exception:

```java
public class MyPipeline {
    
    public static void main(String[] args) {
        stage("Stage name", (context) -> {
            try {
                throw new Exception("Stage failed");
            } catch(Exception e) {
                fail("An error occurred", e);
            }
        });
    }
}
```

#### Get an env

With a message:

```java
import java.util.Optional;

public class MyPipeline {

    public static void main(String[] args) {
        stage("Stage name", (context) -> {
            Optional<String> valueEnv1 = env("MY_ENV_ONE");
            String valueEnv1 = env("MY_ENV_ONE", "Default-value");
        });
    }
}
```

## Utilities

The utilities package provides useful tools such as Http, Serialization, Surefire report analysis etc

To import it, add following dependency:

```xml

<dependency>
    <groupId>org.fennecpipeline</groupId>
    <artifactId>java-sdk-utilities</artifactId>
    <version>1.0.0</version>
</dependency>
```

### How to

#### Make a http call

```java
public class MyPipeline {
    
    public static void main(String[] args) {
        stage("An Http GET", (context) -> {
            Http.Response<String> response = get("http://localhost:8080/test?q=hello")
                    .header("Content-Type", "text/plain")
                    .andReturn();
            assertThat(response.getStatus(), equalTo(200));
            assertThat(response.getBody(), equalTo("Hello world!"));
        });

        stage("An Http POST with serialization", (context) -> {
            Response<Hello> response = post("http://localhost:8080/test")
                    .entity(new Name("John"), ContentTypes.APPLICATION_JSON)
                    .andReturn(Hello.class, ContentTypes.APPLICATION_JSON);

            assertThat(response.getStatus(), equalTo(201));
            assertThat(response.getBody(), equalTo(new Hello("John")));
        });
    }
}

```

> Please note that currently, following content types are supported: `application/json`, `text/html`, `application/xhtml+xml`, `application/xml`, `text/plain`

#### Serialize / Deserialize

```java
public class MyPipeline {
    
    public static void main(String[] args) {
        stage("Json", (context) -> {
            String contentString = writeJSON(new Name("John"));
            Name name = readJSON(contentString, Name.class);
            JsonNode nameNode = readJSON(contentString);
        });

        stage("Yaml", (context) -> {
            String contentString = writeYAML(new Name("John"));
            Name name = readYAML(contentString, Name.class);
            JsonNode nameNode = readYAML(contentString);
        });

        stage("Xml", (context) -> {
            String contentString = writeXML(new Name("John"));
            Name name = readXML(contentString, Name.class);
            JsonNode nameNode = readXML(contentString);
        });

        stage("Properties", (context) -> {
            String contentString = writePROPERTIES(new Name("John"));
            Name name = readPROPERTIES(contentString, Name.class);
            JsonNode nameNode = readPROPERTIES(contentString);
        });
    }
}
```

#### Read surefire report

```java
public class MyPipeline {
    
    public static void main(String[] args) {
        stage("Maven Test", (context) -> {
            log.info("Launching tests");
            try {
                exec("mvn", "verify");
            } catch (ExecCommandException e) {
                fail("Unexpected error during mvn execution", e);
            } finally {
                context.getStageContext().setTestResults(Surefire.getTestsResults("Unit tests"));
            }
        });
    }
}
```

## Kubernetes

The Kubernetes package provides useful tools to interact with Kubernetes. It uses your default configuration, but you can still override it.

To import it, add following dependency:

```xml

<dependency>
    <groupId>org.fennecpipeline</groupId>
    <artifactId>java-sdk-kubernetes-extension</artifactId>
    <version>1.0.0</version>
</dependency>
```

### How to

#### Execute a command in a container

```java
public class MyPipeline {
    
    public static void main(String[] args) {
        stage("Stage name", (context) -> {
            ubernetesExecService kubernetesExecService = new KubernetesExecService(client,
                    "test",
                    "test",
                    "test-container",
                    200L);
            CommandOutput output = kubernetesExecService.execCommand("echo", "Hello\nworld");
        });
    }
}
```
