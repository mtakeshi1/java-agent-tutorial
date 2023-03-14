package entrypoint.bytecode;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A class visitor that replaces the MethodVisitor to {@link NanosToMicrosMethodVisitor}
 */
public class NanosToMicrosClassVisitor extends ClassVisitor {
    public NanosToMicrosClassVisitor(ClassWriter writer) {
        super(Opcodes.ASM9, writer);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new NanosToMicrosMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions));
    }
}
