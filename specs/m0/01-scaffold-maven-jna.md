# M0.1 — Scaffold Maven com JNA

**Marco:** M0 — Projeto Java + JNA + GTK Window  
**Dependências:** Nenhuma  
**Entrega:** `mvn compile exec:java` roda sem erro e printa "Jauri OK"

## Objetivo

Criar a estrutura base do projeto Jauri com Maven e JNA.

## Tarefas

1. Criar `pom.xml` com `com.sun.jna:jna:5.14.0` e `exec-maven-plugin`
2. Criar `jauri/Main.java` com método main que printa "Jauri OK"
3. Criar `jauri/jna/LibC.java` com interface JNA pra libc (printf, getpid, sleep)
4. Criar `Makefile` com targets `build`, `run`, `clean`

## Critério de Aceite

```bash
mvn compile exec:java -Dexec.mainClass=jauri.Main
# Output: Jauri OK
```

`LibC.INSTANCE.getpid()` retorna um PID real do sistema.

## Arquivos Esperados

```
pom.xml
Makefile
src/main/java/jauri/Main.java
src/main/java/jauri/jna/LibC.java
```
