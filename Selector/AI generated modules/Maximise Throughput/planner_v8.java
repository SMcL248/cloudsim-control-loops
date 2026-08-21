package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

import java.util.List;

// Strategy: Capacity Reserve Activation.
// Diagnosis is host-level. This variant does not move or resize anything;
// it adds supply. It measures the fraction of active (non-dead, powered-on)
// hosts flagged OVERLOADED, and once that fraction indicates majority
// cluster-wide pressure, brings the largest-capacity powered-down host back
// online as a fresh landing zone for future migrations and VM creation.
// Acting on the supply side rather than redistributing existing load is a
// distinct mechanism from every migration- or scaling-based variant: it
// grows the pool of usable MIPS before hosts start dropping cloudlets.
public class planner_v8 implements Planner<LoadState[], int[]> {

    private static final double PRESSURE_THRESHOLD = 0.5;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<HostEntity> hosts = readSpace.getAllHosts();

        int overloadedCount = 0;
        int activeCount = 0;

        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            HostEntity h = hosts.get(i);
            if (readSpace.isHostPoweredDown(h) || readSpace.isHostPermanentlyDead(h)) {
                continue;
            }
            activeCount++;
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadedCount++;
            }
        }

        if (activeCount == 0 || (double) overloadedCount / activeCount <= PRESSURE_THRESHOLD) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v8] Cluster pressure below threshold, no reserve activation issued.");
            return new int[]{-1};
        }

        HostEntity reserve = null;
        double biggestCapacity = -1;

        for (HostEntity h : hosts) {
            if (!readSpace.isHostPoweredDown(h)) {
                continue;
            }
            if (readSpace.isHostPermanentlyDead(h)) {
                continue;
            }
            if (readSpace.isHostPoweringUp(h)) {
                continue;
            }
            double cap = readSpace.getHostTotalMips(h);
            if (cap > biggestCapacity) {
                biggestCapacity = cap;
                reserve = h;
            }
        }

        if (reserve == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v8] Cluster under pressure but no powered-down reserve host available.");
            return new int[]{-1};
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v8] Reserve activation: powering up host ", readSpace.getId(reserve), " with capacity ", biggestCapacity, ".");

        return new int[]{readSpace.getId(reserve)};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "requestHostPowerUp";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3011;
    }
}
