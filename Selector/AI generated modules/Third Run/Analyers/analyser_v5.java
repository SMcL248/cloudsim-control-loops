package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

/**
 * Analyser variant 5 - VM-level CPU utilisation, z-score classification.
 *
 * Strategy: converts each VM's CPU utilisation fraction into a z-score
 * against the population mean/stdDev observed this cycle, then applies
 * fixed z cutoffs of +1.0 / -1.0. Functionally similar in spirit to
 * variant 1 but operating one level down (per-VM rather than per-host) and
 * expressed as an explicit z-score rather than a raw value band.
 *
 * Goal alignment: throughput-leaning. VMs running hot relative to their
 * peers are flagged as scale-up/migration candidates before cloudlet
 * completion slows; VMs running cold are flagged as scale-down or
 * consolidation candidates.
 *
 * Level: VM (level 3). Input/output arrays are positionally aligned with
 * readSpace.getVmList().
 */
public class analyser_v5 implements Analyser<double[], LoadState[]> {

    private static final double Z_HIGH = 1.0;
    private static final double Z_LOW = -1.0;

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

        for (int i = 0; i < n; i++) {
            double z = (stdDev > 0.0) ? (metrics[i] - mean) / stdDev : 0.0;
            if (z > Z_HIGH) {
                states[i] = LoadState.OVERLOADED;
            } else if (z < Z_LOW) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v5] classified ", n,
                " vms by cpu util fraction z-score, mean=", mean, " stdDev=", stdDev);

        return states;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpuUtilFraction: fraction of a VM's allocated MIPS currently in use (readSpace.getVmCpuUtil), range 0..1, one entry per VM in readSpace.getVmList() order";
    }

    @Override
    public String outputSemantic() {
        return "vm-loadState: OVERLOADED if the VM's cpuUtilFraction z-score against the population this cycle exceeds +1.0, UNDERLOADED if below -1.0, else BALANCED";
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
