package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.SimEvent;


public class HollowedControl extends DatacenterBroker implements SimulationContext {

    private final Monitor monitor;
    private final Analyser analyser;
    private final Planner planner;
    private final Executor executor;
    private final int observationRate;

    public HollowedControl(String name, int observationRate, Monitor monitor, Analyser analyser, Planner planner, Executor executor) throws Exception {
        super(name);
        this.observationRate = observationRate;
        this.monitor = monitor;
        this.analyser = analyser;
        this.planner = planner;
        this.executor = executor;
    }

    @Override
	public void processEvent(SimEvent ev) {
		CloudSimTags tag = ev.getTag();
        // Resource characteristics request
        if (tag == CloudActionTags.RESOURCE_CHARACTERISTICS_REQUEST) {
            processResourceCharacteristicsRequest(ev);

            // Resource characteristics answer
        } else if (tag == CloudActionTags.RESOURCE_CHARACTERISTICS) {
            processResourceCharacteristics(ev);

            // VM Creation answer
        } else if (tag == CloudActionTags.VM_CREATE_ACK) {
            processVmCreateAck(ev);

            // A finished cloudlet returned
        } else if (tag == CloudActionTags.CLOUDLET_RETURN) {
            processCloudletReturn(ev);

            // if the simulation finishes
        } else if (tag == CloudActionTags.END_OF_SIMULATION) {
            shutdownEntity();

        } else if (tag == CloudActionTags.VM_BROKER_EVENT) {
            observeAndAct();
        }else {
            processOtherEvent(ev);
        }
	}

    @Override
    // This method is called at the beginning of the simulation. It schedules first observation.
    public void startEntity() {
        super.startEntity();
        schedule(getId(), observationRate, CloudActionTags.VM_BROKER_EVENT);
    }

    @Override
    // This method is called at the end of the simulation. It cancels any pending events for this entity.
    public void shutdownEntity() {
        CloudSim.cancelAll(getId(), CloudSim.SIM_ANY);
        super.shutdownEntity();
    }

    @Override
    // This method returns the datacenter ID for a given VM. It uses the mapping of VMs to datacenters maintained by the broker.
    public Integer getDatacenterFor(GuestEntity vm) {
        return getVmsToDatacentersMap().get(vm.getId());
    }

    @Override
    // This method sends a cloudlet to a datacenter with a specified delay. It uses the send() method of the broker.
    public void sendCloudlet(int datacenterId, double delay, Cloudlet cloudlet) {
        send(datacenterId, delay, CloudActionTags.CLOUDLET_SUBMIT, cloudlet);
    }

    // This method observes the current state of the system, analyzes it, plans migrations if necessary, and executes them.
    private void observeAndAct() {

        // If there are no cloudlets to process, do nothing
        if (getCloudletList().isEmpty() && getCloudletSubmittedList().size() == getCloudletReceivedList().size()) {
            return;
        }

        // Take a snapshot of the current state of the system
        WorldState worldState = new WorldState(getGuestsCreatedList(), List.of(), CloudSim.clock());

        // Control loop: monitor, analyze, plan, and execute (MAPE)
        Map<GuestEntity, Map<String, Double>> metrics = monitor.observe(worldState);
        Diagnosis diagnosis = analyser.analyse(metrics);
        List<MigrationPair> migrations = planner.plan(diagnosis);
        boolean success = executor.execute(migrations, this);

        // Print the results of the control loop
        if (!success) {
            Log.println("The system is balanced. No migration needed.");
        }

        // Schedule the next observation
        schedule(getId(), observationRate, CloudActionTags.VM_BROKER_EVENT);
    }
        
}

    


