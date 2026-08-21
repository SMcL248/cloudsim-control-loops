package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

/**
 * Variant 7 - HOST level - Capacity-relative absolute reference.
 * Assumes the input metric is per-host available MIPS headroom in
 * absolute MIPS (not normalised). This analyser never looks at the
 * mean, median or spread of the snapshot at all; instead it compares
 * each host's raw headroom against the system's permitted VM MIPS
 * tiers (readSpace.getMipsTiers()) to ask a concrete question: could
 * this host actually accept a new VM? A host with less headroom than
 * the smallest VM tier is OVERLOADED (it cannot usefully accept new
 * work); a host with more than double the largest VM tier is
 * UNDERLOADED (significant spare capacity); everything else is
 * BALANCED.
 */
public class analyser_v7 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final double UNDERLOAD_MULTIPLIER = 2.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        int[] mipsTiers = readSpace.getMipsTiers();
        double minTier = 0.0;
        double maxTier = 0.0;
        if (mipsTiers != null && mipsTiers.length > 0) {
            minTier = mipsTiers[0];
            maxTier = mipsTiers[0];
            for (int tier : mipsTiers) {
                if (tier < minTier) minTier = tier;
                if (tier > maxTier) maxTier = tier;
            }
        }

        int overloaded = 0;
        int underloaded = 0;

        for (int i = 0; i < n; i++) {
            double headroom = metrics[i];
            if (headroom < minTier) {
                result[i] = LoadState.OVERLOADED;
                overloaded++;
            } else if (headroom > maxTier * UNDERLOAD_MULTIPLIER) {
                result[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        List<HostEntity> hosts = readSpace.getAllHosts();
        Log.printlnConcat(readSpace.getNow(), ": [analyser_v7] classified ", n,
            " of ", hosts.size(), " hosts via VM-tier headroom reference (minTier=",
            minTier, ", maxTier=", maxTier, ") -> overloaded=", overloaded,
            ", underloaded=", underloaded, ", balanced=", (n - overloaded - underloaded));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "host-mipsHeadroomAbsolute-instantaneous";
    }

    @Override
    public String outputSemantic() {
        return "host-loadState-vmTierReference";
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
