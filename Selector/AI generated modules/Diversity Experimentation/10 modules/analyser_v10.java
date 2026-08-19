package org.cloudbus.cloudsim.examples;

import java.util.Arrays;
import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;

public class analyser_v10 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final String INPUT_SEMANTIC = "cloudlet-peDemand-requested";
    private static final String OUTPUT_SEMANTIC = "cloudlet-loadState-urgencyComposite";

    @Override
    public LoadState[] analyse(final double[] metrics, ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v10] no cloudlets to classify.");
            return result;
        }

        double maxPeDemand = 0.0;
        for (double v : metrics) {
            if (v > maxPeDemand) {
                maxPeDemand = v;
            }
        }

        // Urgency blends how resource-hungry a cloudlet is (its PE demand,
        // normalized against the busiest cloudlet this round) with how much
        // work it still has left to do (its remaining-length fraction). A
        // cloudlet that is both heavy and far from done is the most urgent.
        final double[] urgency = new double[n];
        for (int i = 0; i < n; i++) {
            double normalizedPeDemand = (maxPeDemand > 1e-9) ? (metrics[i] / maxPeDemand) : 0.0;

            double progressFraction = 0.5;
            if (i < cloudlets.size()) {
                Cloudlet cl = cloudlets.get(i);
                long total = readSpace.getTotalLength(cl);
                long remaining = readSpace.getRemainingLength(cl);
                if (total > 0) {
                    progressFraction = (double) remaining / (double) total;
                }
            }

            urgency[i] = normalizedPeDemand * progressFraction;
        }

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(urgency[a], urgency[b]));

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

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v10] ranked ", n,
                " cloudlets by composite PE-demand/progress urgency: ", overloadedCount,
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
