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

    public MeasuringBroker(String name) throws Exception { super(name); }

    @Override
    public void startEntity() {
        super.startEntity();
        schedule(getId(), OBSERVATION_RATE, CloudActionTags.VM_BROKER_EVENT);
    }

    @Override
    public void processEvent(SimEvent ev) {
        CloudSimTags tag = ev.getTag();
        if (tag == CloudActionTags.RESOURCE_CHARACTERISTICS_REQUEST) {
            processResourceCharacteristicsRequest(ev);
        } else if (tag == CloudActionTags.RESOURCE_CHARACTERISTICS) {
            processResourceCharacteristics(ev);
        } else if (tag == CloudActionTags.VM_CREATE_ACK) {
            processVmCreateAck(ev);
        } else if (tag == CloudActionTags.CLOUDLET_RETURN) {
            processCloudletReturn(ev);
        } else if (tag == CloudActionTags.END_OF_SIMULATION) {
            shutdownEntity();
        } else if (tag == CloudActionTags.VM_BROKER_EVENT) {
            measure();
        } else {
            processOtherEvent(ev);
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

        double now = CloudSim.clock();
        List<HostEntity> hosts = getDatacenterCharacteristicsList()
            .values().iterator().next().getHostList();

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

    public double getAvgVariance() {
        return cycleCount == 0 ? 0.0 : varianceSum / cycleCount;
    }
}