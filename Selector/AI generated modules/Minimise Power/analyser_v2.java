package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

/**
 * analyser_v2
 *
 * STRATEGY: Population mean +/- one standard deviation, recomputed from the observed
 * cohort on every call. Level: HOST (2). Metric: host CPU utilisation ratio (0..1).
 *
 * Rationale: rather than a human-chosen cut point, "overloaded" and "underloaded" are
 * defined relative to how this cohort of hosts is behaving right now. A workload spike
 * that lifts every host together will not itself trigger a flood of OVERLOADED verdicts,
 * because the band moves with the cohort - only hosts that stand out from their peers
 * are flagged, which keeps consolidation/power-down decisions targeted.
 */
public class analyser_v2 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-cpuUtilRatio-0to1";
    private static final String OUTPUT_SEMANTIC = "host-loadState-meanStdDev";

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];
        if (n == 0) {
            return states;
        }

        double sum = 0.0;
        for (double v : metrics) {
            sum += v;
        }
        double mean = sum / n;

        double sqDiffSum = 0.0;
        for (double v : metrics) {
            sqDiffSum += (v - mean) * (v - mean);
        }
        double stdDev = Math.sqrt(sqDiffSum / n);

        int overloaded = 0;
        int underloaded = 0;
        int balanced = 0;

        if (stdDev == 0.0) {
            // Uniform cohort - no host deviates from any other, nothing to single out.
            for (int i = 0; i < n; i++) {
                states[i] = LoadState.BALANCED;
            }
            balanced = n;
        } else {
            double upperBand = mean + stdDev;
            double lowerBand = mean - stdDev;
            for (int i = 0; i < n; i++) {
                double util = metrics[i];
                if (util > upperBand) {
                    states[i] = LoadState.OVERLOADED;
                    overloaded++;
                } else if (util < lowerBand) {
                    states[i] = LoadState.UNDERLOADED;
                    underloaded++;
                } else {
                    states[i] = LoadState.BALANCED;
                    balanced++;
                }
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v2] mean=", mean, " stdDev=", stdDev, " overloaded=", overloaded, " underloaded=", underloaded, " balanced=", balanced);
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
