package entrypoint.bytecode;

import entrypoint.Agent;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
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

    public static class NanosToMicrosClassVisitor extends ClassVisitor {
        public NanosToMicrosClassVisitor(ClassWriter writer) {
            super(Opcodes.ASM9, writer);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new NanosToMicrosMethodVisitor(delegate);
        }
    }

    public static class NanosToMicrosMethodVisitor extends MethodVisitor {
        public NanosToMicrosMethodVisitor(MethodVisitor delegate) {
            super(Opcodes.ASM9, delegate);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            if (opcode == Opcodes.INVOKESTATIC && name.equals("nanoTime") && owner.equals("java/lang/System")) {
                super.visitLdcInsn(1000L);
                super.visitInsn(Opcodes.LDIV);
                super.visitLdcInsn(1000L);
                super.visitInsn(Opcodes.LMUL);
            }

        }
    }
}
