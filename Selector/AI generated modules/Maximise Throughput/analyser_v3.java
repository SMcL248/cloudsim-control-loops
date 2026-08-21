package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;

import java.util.List;

/**
 * Variant 3 - CLOUDLET level - Fixed absolute thresholds.
 * Assumes the input metric is per-cloudlet remaining-length fraction,
 * i.e. remainingLength / totalLength, in [0,1]. Unlike the
 * distribution-relative variants, this analyser uses fixed constants
 * that do not adapt to the observed snapshot: a cloudlet that still
 * has most of its work outstanding is OVERLOADED (falling behind),
 * one that is nearly done is UNDERLOADED (little demand left).
 */
public class analyser_v3 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;

    private static final double OVERLOAD_FRACTION = 0.85;
    private static final double UNDERLOAD_FRACTION = 0.15;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        int overloaded = 0;
        int underloaded = 0;

        for (int i = 0; i < n; i++) {
            double fraction = metrics[i];
            if (fraction > OVERLOAD_FRACTION) {
                result[i] = LoadState.OVERLOADED;
                overloaded++;
            } else if (fraction < UNDERLOAD_FRACTION) {
                result[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        Log.printlnConcat(readSpace.getNow(), ": [analyser_v3] classified ", n,
            " of ", cloudlets.size(), " cloudlets via fixed thresholds (over=",
            OVERLOAD_FRACTION, ", under=", UNDERLOAD_FRACTION, ") -> overloaded=",
            overloaded, ", underloaded=", underloaded, ", balanced=",
            (n - overloaded - underloaded));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-remainingLengthFraction-instantaneous";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-loadState-fixedThreshold";
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
