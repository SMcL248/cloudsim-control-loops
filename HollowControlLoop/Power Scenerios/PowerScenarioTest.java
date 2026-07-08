/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.examples;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.VmAllocationWithSelectionPolicy;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyCustomRandom;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyFirstFit;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyLeastFull;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyLeastFullByCapacity;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicy;

/**
 * An example showing how to pause and resume the simulation,
 * and create simulation entities (a DatacenterBroker in this example)
 * dynamically.
 */
public class PowerScenarioTest {

    public static MeasuringBroker broker;

	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList;

	/** The vmlist. */
	private static List<PowerVm> vmlist;

	// Possible VM MIPS capacities
    private static final int[] MIPS_TIERS = {250, 500, 1000};

	// Random scenerio seed
	private static final long SEED = 42L;

	// Scenario architecture
	private static final int NUM_VMS = 12;
	private static final int NUM_CLOUDLETS = 60;
	private static final int NUM_HOSTS = 6;

	// Random allocation of entites switch
	private static final boolean RANDOM_PLACEMENT = true;// Should VMs be randomly allocated to hosts?
	private static final boolean RANDOM_ASSIGNMENT = true;// Should cloudlets be randomly allocated to VMs?

	private static List<PowerVm> createVM(int userId, int vms, int idShift) {
		//Creates a container to store VMs. This list is passed to the broker later
		LinkedList<PowerVm> list = new LinkedList<>();

        Random rng = new Random(SEED);

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
            Log.println("VM #" + vm[i].getId() + " | MIPS: " + vm[i].getMips());
		}

		return list;

	}

	private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift){
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<>();

		Random lengthRng = new Random(SEED);
		Random assignRng = new Random(SEED^ 0x9E3779B97F4A7C15L);  

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

	////////////////////////// STATIC METHODS ///////////////////////

	/**
	 * Creates main() to run this example
	 */
	public static void main(String[] args) {
		Log.println("Starting PowerScenarioTest...");

		try {
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1;   // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  // mean trace events

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);

			// Second step: Create Datacenters
			//Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
			PowerDatacenter datacenter0 = createDatacenter("Datacenter_0", NUM_HOSTS, 4);

            datacenter0.setDisableMigrations(true);

			//Third step: Create Broker
			broker = new MeasuringBroker("broker_0");
			int brokerId = broker.getId();

			//Fourth step: Create VMs and Cloudlets and send them to broker
			vmlist = createVM(brokerId, NUM_VMS, 0); //creating 5 vms
			cloudletList = createCloudlet(brokerId, NUM_CLOUDLETS, 0); // creating 10 cloudlets

			broker.submitGuestList(vmlist);
			broker.submitCloudletList(cloudletList);

			Log.disable();
			// Fifth step: Starts the simulation
			CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();

			CloudSim.stopSimulation();
			Log.enable();

			printCloudletList(newList);

            Log.println("Total energy: " + datacenter0.getPower() + " W*sec");
			Log.printlnConcat("Average CPU demand variance: ", broker.getAvgVariance());
			Log.println("PowerScenarioTest finished!");
		}
		catch (Exception e)
		{
			Log.enable();
			e.printStackTrace();
			Log.println("The simulation has been terminated due to an unexpected error");
		}
	}

    private static PowerDatacenter createDatacenter(String name, int numHosts, int pesPerHost) {

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
			? new SelectionPolicyCustomRandom<>(SEED) 
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

	/**
	 * Prints the Cloudlet objects
	 * @param list  list of Cloudlets
	 */
	private static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.println();
		Log.println("========== OUTPUT ==========");
		Log.println("Cloudlet ID" + indent + "STATUS" + indent +
				"Data center ID" + indent + "VM ID" + indent + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
        for (Cloudlet value : list) {
            cloudlet = value;
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                Log.print("SUCCESS");

                Log.println(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getGuestId() +
                        indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent + indent + dft.format(cloudlet.getExecFinishTime()));

            }
        }

	}

}