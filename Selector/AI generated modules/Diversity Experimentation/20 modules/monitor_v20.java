package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Cloudlet level - blended abandonment risk score combining progress, VM throughput and host health.
public class monitor_v20 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1400;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        List<GuestEntity> vms = readSpace.getVmList();
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[cloudlets.size()];

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);

            long total = readSpace.getTotalLength(cl);
            long remaining = readSpace.getRemainingLength(cl);
            double remainingFrac = (total > 0L) ? (remaining / (double) total) : 0.0;

            GuestEntity owner = findOwningVm(readSpace, vms, cl);

            double stalledFlag;
            double hostFailedFlag;

            if (owner == null) {
                stalledFlag = 1.0;
                hostFailedFlag = 1.0;
            } else {
                double throughput = readSpace.getVmEffectiveThroughput(owner);
                stalledFlag = (throughput <= 0.0) ? 1.0 : 0.0;

                HostEntity ownerHost = findOwningHost(readSpace, hosts, owner);
                if (ownerHost == null) {
                    hostFailedFlag = 1.0;
                } else {
                    boolean unhealthy = readSpace.isHostFailed(ownerHost) || readSpace.isHostPermanentlyDead(ownerHost);
                    hostFailedFlag = unhealthy ? 1.0 : 0.0;
                }
            }

            result[i] = (remainingFrac + stalledFlag + hostFailedFlag) / 3.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v20] computed cloudlet-abandonment-risk-composite for ", cloudlets.size(), " cloudlets.");

        return result;
    }

    private GuestEntity findOwningVm(ReadSpace readSpace, List<GuestEntity> vms, Cloudlet cl) {
        int clId = readSpace.getId(cl);
        for (GuestEntity vm : vms) {
            for (Cloudlet candidate : readSpace.getVmCloudletList(vm)) {
                if (readSpace.getId(candidate) == clId) {
                    return vm;
                }
            }
        }
        return null;
    }

    private HostEntity findOwningHost(ReadSpace readSpace, List<HostEntity> hosts, GuestEntity vm) {
        int vmId = readSpace.getId(vm);
        for (HostEntity host : hosts) {
            for (GuestEntity candidate : readSpace.getVmListForHost(host)) {
                if (readSpace.getId(candidate) == vmId) {
                    return host;
                }
            }
        }
        return null;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-abandonment-risk-composite: 0.0-1.0 blended risk score for this cloudlet estimating "
                + "chance of workload loss, averaging (a) its unfinished length fraction, (b) whether its owning "
                + "VM currently has zero effective throughput (stalled), and (c) whether its owning VM's host is "
                + "failed or permanently dead. If the owning VM or host cannot be resolved, factors (b) and/or (c) "
                + "default to their highest-risk value (1.0) as a conservative estimate. Index i corresponds to "
                + "getActiveCloudlets().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
