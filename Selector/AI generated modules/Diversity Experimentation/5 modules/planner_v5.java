package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Strategy: horizontal scale-out.
// Only fires when a majority of active hosts are OVERLOADED, i.e. genuine system-wide
// saturation rather than a local imbalance that migration/rescaling could fix. Sizes the new
// VM by severity (near-total saturation gets a bigger guest than borderline saturation) and
// resolves the datacenter id from any existing VM, since this is a single-datacenter sim.
public class planner_v5 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();

        int consideredHosts = 0;
        int overloadedHosts = 0;

        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host) || readSpace.isHostPoweredDown(host)) continue;
            consideredHosts++;
            if (diagnosis[i] == LoadState.OVERLOADED) overloadedHosts++;
        }

        if (consideredHosts == 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] ", "no active hosts to assess, no action");
            return new int[0];
        }

        double overloadFraction = (double) overloadedHosts / (double) consideredHosts;

        // Only scale out horizontally when a majority of active hosts are struggling; below
        // that threshold, migration/rescaling variants are better placed to fix localised load.
        if (overloadFraction < 0.5) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] ", "system-wide load not saturated, no scale-out planned");
            return new int[0];
        }

        List<GuestEntity> vms = readSpace.getVmList();
        if (vms.isEmpty()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] ", "no existing vm to resolve datacenter id from, no action");
            return new int[0];
        }

        int datacenterId = readSpace.getDatacenterFor(readSpace.getId(vms.get(0)));

        // Severity decides how generously the new VM is sized: near-total saturation warrants
        // a bigger guest than borderline saturation.
        int tierIndex = overloadFraction >= 0.85 ? 2 : 0;

        Log.printlnConcat(readSpace.getNow(), ": [planner_v5] planning creation of new vm at tier ", tierIndex, " in datacenter ", datacenterId, " due to overload fraction ", overloadFraction);

        return new int[] { tierIndex, 0, datacenterId };
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-saturation-signal";
    }

    @Override
    public String outputSemantic() {
        return "requestVmCreation";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3003;
    }
}
