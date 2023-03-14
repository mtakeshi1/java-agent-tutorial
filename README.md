# java-agent-tutorial

A small project providing explanation of how java agents work and also a skeleton for a java agent

# Introduction - what is exactly a java agent anyway?

A java agent is a special type of program, most commonly used to modify classes (like regular debuggers) of another java
program (or maybe itself) either during class loading time (if the java process to be modified starts with a special
argument) or during execution.
However, it can be used to run arbitraty code in another JVM process if one wants to, for instance, initiate a JMX
server, change logging settings, really any code.
When starting an agent in a process startup (-javaagent), it runs before the Main class does.

# Writing a java agent

The easiest way is to just clone this repo and customize the POM to your liking (your groupId, artifactId, version and
java version). After that, just rewrite the class: ```entrypoint.Agent``` to do what you want.

## Project structure

This project can be cloned to provide a starting point if you want to write your own agent. It provides:

- single jar packaging with maven-shade-plugin
- Manifest entries for the agent and for the standalone java main
- commonly used dependencies (junit and asm for bytecode manipulation) with shade configuration to try to avoid
  classloader issues

### Manifest

The following entries are defined and used by a java agent:

- Premain-Class: fully qualified classname of the agent class to be used when starting a java process with the argument
  -javaagent:. That class nust define a method with the following signature:

```java 
public static void premain(String,Instrumentation) 
```

- Agent-Class: fully qualified name of the agent class to be used when it's attaching to a running process. That class
  nust define a method with the following signature:

```java 
public static void agentmain(String,Instrumentation) 
```

- Can-Redefine-Classes [true / false]: if the agent can call ```redefineClasses``` to, well, redefine some classes
- Can-Retransform-Classes [true / false]: likewise if the agent can append class transformers and trigger
  retransformation of classes

## The instrumentation API

From the agent API, some of the most usefull APIs:

- addTransformer / removeTransformer: adds a class transformers (more on that later)
- appendToBootstrapClassLoaderSearch / appendToSystemClassLoaderSearch modifies the bootstrap / system classloaders
- getAllLoadedClasses: returns all known classes. Must be used to trigger retransformation / redefinition of classes
- redefineClasses: triggers a class redefinition, that is, you can replace the bytecode of an existing class (or new
  class) with new definition, as long as it respects some limitations - in general, you cannot add or remove class
  members or arguments. See https://docs.oracle.com/en/java/javase/18/docs/specs/jvmti.html#RetransformClasses for more
  information. This method is intended to be used when the original bytecode definition is not necessary for the new
  definition - for instance, in a recompilation of the original classes during a debug session.
- retransformClases: triggers a class retransformation of the given classes. That means that the given classes will go
  through all the defined class transformers and the final bytecode will replace the running class. It has the same
  restrictions as redefineClasses and it's intended use case is for when you want to make modification to existing
  bytecode.

## entrypoint.Agent

Main entrypoint for the java agent. If you want to write your own agent, here would be the starting point.

## entrypoing.Main

This is completely optional, but something that I like to do when writing a java agent if its run like a regular jar
file (java -jar <agent>). It does the following:

- if no arguments are provided, it lists all the java process that it can attach to (tipically processes running with
  the EXACT SAME USER and not necessarily ROOT)
- if it has arguments, use the first one to find a process to attach to, either matching (exactly) to the pid, partial
  string of the description (tipically full command like of the java process) or treat it like a regex to match against
  the description or pid
- it it has an extra argument, use that as agent options when attaching

Doing this creates, IMO, a more robust java agent.

## ClassTransformers

A class tranformer is an interface that has the following signature:

```java
public interface ClassFileTransformer {

    /**
     * Transforms the given class file and returns a new replacement class file.
     * This method is invoked when the {@link Module Module} bearing {@link
     * ClassFileTransformer#transform(Module, ClassLoader, String, Class, ProtectionDomain, byte[])
     * transform} is not overridden.
     *
     * @implSpec The default implementation returns null.
     *
     * @param loader                the defining loader of the class to be transformed,
     *                              may be {@code null} if the bootstrap loader
     * @param className             the name of the class in the internal form of fully
     *                              qualified class and interface names as defined in
     *                              <i>The Java Virtual Machine Specification</i>.
     *                              For example, <code>"java/util/List"</code>.
     * @param classBeingRedefined   if this is triggered by a redefine or retransform,
     *                              the class being redefined or retransformed;
     *                              if this is a class load, {@code null}
     * @param protectionDomain      the protection domain of the class being defined or redefined
     * @param classfileBuffer       the input byte buffer in class file format - must not be modified
     *
     * @return a well-formed class file buffer (the result of the transform),
     *         or {@code null} if no transform is performed
     *
     */
    byte[] transform(ClassLoader loader,
                     String className,
                     Class<?> classBeingRedefined,
                     ProtectionDomain protectionDomain,
                     byte[] classfileBuffer)
            throws IllegalClassFormatException;

}


```

