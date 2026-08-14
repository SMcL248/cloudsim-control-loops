package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

/**
 * Analyser variant 1 - Host-level CPU utilisation, mean +/- one standard
 * deviation band.
 *
 * Strategy: classifies each host's CPU utilisation fraction against the
 * mean and standard deviation of the whole fleet observed this cycle. The
 * band is fully data-driven (no fixed constants) so it adapts each control
 * cycle as the fleet's utilisation profile shifts.
 *
 * Goal alignment: throughput-leaning. Hosts pushed above the population
 * band are flagged before they become a throughput bottleneck; hosts below
 * the band are flagged as consolidation/power-down candidates.
 *
 * Level: host (level 2). Input/output arrays are positionally aligned with
 * readSpace.getAllHosts().
 */
public class analyser_v1 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double sum = 0.0;
        for (double v : metrics) {
            sum += v;
        }
        double mean = (n > 0) ? sum / n : 0.0;

        double sqDiffSum = 0.0;
        for (double v : metrics) {
            sqDiffSum += (v - mean) * (v - mean);
        }
        double stdDev = (n > 0) ? Math.sqrt(sqDiffSum / n) : 0.0;

        double upperBound = mean + stdDev;
        double lowerBound = mean - stdDev;

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (v > upperBound) {
                states[i] = LoadState.OVERLOADED;
            } else if (v < lowerBound) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v1] classified ", n,
                " hosts by cpu util fraction, mean=", mean,
                " stdDev=", stdDev, " band=[", lowerBound, ",", upperBound, "]");

        return states;
    }

    @Override
    public String inputSemantic() {
        return "host-cpuUtilFraction: fraction of host total MIPS capacity currently allocated to guest VMs, range 0..1, one entry per host in readSpace.getAllHosts() order";
    }

    @Override
    public String outputSemantic() {
        return "host-loadState: OVERLOADED if cpuUtilFraction is above the fleet mean plus one standard deviation this cycle, UNDERLOADED if below the mean minus one standard deviation, else BALANCED";
    }

    @Override
    public int inputGuid() {
        return 1200;
    }

    @Override
    public int outputGuid() {
        return 2200;
    }
}
