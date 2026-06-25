package org.cloudbus.cloudsim.examples;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;

public class Constructor {

    public static HollowedControl broker;

    /** The cloudlet list. */
	private static List<Cloudlet> cloudletList;

	/** The vmlist. */
	private static List<Vm> vmlist;

    static List<SimulationResult> results = new ArrayList<>();

    static Monitor[] monitorDict = {new monitor_v1(), new monitor_v2(), new monitor_v3()};
    static Analyser[] analyserDict = {new analyser_v1(), new analyser_v2(), new analyser_v3()};
    static Planner[] plannerDict = {new planner_v1(), new planner_v2(), new planner_v3()};
    static Executor[] executorDict = {new executor_v1(), new executor_v2(), new executor_v3()};

    public static void main (String[] args) throws Exception{

        int compatibleCounter = 0;
        int failCounter = 0;
        int compatibleAndFailCounter = 0;
        int notCompatibleAndSucceededCounter = 0;
        int numCombinations = monitorDict.length * analyserDict.length * plannerDict.length * executorDict.length;

        for (Monitor m : monitorDict){
            for (Analyser a : analyserDict) {
                for (Planner p : plannerDict){
                    for (Executor e : executorDict){

                        Monitor mFresh = m.getClass().getDeclaredConstructor().newInstance();
                        Analyser aFresh = a.getClass().getDeclaredConstructor().newInstance();
                        Planner pFresh = p.getClass().getDeclaredConstructor().newInstance();
                        Executor eFresh = e.getClass().getDeclaredConstructor().newInstance();

                        SimulationResult result = runSimulation(mFresh, aFresh, pFresh, eFresh);
                        results.add(result);

                        if (m.outputGuid().equals(a.inputGuid()) && a.outputGuid().equals(p.inputGuid()) && p.outputGuid().equals(e.inputGuid())){
                            compatibleCounter++;
                        }

                        if (result.makespan() == -1){
                            failCounter++;
                        }

                        if (m.outputGuid().equals(a.inputGuid()) && a.outputGuid().equals(p.inputGuid()) && p.outputGuid().equals(e.inputGuid()) && result.makespan() == -1){
                            compatibleAndFailCounter++;
                        }

                        if (!(m.outputGuid().equals(a.inputGuid()) && a.outputGuid().equals(p.inputGuid()) && p.outputGuid().equals(e.inputGuid())) && result.makespan() != -1){
                            notCompatibleAndSucceededCounter++;
                        }
                    }
                }
            }
        }

        for (SimulationResult r : results){
                logResult(r);
        }

        Log.printlnConcat((double) compatibleCounter/(numCombinations) * 100, "% of combinations are semantically compatible.");
        Log.printlnConcat((double) failCounter/(numCombinations) * 100, "% of combinations failed.");
        if (compatibleCounter > 0) {
            Log.printlnConcat((double) compatibleAndFailCounter / compatibleCounter * 100, "% of semantically compatible combinations failed.");
        }
        if (numCombinations - compatibleCounter > 0) {
            Log.printlnConcat((double) notCompatibleAndSucceededCounter / (numCombinations-compatibleCounter) * 100, "% of non-semantically compatible combinations succeeded.");
        }
    }

