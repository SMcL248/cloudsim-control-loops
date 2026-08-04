package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;

// VM-level analyser. Interprets metrics[i] as a per-VM raw MIPS allocation
// aligned with readSpace.getVmList(). Rather than a fixed or purely
// statistical threshold, boundaries are pulled from the system's own MIPS
// tier ladder (readSpace.getMipsTiers()): a VM already sitting at or above
// the top tier has no room to scale up and is OVERLOADED (cannot absorb
// incoming work); a VM at or below the bottom tier has maximum headroom
// and is UNDERLOADED. This directly serves the "preserve VM capacity for
// incoming work" availability goal.
public class analyser_v8 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v8";
    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        int[] mipsTiers = readSpace.getMipsTiers();

        if (mipsTiers == null || mipsTiers.length == 0) {
            // No tier information available: nothing to compare against.
            for (int i = 0; i < n; i++) {
                result[i] = LoadState.BALANCED;
            }
            return result;
        }

        double topTier = mipsTiers[mipsTiers.length - 1];
        double bottomTier = mipsTiers[0];

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (v >= topTier) {
                result[i] = LoadState.OVERLOADED;
            } else if (v <= bottomTier) {
                result[i] = LoadState.UNDERLOADED;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME
                + "] classified " + n + " vms via mips-tier-relative thresholds (bottomTier="
                + bottomTier + ", topTier=" + topTier + ")");

        return result;
    }

    @Override
    public String inputSemantic() {
        return "vm-mips-allocation-raw";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-classification-mips-tier-relative";
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
