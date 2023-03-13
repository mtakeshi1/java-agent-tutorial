package entrypoint;

import entrypoint.bytecode.NanotimeToMicrosTransformer;

import java.lang.instrument.Instrumentation;

public class Agent {

    /**
     * Called when the agent jar is used as a JVM parameter, like: -javaagent [path_to_ja]
     *
     * @param agentArgs arguments passed to the agent
     * @param inst      the Instrumentation object provided by the JVM
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("starting premain with args: " + agentArgs);
        NanotimeToMicrosTransformer tx = new NanotimeToMicrosTransformer();
        try {
            inst.addTransformer(tx, true);
            for (Class<?> any : inst.getAllLoadedClasses()) {
                if (inst.isModifiableClass(any) && nonJDKClass(any.getName())) {
                    System.out.println("triggering retransformation of " + any);
                    inst.retransformClasses(any);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            inst.removeTransformer(tx);
        }
    }

    /**
     * Called when the agent is dynamically attached to a running JVM
     *
     * @param agentArgs arguments passed to the agent using the attach API
     * @param inst      the Instrumentation object provided by the JVM
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("starting agentmain with args: " + agentArgs);
        NanotimeToMicrosTransformer tx = new NanotimeToMicrosTransformer();
        try {
            inst.addTransformer(tx, true);
            for (Class<?> any : inst.getAllLoadedClasses()) {
                if (inst.isModifiableClass(any) && nonJDKClass(any.getName())) {
                    System.out.println("triggering retransformation of " + any);
                    inst.retransformClasses(any);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            inst.removeTransformer(tx);
        }
    }

    public static boolean nonJDKClass(String className) {
        return !className.startsWith("java.") && !className.startsWith("sun.") && !className.startsWith("jdk.");
    }
}
