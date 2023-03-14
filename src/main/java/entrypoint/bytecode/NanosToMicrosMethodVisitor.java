package entrypoint.bytecode;

import entrypoint.utils.NanosConverter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A {@link MethodVisitor} that, whenever it sees calls to {@link System#nanoTime()}, truncates it to microseconds by dividing it by 1000L and multipling by 1000L
 */
public class NanosToMicrosMethodVisitor extends MethodVisitor {
    public NanosToMicrosMethodVisitor(MethodVisitor delegate) {
        super(Opcodes.ASM9, delegate);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        if (opcode == Opcodes.INVOKESTATIC && name.equals("nanoTime") && owner.equals("java/lang/System")) {
            super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(NanosConverter.class), "normalize", Type.getMethodDescriptor(Type.LONG_TYPE, Type.LONG_TYPE), false);
//            super.visitLdcInsn(1000L);
//            super.visitInsn(Opcodes.LDIV);
//            super.visitLdcInsn(1000L);
//            super.visitInsn(Opcodes.LMUL);
        }

    }
}
