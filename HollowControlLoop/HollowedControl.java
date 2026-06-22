package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.VmAllocationPolicy.GuestMapping;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.HostEntity;


public class HollowedControl<M,D,A> extends DatacenterBroker implements ActionSpace {

    private final Monitor<M> monitor;
    private final Analyser<M,D> analyser;
    private final Planner<D,A> planner;
    private final Executor<A> executor;
    private final int observationRate;

    public HollowedControl(String name, int observationRate, Monitor<M> monitor, Analyser<M,D> analyser, Planner<D,A> planner, Executor<A> executor) throws Exception {
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

    @Override
    // This method sends a cloudlet to a datacenter with a specified delay. It uses the send() method of the broker.
    public void moveCloudlet(Cloudlet cloudlet, GuestEntity fromVm, GuestEntity toVm, int destDatacenterId) {
        int[] data = new int[5];
        data[0] = cloudlet.getCloudletId();
        data[1] = cloudlet.getUserId();
        data[2] = fromVm.getId();
        data[3] = toVm.getId();
        data[4] = destDatacenterId;
        send(getDatacenterFor(fromVm), 0, CloudActionTags.CLOUDLET_MOVE, data);
    }

    @Override
    public void requestVmMigration(GuestEntity vm, HostEntity targetHost){

        GuestMapping payload = new GuestMapping(vm, targetHost);
        send(getDatacenterFor(vm), 0, CloudActionTags.VM_MIGRATE, payload);

    }

    @Override
    public List<GuestEntity> getVmList() {
        return getGuestsCreatedList();
    }

    @Override
    public Integer getUserId() {
        return getId();
    }

    @Override
    public void requestVmCreation(GuestEntity newVm, int datacenterId) {
        getGuestList().add(newVm);
        sendNow(datacenterId, CloudActionTags.VM_CREATE, newVm);
    }

    @Override
    public List<HostEntity> getAllHosts() {

        return getDatacenterCharacteristicsList().values().iterator().next().getHostList();    

    }

    @Override
    public double getNow(){
        return CloudSim.clock();
    }


    // This method observes the current state of the system, analyzes it, plans migrations if necessary, and executes them.
    private void observeAndAct() {

        // If there are no cloudlets to process, do nothing
        if (getCloudletList().isEmpty() && getCloudletSubmittedList().size() == getCloudletReceivedList().size()) {
            return;
        }

        // Control loop: monitor, analyze, plan, and execute (MAPE)
        M metrics = monitor.observe(this);
        D diagnosis = analyser.analyse(metrics, this);
        A actions = planner.plan(diagnosis, this);
        boolean success = executor.execute(actions, this);

        // Print the results of the control loop
        if (!success) {
            Log.printlnConcat("The system is balanced. No ", executor.actionDescription(), " needed.");
        }

        // Schedule the next observation
        schedule(getId(), observationRate, CloudActionTags.VM_BROKER_EVENT);

    }
        
}

    