(it has a second method that adds Module information, but we're ignoring it for now.)

If your transformer doesn't wish to to modify a class, it can either return the (unmodified) classfileBuffer or null.
Keep in mind that the transformer may be called for more classes than what the agent has requested (by the
retransformClasses), depending on the class hierarchy. That means that if you're filtering the classes you want to
retransform by name, you have to double check in your transformer.

## ASM

See [ASM.md](ASM.md) for more information

# Using an agent

## Beware of classloaders

When you attach a java agent to a running process, it's (the process) classpath has his system classloader modified and
the agent jar is appended to it.
While it allows for modified classes to refer to classes in the java agent (see the NanosToMicrosMethodVisitor calling
NanosConverter, a class that was not present in the original classloader), it can create issues.
The most common of those is a version mismatch between classes that were previously loaded and classes with the same
fully classified class name present in the jar - and usually either the bytecode library that you're using (like ASM) or
the logger facade that you're using (commons logging, slf4j, etc).
The way this project is structured is to shade (rename) the classes from ASM to avoid (or at least minimize the chances
of) collisions, so all the ASM classes in this jar have their package name renamed from org.objectweb.asm to
org.objectweb.asmshadeda and you can do the same if you encounter any isses - they usually manifest as
ClassCastException, IncompatibleClassChangeError, LinkageError, or similar.

## javaagent parameter

## attaching to a running process

If you used this template, the easiest way is to run:
```java -jar java-agent-tutorial.jar // adjust according the actual jar file name  ```

It will list the java process that your user can attach to. The following command will attach the agent:

```java -jar java-agent-tutorial.jar  <pid or name> // adjust according the actual jar file name  ```

### attaching to itself

Usually the VM will prevent an agent to attach to itself, but you can 'fix' this by setting a System property:

```-Djdk.attach.allowAttachSelf=true```

I'm not entirely sure the use case, but if you ever need a program that can attach to itself, there it goes.

# common issues

Try to not modify classes in the sun.* and java.* packages as they can quickly cause issues. I had the following error
in a very innocent [agent](SampleAgent.md):

```
Exception in thread "Attach Listener" java.lang.reflect.InvocationTargetException
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:119)
	at java.base/java.lang.reflect.Method.invoke(Method.java:578)
	at java.instrument/sun.instrument.InstrumentationImpl.loadClassAndStartAgent(InstrumentationImpl.java:491)
	at java.instrument/sun.instrument.InstrumentationImpl.loadClassAndCallAgentmain(InstrumentationImpl.java:513)
Caused by: java.lang.ClassCircularityError: java/lang/invoke/MethodHandleImpl$AsVarargsCollector
	at java.base/java.lang.invoke.MethodHandleImpl.makeVarargsCollector(MethodHandleImpl.java:447)
	at java.base/java.lang.invoke.MethodHandle.asVarargsCollector(MethodHandle.java:1516)
	at java.base/java.lang.invoke.MethodHandle.withVarargs(MethodHandle.java:1201)
	at java.base/java.lang.invoke.MethodHandle.setVarargs(MethodHandle.java:1710)
	at java.base/java.lang.invoke.MethodHandles$Lookup.getDirectMethodCommon(MethodHandles.java:4027)
	at java.base/java.lang.invoke.MethodHandles$Lookup.getDirectMethodNoSecurityManager(MethodHandles.java:3975)
	at java.base/java.lang.invoke.MethodHandles$Lookup.getDirectMethodForConstant(MethodHandles.java:4219)
	at java.base/java.lang.invoke.MethodHandles$Lookup.linkMethodHandleConstant(MethodHandles.java:4167)
	at java.base/java.lang.invoke.MethodHandleNatives.linkMethodHandleConstant(MethodHandleNatives.java:612)
	at entrypoint.Agent.agentmain(Agent.java:49)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)

```

Also, be carefull of classloader issues and do notice how the agent classes themselves can be transformed (as they are
regular java classes).

Keep notice that classes that are currently on the stack will preserve their bytecode (so if you're not seeing changes
in your class, make sure that they are not on the stack at all times)

# More references

- https://www.baeldung.com/java-instrumentation
- https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html#jvms-6.5.ldiv
- https://docs.oracle.com/en/java/javase/18/docs/api/java.instrument/java/lang/instrument/Instrumentation.html