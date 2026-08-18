package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;

/**
 * Variant 1 - Host level, fixed global thresholds.
 * Simplest possible boundary policy: a host is overloaded above a fixed
 * utilisation ratio and underloaded below a fixed ratio, regardless of how
 * the rest of the population is behaving this tick.
 */
public class analyser_v1 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-cpu-utilization-ratio-fraction-of-host-total-mips-in-use";
    private static final String OUTPUT_SEMANTIC = "host-load-classification-balanced-under-over";

    private static final double OVERLOAD_THRESHOLD = 0.80;
    private static final double UNDERLOAD_THRESHOLD = 0.20;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        int overloaded = 0;
        int underloaded = 0;
        int balanced = 0;

        for (int i = 0; i < n; i++) {
            double util = metrics[i];
            LoadState state;

            if (util > OVERLOAD_THRESHOLD) {
                state = LoadState.OVERLOADED;
            } else if (util < UNDERLOAD_THRESHOLD) {
                state = LoadState.UNDERLOADED;
            } else {
                state = LoadState.BALANCED;
            }

            states[i] = state;
            if (state == LoadState.OVERLOADED) overloaded++;
            else if (state == LoadState.UNDERLOADED) underloaded++;
            else balanced++;
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v1] fixed-threshold host classification complete -> ",
                n, " hosts, overloaded=", overloaded, ", underloaded=", underloaded, ", balanced=", balanced);

        return states;
    }

    @Override
    public String inputSemantic() {
        return INPUT_SEMANTIC;
    }

    @Override
    public String outputSemantic() {
        return OUTPUT_SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
