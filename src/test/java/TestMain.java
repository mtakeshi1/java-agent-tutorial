import entrypoint.bytecode.NanosToMicrosClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.FileOutputStream;

public class TestMain {

    public static void main(String[] args) throws Exception {
        ClassReader reader = new ClassReader(TestMain.class.getClassLoader().getResourceAsStream("ExampleProgram1.class"));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        reader.accept(new NanosToMicrosClassVisitor(writer), ClassReader.EXPAND_FRAMES);
        byte[] bytes = writer.toByteArray();
        FileOutputStream fout = new FileOutputStream("/tmp/a.class");
        fout.write(bytes);
        fout.close();
    }
}
