package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

/**
 * Analyser variant 8 - Cloudlet-level remaining processing length, log-
 * transformed mean +/- stdDev band.
 *
 * Strategy: cloudlet remaining-length distributions are typically heavily
 * right-skewed (many short jobs, a few very long ones), which makes a
 * plain mean/stdDev band unreliable - the mean gets dragged up by the long
 * tail. This variant applies ln(1 + remainingLength) to compress that
 * skew before computing the mean/stdDev band, then classifies in the
 * transformed space.
 *
 * Goal alignment: throughput-leaning. Cloudlets with a disproportionately
 * large remaining backlog relative to their peers are flagged OVERLOADED
 * as they risk dragging out the makespan; cloudlets nearly finished are
 * flagged UNDERLOADED as low-risk/near-complete.
 *
 * Level: cloudlet (level 4). Input/output arrays are positionally aligned
 * with readSpace.getActiveCloudlets().
 */
public class analyser_v8 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double[] logValues = new double[n];
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            logValues[i] = Math.log(1.0 + Math.max(0.0, metrics[i]));
            sum += logValues[i];
        }
        double mean = (n > 0) ? sum / n : 0.0;

        double sqDiffSum = 0.0;
        for (double v : logValues) {
            sqDiffSum += (v - mean) * (v - mean);
        }
        double stdDev = (n > 0) ? Math.sqrt(sqDiffSum / n) : 0.0;

        double upperBound = mean + stdDev;
        double lowerBound = mean - stdDev;

        for (int i = 0; i < n; i++) {
            double v = logValues[i];
            if (v > upperBound) {
                states[i] = LoadState.OVERLOADED;
            } else if (v < lowerBound) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v8] classified ", n,
                " cloudlets by log-remaining-length, logMean=", mean, " logStdDev=", stdDev);

        return states;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-remainingLength: readSpace.getRemainingLength for the cloudlet, in millions of instructions (MI), one entry per cloudlet in readSpace.getActiveCloudlets() order";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-loadState: OVERLOADED if ln(1+remainingLength) is above the fleet's log-space mean plus one stdDev this cycle, UNDERLOADED if below the mean minus one stdDev, else BALANCED";
    }

    @Override
    public int inputGuid() {
        return 1400;
    }

    @Override
    public int outputGuid() {
        return 2400;
    }
}
