package org.cloudbus.cloudsim.examples;

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class analyser_v9 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-mipsDemand-absolute";
    private static final String OUTPUT_SEMANTIC = "host-loadState-capacityNormalizedZscore";

    private static final double Z_HIGH = 1.0;
    private static final double Z_LOW = -1.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v9] no hosts to classify.");
            return result;
        }

        // Convert each host's absolute MIPS demand into a utilisation
        // fraction of that specific host's own capacity before comparing
        // across hosts, since raw MIPS demand alone is not comparable
        // between differently sized hosts.
        double[] utilisation = new double[n];
        for (int i = 0; i < n; i++) {
            double capacity = (i < hosts.size()) ? readSpace.getHostTotalMips(hosts.get(i)) : 0.0;
            utilisation[i] = (capacity > 1e-9) ? (metrics[i] / capacity) : 0.0;
        }

        double sum = 0.0;
        for (double u : utilisation) {
            sum += u;
        }
        double mean = sum / n;

        double sqDiffSum = 0.0;
        for (double u : utilisation) {
            sqDiffSum += (u - mean) * (u - mean);
        }
        double stdDev = Math.sqrt(sqDiffSum / n);

        int overloadedCount = 0;
        int underloadedCount = 0;

        for (int i = 0; i < n; i++) {
            if (stdDev < 1e-9) {
                result[i] = LoadState.BALANCED;
                continue;
            }
            double z = (utilisation[i] - mean) / stdDev;
            if (z >= Z_HIGH) {
                result[i] = LoadState.OVERLOADED;
                overloadedCount++;
            } else if (z <= Z_LOW) {
                result[i] = LoadState.UNDERLOADED;
                underloadedCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v9] classified ", n,
                " hosts by capacity-normalized z-score (mean util=", mean, ", stdDev=",
                stdDev, "): ", overloadedCount, " overloaded, ", underloadedCount,
                " underloaded, ", (n - overloadedCount - underloadedCount), " balanced.");

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
