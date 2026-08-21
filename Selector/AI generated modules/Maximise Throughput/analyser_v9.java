package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;

import java.util.List;

/**
 * Variant 9 - CLOUDLET level - Histogram modal-bin classification.
 * Assumes the input metric is the per-cloudlet absolute remaining
 * length (MI). Buckets the snapshot into a small histogram, finds the
 * bin with the most entries (the "typical" cloudlet), and classifies
 * each cloudlet by how many bins away it sits from that modal bin:
 * far above is OVERLOADED (much more work outstanding than typical),
 * far below is UNDERLOADED (much less work outstanding than typical).
 */
public class analyser_v9 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final int MIN_BINS = 3;
    private static final int MAX_BINS = 10;
    private static final int BIN_DISTANCE_THRESHOLD = 1;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        double min = metrics[0];
        double max = metrics[0];
        for (double v : metrics) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        double range = max - min;

        if (range <= 0.0) {
            for (int i = 0; i < n; i++) {
                result[i] = LoadState.BALANCED;
            }
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v9] all ", n,
                " cloudlets identical -> all balanced");
            return result;
        }

        int binCount = (int) Math.ceil(Math.sqrt(n));
        binCount = Math.max(MIN_BINS, Math.min(MAX_BINS, binCount));

        int[] binOf = new int[n];
        int[] counts = new int[binCount];
        double binWidth = range / binCount;

        for (int i = 0; i < n; i++) {
            int bin = (int) ((metrics[i] - min) / binWidth);
            if (bin >= binCount) {
                bin = binCount - 1; // catch the max value
            }
            binOf[i] = bin;
            counts[bin]++;
        }

        int modalBin = 0;
        int modalCount = counts[0];
        for (int b = 1; b < binCount; b++) {
            if (counts[b] > modalCount) {
                modalCount = counts[b];
                modalBin = b;
            }
        }

        int overloaded = 0;
        int underloaded = 0;

        for (int i = 0; i < n; i++) {
            int distance = binOf[i] - modalBin;
            if (distance > BIN_DISTANCE_THRESHOLD) {
                result[i] = LoadState.OVERLOADED;
                overloaded++;
            } else if (distance < -BIN_DISTANCE_THRESHOLD) {
                result[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        Log.printlnConcat(readSpace.getNow(), ": [analyser_v9] classified ", n,
            " of ", cloudlets.size(), " cloudlets via ", binCount,
            "-bin histogram (modalBin=", modalBin, ") -> overloaded=", overloaded,
            ", underloaded=", underloaded, ", balanced=", (n - overloaded - underloaded));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-remainingLengthAbsolute-instantaneous";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-loadState-histogramModalBin";
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
