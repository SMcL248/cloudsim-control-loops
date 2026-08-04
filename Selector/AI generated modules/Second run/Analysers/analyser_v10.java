package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;

// Cloudlet-level analyser. Interprets metrics[i] as a per-cloudlet PE
// (processing element) requirement count aligned with
// readSpace.getActiveCloudlets(). Uses simple fixed integer thresholds:
// cloudlets requiring several PEs are harder to place and more likely to
// be starved or delayed, so they are OVERLOADED; single-PE cloudlets are
// trivial to schedule and are UNDERLOADED. Serves the service-quality
// (completion rate) goal by flagging cloudlets most at risk of being
// deprioritised by a planner.
public class analyser_v10 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v10";
    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final double OVERLOAD_PE_COUNT = 3.0;
    private static final double UNDERLOAD_PE_COUNT = 1.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (v >= OVERLOAD_PE_COUNT) {
                result[i] = LoadState.OVERLOADED;
            } else if (v <= UNDERLOAD_PE_COUNT) {
                result[i] = LoadState.UNDERLOADED;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME
                + "] classified " + n + " cloudlets via pe-requirement fixed thresholds (overload="
                + OVERLOAD_PE_COUNT + ", underload=" + UNDERLOAD_PE_COUNT + ")");

        return result;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-pe-requirement-count";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-classification-pe-fixed-threshold";
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
