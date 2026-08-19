package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import java.util.HashMap;
import java.util.Map;

// Strategy: stateful hysteresis / dead-band classifier.
// This variant remembers each index's previous classification across
// invocations. Entering OVERLOADED or UNDERLOADED requires crossing an
// outer band; leaving that state requires retreating past a separate,
// looser inner band. This asymmetric dead-band damps rapid flapping
// between states when a host's power draw oscillates near a boundary -
// a purely instantaneous classifier (as in v1) would flip-flop here.
public class analyser_v4 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v4";

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-power-draw-ratio";
    private static final String OUTPUT_SEMANTIC = "host-power-hysteresis-classification";

    private static final double OVERLOAD_ENTER = 0.75;
    private static final double OVERLOAD_EXIT = 0.55;
    private static final double UNDERLOAD_ENTER = 0.15;
    private static final double UNDERLOAD_EXIT = 0.30;

    private final Map<Integer, LoadState> previousState = new HashMap<Integer, LoadState>();

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            LoadState prior = previousState.get(i);
            LoadState next;

            if (Double.isNaN(v)) {
                next = (prior != null) ? prior : LoadState.BALANCED;
            } else if (prior == LoadState.OVERLOADED) {
                next = (v >= OVERLOAD_EXIT) ? LoadState.OVERLOADED : LoadState.BALANCED;
            } else if (prior == LoadState.UNDERLOADED) {
                next = (v <= UNDERLOAD_EXIT) ? LoadState.UNDERLOADED : LoadState.BALANCED;
            } else if (v >= OVERLOAD_ENTER) {
                next = LoadState.OVERLOADED;
            } else if (v <= UNDERLOAD_ENTER) {
                next = LoadState.UNDERLOADED;
            } else {
                next = LoadState.BALANCED;
            }

            result[i] = next;
            previousState.put(i, next);
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] classified ", n,
                " hosts with hysteresis bands; overload enter/exit=", OVERLOAD_ENTER, "/", OVERLOAD_EXIT,
                " underload enter/exit=", UNDERLOAD_ENTER, "/", UNDERLOAD_EXIT);

        return result;
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
