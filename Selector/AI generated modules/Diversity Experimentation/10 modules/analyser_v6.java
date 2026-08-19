package org.cloudbus.cloudsim.examples;

import java.util.Arrays;
import org.cloudbus.cloudsim.Log;

public class analyser_v6 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final String INPUT_SEMANTIC = "cloudlet-remainingLengthFraction-progressRatio";
    private static final String OUTPUT_SEMANTIC = "cloudlet-loadState-rankTertile";

    @Override
    public LoadState[] analyse(final double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v6] no cloudlets to classify.");
            return result;
        }

        // Rank each cloudlet by its remaining-length fraction relative to its
        // peers this round, rather than against any fixed or distribution
        // derived cutoff. The top third of the population becomes
        // OVERLOADED, the bottom third UNDERLOADED, and the middle third
        // BALANCED.
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(metrics[a], metrics[b]));

        int lowerCut = n / 3;
        int upperCut = n - n / 3;

        int overloadedCount = 0;
        int underloadedCount = 0;

        for (int rank = 0; rank < n; rank++) {
            int originalIndex = order[rank];
            if (rank >= upperCut) {
                result[originalIndex] = LoadState.OVERLOADED;
                overloadedCount++;
            } else if (rank < lowerCut) {
                result[originalIndex] = LoadState.UNDERLOADED;
                underloadedCount++;
            } else {
                result[originalIndex] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v6] ranked ", n,
                " cloudlets into tertiles by remaining-length fraction: ", overloadedCount,
                " overloaded, ", underloadedCount, " underloaded, ",
                (n - overloadedCount - underloadedCount), " balanced.");

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
