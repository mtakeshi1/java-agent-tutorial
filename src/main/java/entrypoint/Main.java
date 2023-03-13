package entrypoint;

import com.sun.tools.attach.*;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Objects;
import java.util.function.Predicate;

public class Main {

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            for (var vm : VirtualMachine.list()) {
                String myPid = String.valueOf(ManagementFactory.getRuntimeMXBean().getPid());
                if (!vm.id().equals(myPid)) {
                    System.out.printf("%6s - %s %n", vm.id(), vm.displayName());
                }
            }
        } else {
            String jar = Objects.requireNonNull(Main.class.getClassLoader().getResource(Main.class.getName().replace('.', '/') + ".class")).toString();
            String myPid = String.valueOf(ManagementFactory.getRuntimeMXBean().getPid());
            //jar:file:/home/takeshi/projects/github/java-agent-tutorial/target/java-agent-tutorial-1.0-SNAPSHOT.jar
            if (jar.startsWith("jar:file:")) {
                if (jar.contains("!")) {
                    jar = jar.substring(0, jar.lastIndexOf('!'));
                }
                jar = jar.substring("jar:file:".length());
                File f = new File(jar);
                if (!f.exists()) {
                    throw new RuntimeException("something wrong - jar file: " + f.getAbsolutePath() + " does not exist =(");
                }
                attachJavaAgent(args, jar, myPid);
            } else {
                System.err.println("Must be ran from a jar file, but was: " + jar);
                System.exit(-1);
            }
        }
    }

    private static void attachJavaAgent(String[] args, String jar, String myPid) throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException {
        Predicate<VirtualMachineDescriptor> pidMatcher = createMatcher(args[0]);
        for (var vm : VirtualMachine.list()) {
            if (!vm.id().equals(myPid) && pidMatcher.test(vm)) {
                VirtualMachine machine = VirtualMachine.attach(vm.id());
                try {
                    if (args.length >= 2) {
                        machine.loadAgent(jar, args[1]);
                    } else {
                        machine.loadAgent(jar);
                    }
                    break;
                } finally {
                    machine.detach();
                }
            }
        }
    }

    private static Predicate<VirtualMachineDescriptor> createMatcher(String arg) {
        return vm -> vm.id().equals(arg) || vm.displayName().contains(arg) || vm.id().matches(arg) || vm.displayName().matches(arg);
    }

}
