package org.cloudbus.cloudsim.examples;

public class StructuralMismatchException extends RuntimeException {
    public final String bridge;       // "M-A", "A-P", "P-E"
    public final String outputName;
    public final String inputName;
    public final Class<?> expectedClass;
    public final Class<?> actualClass;

    public StructuralMismatchException(String bridge, String outputName, String inputName,
                                        Class<?> expectedClass, Class<?> actualClass) {
        super("[" + bridge + "] output controller = " + outputName + " | input controller = "  + inputName + " | expected = " + expectedClass.getSimpleName()
                + " | actual = " + actualClass.getSimpleName());
        this.bridge = bridge; this.outputName = outputName; this.inputName = inputName;
        this.expectedClass = expectedClass; this.actualClass = actualClass;
    }
}