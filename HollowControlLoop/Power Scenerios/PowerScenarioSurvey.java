package org.cloudbus.cloudsim.examples;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.VmAllocationWithSelectionPolicy;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicy;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyCustomRandom;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyLeastFullByCapacity;

public class PowerScenarioSurvey {

    public static MeasuringBroker broker;

	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList;

	/** The vmlist. */
	private static List<PowerVm> vmlist;

	// Possible VM MIPS capacities
    private static final int[] MIPS_TIERS = {250, 500, 1000};

    private static final int NUM_SEEDS = 100;
    private static final long SEED = 42;

    // Scenario architecture
	private static final int NUM_VMS = 12;
	private static final int NUM_CLOUDLETS = 60;
	private static final int NUM_HOSTS = 6;

    // Random allocation of entites switch
	private static final boolean RANDOM_PLACEMENT = true;// Should VMs be randomly allocated to hosts?
	private static final boolean RANDOM_ASSIGNMENT = true;// Should cloudlets be randomly allocated to VMs?

    private static List<PowerVm> createVM(int userId, int vms, int idShift, long seed) {
		//Creates a container to store VMs. This list is passed to the broker later
		LinkedList<PowerVm> list = new LinkedList<>();

        Random rng = new Random(seed);

		//VM Parameters
		long size = 10000; //image size (MB)
		int ram = 512; //vm memory (MB)
		long bw = 1000;
		int pesNumber = 1; //number of cpus
		String vmm = "Xen"; //VMM name

		//create VMs
		PowerVm[] vm = new PowerVm[vms];

		for(int i=0;i<vms;i++){
			vm[i] = new PowerVm(
                idShift + i, 
                userId, 
                MIPS_TIERS[rng.nextInt(MIPS_TIERS.length)], 
                pesNumber, 
                ram, 
                bw, 
                size, 
                1,
                vmm, 
                new CloudletSchedulerTimeShared(), 
                1
            );
			list.add(vm[i]);
            //Log.println("VM #" + vm[i].getId() + " | MIPS: " + vm[i].getMips());
		}

		return list;

	}

	private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift, long seed){
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<>();

		Random lengthRng = new Random(seed);
		Random assignRng = new Random(seed^ 0x9E3779B97F4A7C15L);  

		//cloudlet parameters
		long minLength = 10000;
        long maxLength = 500000;
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for(int i=0;i<cloudlets;i++){
			cloudlet[i] = new Cloudlet(
				idShift + i, 
				(long)(minLength + lengthRng.nextDouble() * (maxLength - minLength)), 
				pesNumber, 
				fileSize, 
				outputSize, 
				utilizationModel, 
				utilizationModel, 
				utilizationModel);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			if (RANDOM_ASSIGNMENT) {
    			cloudlet[i].setGuestId(assignRng.nextInt(NUM_VMS));// VM ids are 0..numVms-1 with idShift 0
			}
			list.add(cloudlet[i]);
		}

		return list;
	}

    public static void main(String[] args) throws Exception {

        try (PrintWriter csv = new PrintWriter(new FileWriter("scenario_survey_clean.csv"))) {

            csv.println("scenario_seed,placement,assignment,makespan,total_energy,"
                      + "average_power,average_cpu_demand_variance,occupied_hosts,max_vm_cloudlets");

            for (int s = 0; s < NUM_SEEDS; s++) {
                long seed = SEED + s;
                csv.println(runOne(seed));
                csv.flush();                    // streaming writes — crash-safe
                System.out.println("seed " + seed + " done");
            }

        }

    }

    private static String runOne(long seed) throws Exception {

        CloudSim.init(1, Calendar.getInstance(), false);   // fresh init per run

        PowerDatacenter datacenter0 = createDatacenter("datacenter_0", NUM_HOSTS, 4, seed);  // fresh policy instance inside

        datacenter0.setDisableMigrations(true);

        MeasuringBroker broker = new MeasuringBroker("broker_0" + seed);  // fresh broker
        int brokerId = broker.getId();

        //Fourth step: Create VMs and Cloudlets and send them to broker
        vmlist = createVM(brokerId, NUM_VMS, 0, seed); //creating 5 vms
        cloudletList = createCloudlet(brokerId, NUM_CLOUDLETS, 0, seed); // creating 10 cloudlets

        int[] perVm = new int[NUM_VMS];
        for (Cloudlet cl : cloudletList)
            if (cl.getGuestId() >= 0) perVm[cl.getGuestId()]++;
        int maxPerVm = RANDOM_ASSIGNMENT
            ? java.util.Arrays.stream(perVm).max().getAsInt()
            : NUM_CLOUDLETS / NUM_VMS;

        broker.submitGuestList(vmlist);
        broker.submitCloudletList(cloudletList);

        Log.disable();
        // Fifth step: Starts the simulation
        CloudSim.startSimulation();

        CloudSim.stopSimulation();
        Log.enable();


        double makespan = maxFinishTime(broker.getCloudletReceivedList()); 

        String placementLabel  = RANDOM_PLACEMENT  ? "RANDOM" : "LEAST_FULL";
        String assignmentLabel = RANDOM_ASSIGNMENT ? "RANDOM" : "ROUND_ROBIN";

        return seed + "," + placementLabel + "," + assignmentLabel + "," + makespan + ","
         + datacenter0.getPower() + "," + (datacenter0.getPower() / makespan) + ","
         + broker.getAvgVariance() + "," + broker.getOccupiedHosts() + "," + maxPerVm;

    }

    private static PowerDatacenter createDatacenter(String name, int numHosts, int pesPerHost, long seed) {

        int mips    = 1000;
        int ram     = 16384;
        long storage = 1000000;
        int bw      = 10000;

        List<FixedPowerHost> hostList = new ArrayList<>();

        for (int hostId = 0; hostId < numHosts; hostId++) {

            List<Pe> peList = new ArrayList<>();
            for (int peId = 0; peId < pesPerHost; peId++) {
                peList.add(new Pe(peId, new PeProvisionerSimple(mips)));
            }

            hostList.add(new FixedPowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                new VmSchedulerTimeShared(peList),
                new PowerModelLinear(250,0.6)
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

		SelectionPolicy<HostEntity> selectionPolicy = (RANDOM_PLACEMENT) 
			? new SelectionPolicyCustomRandom<>(seed) 
			: new SelectionPolicyLeastFullByCapacity<>();

        PowerDatacenter datacenter = null;
        try {
            datacenter = new PowerDatacenter(
				name, 
				characteristics, 
				new VmAllocationWithSelectionPolicy(
					hostList, 
					selectionPolicy
				), 
				new LinkedList<>(), 
				1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private static double maxFinishTime(List<Cloudlet> list) {
        double max = -1;
        for (Cloudlet cl : list) max = Math.max(max, cl.getExecFinishTime());
        return max;
    }

}