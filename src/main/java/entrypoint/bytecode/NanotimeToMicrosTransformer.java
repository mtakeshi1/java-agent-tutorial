package entrypoint.bytecode;

import entrypoint.Agent;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * This simple {@link ClassFileTransformer} truncates the result of the call {@link System#nanoTime()} to microseconds precision. It does this my looking at every invocation to that method dividing by 1000 and multiplying by 1000.
 * It has no practical application, it was just the first thing that came to my mind when thinking of what would be a simple transformation.
 */
public class NanotimeToMicrosTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (Agent.nonJDKClass(className)) {
            System.out.println("starting transformation of " + className);
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            reader.accept(new NanosToMicrosClassVisitor(writer), ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        } else return null;
    }

}
