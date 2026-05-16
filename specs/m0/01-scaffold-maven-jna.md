
# M0.1 — Scaffold Maven com JNA

**Marco:** M0 — Projeto Java + JNA + GTK Window  
**Dependências:** Nenhuma  
**Entrega:** `mvn compile exec:java` roda sem erro e printa "Jauri OK"

## Objetivo

Criar a estrutura base do projeto Jauri com Maven e JNA, com teste TDD validando que o binding JNA funciona.

## TDD (Test-First)

### Teste: LibC.INSTANCE.getpid() retorna PID real

Arquivo: `src/test/java/jauri/jna/LibCTest.java`

```java
package jauri.jna;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LibCTest {
    @Test
    void getpid_returnsPositivePid() {
        int pid = LibC.INSTANCE.getpid();
        assertTrue(pid > 0, "PID deve ser positivo, got: " + pid);
    }

    @Test
    void printf_printsToStdout() {
        // printf retorna número de bytes escritos
        int result = LibC.INSTANCE.printf("Jauri OK\n");
        assertTrue(result > 0, "printf deve retornar bytes escritos");
    }
}
```

### Teste: Main.main() printa mensagem

Arquivo: `src/test/java/jauri/MainTest.java`

```java
package jauri;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @Test
    void main_printsJauriOk() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        Main.main(new String[]{});
        assertTrue(out.toString().contains("Jauri OK"),
            "Main deve printar 'Jauri OK'");
    }
}
```

### Teste: pom.xml tem dependências corretas (validação de build)

Teste indireto: `mvn compile` compila sem erro.

## Implementação

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.thalleslmf</groupId>
    <artifactId>jauri</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <jna.version>5.14.0</jna.version>
        <junit.version>5.10.1</junit.version>
    </properties>

    <dependencies>
        <!-- JNA: bindings nativos sem JNI -->
        <dependency>
            <groupId>net.java.dev.jna</groupId>
            <artifactId>jna</artifactId>
            <version>${jna.version}</version>
        </dependency>

        <!-- Testes -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Executa app sem precisar de jar empacotado -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <mainClass>jauri.Main</mainClass>
                </configuration>
            </plugin>

            <!-- Plugin JUnit 5 para mvn test -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
            </plugin>
        </plugins>
    </build>
</project>
```

### src/main/java/jauri/Main.java

```java
package jauri;

import jauri.jna.LibC;

/**
 * Entry point do Jauri Runtime.
 * 
 * Inicializa o binding JNA e verifica que o ambiente nativo está funcional.
 * Marcos posteriores substituirão este main por JauriApp com ciclo de vida.
 */
public class Main {
    public static void main(String[] args) {
        // Verificação de sanidade: JNA carregou a libc corretamente
        int pid = LibC.INSTANCE.getpid();
        LibC.INSTANCE.printf("Jauri OK (pid=%d)\n", pid);
    }
}
```

### src/main/java/jauri/jna/LibC.java

```java
package jauri.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Binding JNA para funções da libc (C standard library).
 * 
 * Usado inicialmente para verificar que JNA funciona no ambiente.
 * As funções GTK3 e WebKitGTK terão bindings separados (Gtk3.java, WebKit.java).
 * 
 * Padrão JNA:
 * - Interface extends Library
 * - Métodos mapeiam 1:1 com funções C
 * - INSTANCE é o singleton carregado por Native.load()
 */
public interface LibC extends Library {
    LibC INSTANCE = Native.load("c", LibC.class);

    /** Retorna o PID do processo atual. */
    int getpid();

    /** Printf formatado no stdout. Retorna bytes escritos. */
    int printf(String format, Object... args);

    /** Sleep por microseconds. 1s = 1000000. */
    int usleep(int microseconds);
}
```

### Makefile

```makefile
.PHONY: build run test clean

build:
	mvn compile

run:
	mvn exec:java

test:
	mvn test

clean:
	mvn clean
```

## Critério de Aceite

```bash
mvn test                          # Testes JUnit passam
mvn compile exec:java -Dexec.mainClass=jauri.Main
# Output: Jauri OK (pid=XXXXX)
```

`LibC.INSTANCE.getpid()` retorna um PID real do sistema (> 0).

## Edge Cases

- **JNA não encontrado**: Maven baixa automaticamente (central repository)
- **libc não disponível**: impossível em Linux (libc é fundamental)
- **Múltiplas chamadas printf**: INSTANCE é singleton thread-safe