    static SimulationResult runSimulation(Monitor m, Analyser a, Planner p, Executor e) {

       // Log.println("Starting ManualControllerVmMigrationSimple...");

		try {

            Log.disable();

			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1;   // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  // mean trace events

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);

			// Second step: Create Datacenters
			//Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
			Datacenter datacenter0 = createDatacenter("Datacenter_0", 4, 4);

			//Third step: Create Broker
			broker = new HollowedControl("Broker_0", 100, m, a, p, e);
			int brokerId = broker.getId();

			//Fourth step: Create VMs and Cloudlets and send them to broker
			vmlist = createVM(brokerId, 16, 0); //creating 5 vms
			cloudletList = createCloudlet(brokerId, 60, 0); // creating 10 cloudlets

			broker.submitGuestList(vmlist);
			broker.submitCloudletList(cloudletList);

			// Fifth step: Starts the simulation
			CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();

            double makespan = newList.stream().mapToDouble(Cloudlet::getExecFinishTime).max().orElse(-1);

			CloudSim.stopSimulation();

		//	Log.println("ManualControllerVmMigrationSimple finished!");

            Log.enable();

            return new SimulationResult(m.getClass().getSimpleName(), a.getClass().getSimpleName(), p.getClass().getSimpleName(), e.getClass().getSimpleName(), a.getActionableCycles(), e.getActionsExecuted(),  makespan);

		}
		catch (Exception exception)
		{
			exception.printStackTrace();
            Log.enable();
			Log.println("The simulation has been terminated due to an unexpected error");
            return new SimulationResult(m.getClass().getSimpleName(), a.getClass().getSimpleName(), p.getClass().getSimpleName(), e.getClass().getSimpleName(), a.getActionableCycles(), e.getActionsExecuted(), -1);    

		}

    }

    private static Datacenter createDatacenter(String name, int numHosts, int pesPerHost) {

        int mips    = 1000;
        int ram     = 16384;
        long storage = 1000000;
        int bw      = 10000;

        List<Host> hostList = new ArrayList<>();

        for (int hostId = 0; hostId < numHosts; hostId++) {

            List<Pe> peList = new ArrayList<>();
            for (int peId = 0; peId < pesPerHost; peId++) {
                peList.add(new Pe(peId, new PeProvisionerSimple(mips)));
            }

            hostList.add(new Host(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                new VmSchedulerTimeShared(peList)
            ));
        }

        String arch      = "x86";
        String os        = "Linux";
        String vmm       = "Xen";
        double time_zone = 10.0;
        double cost          = 3.0;
        double costPerMem    = 0.05;
        double costPerStorage = 0.1;
        double costPerBw     = 0.1;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
            arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private static List<Vm> createVM(int userId, int vms, int idShift) {
		//Creates a container to store VMs. This list is passed to the broker later
		LinkedList<Vm> list = new LinkedList<>();

		//VM Parameters
		long size = 10000; //image size (MB)
		int ram = 512; //vm memory (MB)
		int mips = 250;
		long bw = 1000;
		int pesNumber = 1; //number of cpus
		String vmm = "Xen"; //VMM name

		//create VMs
		Vm[] vm = new Vm[vms];

		for(int i=0;i<vms;i++){
			vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			list.add(vm[i]);
		}

		return list;
	}


	private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift){
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<>();

        Random random = new Random(42); // fixed seed

		//cloudlet parameters
		long minLength = 10000;
        long maxLength = 100000;
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for(int i=0;i<cloudlets;i++){
			cloudlet[i] = new Cloudlet(idShift + i, (long)(minLength + random.nextDouble() * (maxLength - minLength)), pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}


    static void logResult(SimulationResult result) {

        if (result.makespan() != -1 && result.actionableCycles() >0){
            Log.printlnConcat(
            "[",
            result.monitorId(), " + ",
            result.analyserId(), " + ",
            result.plannerId(), " + ",
            result.executorId(), "] makespan=",
            Math.round(result.makespan()), "| actionable cycles=",
            result.actionableCycles(), "| actions executed=",
            result.actionsExecuted(), "| actionable to action conversion rate=",
            Math.round((double) result.actionsExecuted()/result.actionableCycles() * 100), "%"
            );
        }else if (result.makespan() != -1 ){
            Log.printlnConcat(
            "[",
            result.monitorId(), " + ",
            result.analyserId(), " + ",
            result.plannerId(), " + ",
            result.executorId(), "] makespan=",
            Math.round(result.makespan()), "| actionable cycles=",
            result.actionableCycles(), "| actions executed=",
            result.actionsExecuted()
            );
        }else{
            Log.printlnConcat(
            "[ ",
            result.monitorId(), " + ",
            result.analyserId(), " + ",
            result.plannerId(), " + ",
            result.executorId(), "] FAILED"
            );
        }

    }

    
}
