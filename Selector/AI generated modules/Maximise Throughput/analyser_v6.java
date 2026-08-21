package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;

import java.util.Arrays;
import java.util.List;

/**
 * Variant 6 - CLOUDLET level - Gap-based natural-break clustering.
 * Assumes the input metric is the per-cloudlet requested PE count
 * (cast to double). Instead of a formula-based threshold, this
 * analyser sorts the snapshot and looks for the two largest gaps
 * between consecutive sorted values, using them as natural split
 * points into up to three contiguous clusters: the lowest cluster is
 * UNDERLOADED, the highest is OVERLOADED, and anything between the
 * two chosen gaps is BALANCED. If only one meaningful gap exists the
 * snapshot splits cleanly into two clusters; if the values are too
 * uniform to produce any gap, everything is BALANCED.
 */
public class analyser_v6 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }
        if (n == 1) {
            result[0] = LoadState.BALANCED;
            return result;
        }

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(metrics[a], metrics[b]));

        if (n == 2) {
            if (Double.compare(metrics[order[0]], metrics[order[1]]) == 0) {
                result[order[0]] = LoadState.BALANCED;
                result[order[1]] = LoadState.BALANCED;
            } else {
                result[order[0]] = LoadState.UNDERLOADED;
                result[order[1]] = LoadState.OVERLOADED;
            }
            return result;
        }

        // Compute gaps between consecutive sorted values.
        double[] gaps = new double[n - 1];
        for (int i = 0; i < n - 1; i++) {
            gaps[i] = metrics[order[i + 1]] - metrics[order[i]];
        }

        double biggest = -1.0;
        double secondBiggest = -1.0;
        int biggestIdx = -1;
        int secondIdx = -1;

        for (int i = 0; i < gaps.length; i++) {
            if (gaps[i] > biggest) {
                secondBiggest = biggest;
                secondIdx = biggestIdx;
                biggest = gaps[i];
                biggestIdx = i;
            } else if (gaps[i] > secondBiggest) {
                secondBiggest = gaps[i];
                secondIdx = i;
            }
        }

        if (biggest <= 0.0) {
            // all values identical
            Arrays.fill(result, LoadState.BALANCED);
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v6] no separation found among ",
                n, " cloudlets -> all balanced");
            return result;
        }

        int lowSplit;
        int highSplit;
        if (secondBiggest > 0.0) {
            lowSplit = Math.min(biggestIdx, secondIdx);
            highSplit = Math.max(biggestIdx, secondIdx);
        } else {
            // Only one meaningful gap - split cleanly into two clusters, no middle band.
            lowSplit = biggestIdx;
            highSplit = biggestIdx;
        }

        int overloaded = 0;
        int underloaded = 0;

        for (int rank = 0; rank < n; rank++) {
            int idx = order[rank];
            LoadState state;
            if (rank <= lowSplit) {
                state = LoadState.UNDERLOADED;
            } else if (rank > highSplit) {
                state = LoadState.OVERLOADED;
            } else {
                state = LoadState.BALANCED;
            }
            result[idx] = state;
            if (state == LoadState.OVERLOADED) overloaded++;
            if (state == LoadState.UNDERLOADED) underloaded++;
        }

        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        Log.printlnConcat(readSpace.getNow(), ": [analyser_v6] clustered ", n,
            " of ", cloudlets.size(), " cloudlets via largest-gap breaks -> overloaded=",
            overloaded, ", underloaded=", underloaded, ", balanced=",
            (n - overloaded - underloaded));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-peRequestCount-instantaneous";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-loadState-gapNaturalBreaks";
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
