package org.cloudbus.cloudsim.examples;

import java.util.List;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.SimEvent;

public class MeasuringBroker extends DatacenterBroker {

    private static final int OBSERVATION_RATE = 100;
    private double varianceSum = 0.0;
    private int cycleCount = 0;
    private boolean placementDumped = false;
    private int occupiedHosts = -1;
    private boolean verbose = false;

    public MeasuringBroker(String name) throws Exception { super(name); }

    @Override
    public void startEntity() {
        super.startEntity();
        schedule(getId(), OBSERVATION_RATE, CloudActionTags.VM_BROKER_EVENT);
    }

    @Override
    protected void processOtherEvent(SimEvent ev) {
        if (ev.getTag() == CloudActionTags.VM_BROKER_EVENT) {
            measure();
        } else {
            super.processOtherEvent(ev);
        }
    }

    @Override
    public void shutdownEntity() {
        CloudSim.cancelAll(getId(), CloudSim.SIM_ANY);
        super.shutdownEntity();
    }

    private void measure() {

        if (getCloudletList().isEmpty() &&
            getCloudletSubmittedList().size() == getCloudletReceivedList().size()) return;

        if (CloudSim.clock() > 100_000){
            throw new IllegalStateException("MeasuringBroker still observing at t=" + CloudSim.clock()
            + " — workload likely stalled; investigate this seed");
        }

        double now = CloudSim.clock();
        List<HostEntity> hosts = getDatacenterCharacteristicsList()
            .values().iterator().next().getHostList();

        if (!placementDumped) {
            int occupied = 0;
            for (HostEntity h : hosts) {
                if (!h.getGuestList().isEmpty()) occupied++;
                if (verbose) {
                    StringBuilder sb = new StringBuilder("Host #" + h.getId() + ":");
                    for (GuestEntity vm : h.getGuestList())
                        sb.append(" VM#").append(vm.getId())
                        .append("(").append((int) vm.getMips())
                        .append(", cloudlets=").append(vm.getCloudletScheduler().getCloudletExecList().size()).append(")");
                    System.out.println(sb);
                }
            }
            occupiedHosts = occupied;      // unconditional
            placementDumped = true;        // unconditional
        }

        double[] utils = new double[hosts.size()];
        double mean = 0.0;
        for (int i = 0; i < hosts.size(); i++) {
            double used = 0;
            for (GuestEntity vm : hosts.get(i).getGuestList())
                used += vm.getCurrentRequestedTotalMips();
            utils[i] = used / hosts.get(i).getTotalMips();
            mean += utils[i];
        }
        mean /= hosts.size();
        double variance = 0.0;
        for (double u : utils) variance += (u - mean) * (u - mean);
        varianceSum += variance / hosts.size();
        cycleCount++;

        schedule(getId(), OBSERVATION_RATE, CloudActionTags.VM_BROKER_EVENT);
    }

    public int getOccupiedHosts() {     
        return occupiedHosts;   
    }

    public double getAvgVariance() {
        return cycleCount == 0 ? 0.0 : varianceSum / cycleCount;
    }
}