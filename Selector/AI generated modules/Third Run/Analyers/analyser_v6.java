package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

/**
 * Analyser variant 6 - VM-level effective throughput, min-max normalised
 * fixed-threshold classification.
 *
 * Strategy: rescales each VM's effective throughput into a 0..1 range
 * using this cycle's observed minimum and maximum, then applies fixed
 * relative thresholds (0.2 / 0.8) on the normalised value. Because the
 * metric is directly the throughput goal (rather than a proxy like CPU
 * utilisation), and normalisation removes the effect of absolute scale
 * differences between VM tiers, the same 0.2/0.8 cutoffs stay meaningful
 * regardless of which mix of small/medium/large VMs is present.
 *
 * Goal alignment: throughput-leaning (the metric IS the throughput goal).
 * VMs whose relative throughput is low are flagged as starved (UNDERLOADED
 * despite the name meaning "achieving little work" here, i.e. a corrective
 * signal); VMs at the top of the range are flagged OVERLOADED as they may
 * be absorbing more work than they can sustain.
 *
 * Level: VM (level 3). Input/output arrays are positionally aligned with
 * readSpace.getVmList().
 */
public class analyser_v6 implements Analyser<double[], LoadState[]> {

    private static final double LOW_CUTOFF = 0.2;
    private static final double HIGH_CUTOFF = 0.8;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double v : metrics) {
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
        }
        double range = max - min;

        for (int i = 0; i < n; i++) {
            double normalised = (range > 0.0) ? (metrics[i] - min) / range : 0.5;
            if (normalised >= HIGH_CUTOFF) {
                states[i] = LoadState.OVERLOADED;
            } else if (normalised <= LOW_CUTOFF) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v6] classified ", n,
                " vms by normalised effective throughput, min=", min, " max=", max);

        return states;
    }

    @Override
    public String inputSemantic() {
        return "vm-effectiveThroughput: readSpace.getVmEffectiveThroughput value for the VM (work completed per unit time), absolute units, one entry per VM in readSpace.getVmList() order";
    }

    @Override
    public String outputSemantic() {
        return "vm-loadState: OVERLOADED if effectiveThroughput min-max normalised against this cycle's fleet range is >= 0.8, UNDERLOADED if <= 0.2, else BALANCED";
    }

    @Override
    public int inputGuid() {
        return 1300;
    }

    @Override
    public int outputGuid() {
        return 2300;
    }
}
