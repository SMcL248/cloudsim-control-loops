package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Planner v5: CPU-util based — Global Variance Minimisation.
 *
 * Simulates every possible (overloaded-source VM, underloaded-destination host)
 * migration and selects the one that minimises the variance in MIPS load
 * across all hosts after migration.
 *
 * Rationale: unlike greedy local strategies, this approach explicitly optimises
 * the global load distribution. A migration is only chosen if it strictly
 * reduces current variance; if none does, the sentinel {-1,-1} is returned.
 */
public class planner_v5 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();

        // Snapshot current per-host MIPS loads
        double[] loads = new double[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            loads[i] = totalMips(hosts.get(i));
        }

        double baseVariance = variance(loads);

        GuestEntity bestVm = null;
        HostEntity bestSource = null;
        HostEntity bestDest = null;
        double bestVariance = baseVariance;

        for (int si = 0; si < diagnosis.length && si < hosts.size(); si++) {
            if (diagnosis[si] != LoadState.OVERLOADED) continue;
            HostEntity source = hosts.get(si);

            for (GuestEntity vm : source.getGuestList()) {
                double vmMips = vm.getCurrentRequestedTotalMips();

                for (int di = 0; di < diagnosis.length && di < hosts.size(); di++) {
                    if (diagnosis[di] != LoadState.UNDERLOADED) continue;
                    if (di == si) continue;
                    HostEntity dest = hosts.get(di);
                    if (!dest.isSuitableForGuest(vm)) continue;

                    // Simulate migration outcome
                    double[] simLoads = loads.clone();
                    simLoads[si] -= vmMips;
                    simLoads[di] += vmMips;
                    double simVariance = variance(simLoads);

                    if (simVariance < bestVariance) {
                        bestVariance = simVariance;
                        bestVm = vm;
                        bestSource = source;
                        bestDest = dest;
                    }
                }
            }
        }

        if (bestVm == null) {
            Log.printlnConcat(now, ": planner_v5: No variance-reducing migration found (base variance ",
                    baseVariance, ").");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(now, ": planner_v5: Variance-optimal migrate VM ", bestVm.getId(),
                " from Host ", bestSource.getId(),
                " to Host ", bestDest.getId(),
                " (variance ", baseVariance, " -> ", bestVariance, ")");
        return new int[]{bestVm.getId(), bestDest.getId()};
    }

    private double totalMips(HostEntity host) {
        double total = 0.0;
        for (GuestEntity vm : host.getGuestList()) {
            total += vm.getCurrentRequestedTotalMips();
        }
        return total;
    }

    private double variance(double[] values) {
        if (values.length == 0) return 0.0;
        double mean = 0.0;
        for (double v : values) mean += v;
        mean /= values.length;
        double sumSq = 0.0;
        for (double v : values) sumSq += (v - mean) * (v - mean);
        return sumSq / values.length;
    }

    @Override
    public String inputGuid() { return "host-cpu-util-loadstate"; }

    @Override
    public String outputGuid() { return "host-migration-pair"; }
}
