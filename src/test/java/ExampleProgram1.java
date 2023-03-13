public class ExampleProgram1 {

    public static class ExampleInner {
        public void print() {
            System.out.println(System.nanoTime());
        }
    }

    public static void main(String[] args) throws Exception {
        while (true) {
            new ExampleInner().print();
            Thread.sleep(1000);
        }

    }

}
