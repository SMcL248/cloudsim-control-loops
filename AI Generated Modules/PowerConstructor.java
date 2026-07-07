package org.cloudbus.cloudsim.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.power.PowerDatacenter;
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
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.VmAllocationPolicySimpler;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;

public class PowerConstructor {

    public static HollowedControl<double[], LoadState[], int[]> broker;

    private static List<Cloudlet> cloudletList;
    private static List<PowerVm> vmlist;

    static List<SimulationResult> results = new ArrayList<>();
    static PrintWriter csvWriter;

    private static final int[] MIPS_TIERS = {250, 500, 1000};

    // Kept raw: these registries only ever get .getClass()'d, so there's nothing
    // to gain from fighting Java's "generic array creation" restriction here.
    static Monitor[] monitorDict = {new monitor_v1(), new monitor_v2(), new monitor_v3(), new monitor_v4(), new monitor_v5()};
    static Analyser[] analyserDict = {new analyser_v1(), new analyser_v2(), new analyser_v3(), new analyser_v4(), new analyser_v5()};
    static Planner[] plannerDict = {new planner_v1(), new planner_v2(), new planner_v3(), new planner_v4(), new planner_v5()};
    static Executor[] executorDict = {new executor_v1(), new executor_v2(), new executor_v3()};

    public static void main (String[] args) throws Exception{

        int compatibleCounter = 0;
        int failCounter = 0;
        int compatibleAndFailCounter = 0;
        int notCompatibleAndSucceededCounter = 0;
        int numCombinations = monitorDict.length * analyserDict.length * plannerDict.length * executorDict.length;

        initCsv();

        int counter = 0;

        for (Monitor m : monitorDict){
            for (Analyser a : analyserDict) {
                for (Planner p : plannerDict){
                    for (Executor e : executorDict){

                        // Single chokepoint for the unchecked cast — see instantiate() below.
                        // Target-typing on the LHS infers T = Monitor<double[]> etc. at each call.
                        Monitor<double[]> mFresh = instantiate(m.getClass());
                        Analyser<double[], LoadState[]> aFresh = instantiate(a.getClass());
                        Planner<LoadState[], int[]> pFresh = instantiate(p.getClass());
                        Executor<int[]> eFresh = instantiate(e.getClass());

                        SimulationResult result = runSimulation(mFresh, aFresh, pFresh, eFresh);
                        results.add(result);

                        if (result.compatible()){
                            compatibleCounter++;
                        }

                        if (result.makespan() == -1){
                            failCounter++;
                        }

                        if (result.compatible() &&
                            result.makespan() == -1){
                            compatibleAndFailCounter++;
                        }

                        if (!(result.compatible()) &&
                            result.makespan() != -1){
                            notCompatibleAndSucceededCounter++;
                        }

                    }
                }
            }
        }

        for (SimulationResult r : results){
                logResult(r);
        }

        csvWriter.close();

        Log.printlnConcat((double) compatibleCounter/(numCombinations) * 100, "% of combinations are semantically compatible.");
        Log.printlnConcat((double) failCounter/(numCombinations) * 100, "% of combinations could not be deployed.");
        if (compatibleCounter > 0) {
            Log.printlnConcat((double) compatibleAndFailCounter / compatibleCounter * 100, "% of semantically compatible combinations could not be deployed.");
        }
        if (numCombinations - compatibleCounter > 0) {
            Log.printlnConcat((double) notCompatibleAndSucceededCounter / (numCombinations-compatibleCounter) * 100, "% of non-semantically compatible combinations can be deployed.");
        }

    }

    // The one place in the file that asserts "reflection + registry membership
    // is sufficient proof of type" — narrowly scoped, nothing else hides behind it.
    @SuppressWarnings("unchecked")
    private static <T> T instantiate(Class<?> clazz) throws Exception {
        return (T) clazz.getDeclaredConstructor().newInstance();
    }

    static void initCsv() throws IOException {
        csvWriter = new PrintWriter(new FileWriter("simulation_results_energy.csv"));
        csvWriter.println("compatible,monitor,analyser,planner,executor,makespan,average_cpu_demand_variance,average_power,actionable_cycles,opportunity_cycles,actions_proposed,actions_executed,conversion_rate,status");
    }

    static SimulationResult runSimulation(Monitor<double[]> m, Analyser<double[], LoadState[]> a,
                                          Planner<LoadState[], int[]> p, Executor<int[]> e) {

        boolean compatible = m.outputGuid().equals(a.inputGuid()) &&
            a.outputGuid().equals(p.inputGuid()) &&
            p.outputGuid().equals(e.inputGuid());

		try {

            Log.disable();

			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;

			CloudSim.init(num_user, calendar, trace_flag);

			PowerDatacenter datacenter0 = createDatacenter("Datacenter_0", 6, 4);

			broker = new HollowedControl<>(
                "broker_0",
                100,
                m,
                a,
                p,
                e,
                diagnosis -> hasAny(diagnosis, LoadState.OVERLOADED) || hasAny(diagnosis, LoadState.UNDERLOADED),
                diagnosis -> hasAny(diagnosis, LoadState.OVERLOADED) && hasAny(diagnosis, LoadState.UNDERLOADED),
                actions -> !Arrays.equals(actions, new int[]{-1, -1, -1})
            );

			int brokerId = broker.getId();

			vmlist = createVM(brokerId, 12, 0);
			cloudletList = createCloudlet(brokerId, 60, 0);

			broker.submitGuestList(vmlist);
			broker.submitCloudletList(cloudletList);

			CloudSim.startSimulation();

			List<Cloudlet> newList = broker.getCloudletReceivedList();

            double makespan = newList.stream().mapToDouble(Cloudlet::getExecFinishTime).max().orElse(-1);

			CloudSim.stopSimulation();

            Log.enable();

            return new SimulationResult(m.getClass().getSimpleName(),
                a.getClass().getSimpleName(),
                p.getClass().getSimpleName(),
                e.getClass().getSimpleName(),
                broker.getImbalanceCycles(),
                broker.getOpportunityCycles(),
                broker.getActionsProposed(),
                broker.getActionsExecuted(),
                makespan, 
                compatible, 
                broker.getGroundTruthAvgVariance(),
                datacenter0.getPower()
            );

		}
		catch (Exception exception)
		{
			exception.printStackTrace();
            Log.enable();
			Log.println("The simulation has been terminated due to an unexpected error");
            // NOTE (pre-existing, not introduced by this change): `broker` is a shared static
            // field. If the exception happened before `broker = new HollowedControl<>(...)`
            // completed this run, these getters report the PREVIOUS run's leftover object,
            // not this one. Worth a null-guard if FAILED rows ever need to be trusted.
            return new SimulationResult(m.getClass().getSimpleName(),
                a.getClass().getSimpleName(),
                p.getClass().getSimpleName(),
                e.getClass().getSimpleName(),
                broker.getImbalanceCycles(),
                broker.getOpportunityCycles(),
                broker.getActionsProposed(),
                broker.getActionsExecuted(),
                -1, 
                compatible, 
                broker.getGroundTruthAvgVariance()
            );

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
                new PowerModelLinear(250, 0.6)
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

        PowerDatacenter datacenter = null;
        try {
            datacenter = new PowerDatacenter(name, characteristics, new VmAllocationPolicySimpler(hostList), new LinkedList<>(), 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

	private static List<PowerVm> createVM(int userId, int vms, int idShift) {
		LinkedList<PowerVm> list = new LinkedList<>();

        Random rng = new Random(42);

		long size = 10000;
		int ram = 512;
		long bw = 1000;
		int pesNumber = 1;
		String vmm = "Xen";

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
                100
            );
			list.add(vm[i]);
            Log.println("VM #" + vm[i].getId() + " | MIPS: " + vm[i].getMips());
		}

		return list;

	}

	private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift){
		LinkedList<Cloudlet> list = new LinkedList<>();

        Random random = new Random(42);

		long minLength = 10000;
        long maxLength = 500000;
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for(int i=0;i<cloudlets;i++){
			cloudlet[i] = new Cloudlet(idShift + i, (long)(minLength + random.nextDouble() * (maxLength - minLength)), pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}

    private static boolean hasAny(LoadState[] arr, LoadState target) {
        for (LoadState s : arr) if (s == target) return true;
        return false;
    }

    static void logResult(SimulationResult result) {

        boolean failed = result.makespan() == -1;
        boolean inert  = !failed && result.actionableCycles() == 0;
        double conversionRate = (!failed && result.actionableCycles() > 0)
                ? (double) result.actionsExecuted() / result.actionableCycles()
                : -1;

        if (failed) {
            Log.printlnConcat(
                    "Compatible: ", result.compatible(), " [",
                    result.monitorId(), " + ", result.analyserId(), " + ",
                    result.plannerId(), " + ", result.executorId(), "] FAILED");

        } else if (inert) {
            Log.printlnConcat(
                    "Compatible: ", result.compatible(), " [",
                    result.monitorId(), " + ", result.analyserId(), " + ",
                    result.plannerId(), " + ", result.executorId(), "] makespan=",
                    Math.round(result.makespan()), "| actionable cycles=",
                    result.actionableCycles(), "| opportunity cycles=",
                    result.opportunityCycles(), "| actions executed=",
                    result.actionsExecuted());

        } else {
            Log.printlnConcat(
                    "Compatible: ", result.compatible(), " [",
                    result.monitorId(), " + ", result.analyserId(), " + ",
                    result.plannerId(), " + ", result.executorId(), "] makespan=",
                    Math.round(result.makespan()), "| actionable cycles=",
                    result.actionableCycles(), "| opportunity cycles=",
                    result.opportunityCycles(), "| actions executed=",
                    result.actionsExecuted(), "| conversion rate=",
                    conversionRate);
        }

        String status = failed ? "FAILED" : inert ? "INERT" : "ACTIVE";
        double conversionRateCell = (conversionRate >= 0) ? conversionRate : 0;
        double makespanCell = failed ? 0 : result.makespan();
        double groundTruthCell = failed ? 0 : result.groundTruthAvgVariance();
        double powerCell = failed ? 0 : result.energy()/makespanCell;

        csvWriter.printf("%b,%s,%s,%s,%s,%.2f,%.6f,%.2f,%d,%d,%d,%d,%.6f,%s%n",
                result.compatible(),
                result.monitorId(),
                result.analyserId(),
                result.plannerId(),
                result.executorId(),
                makespanCell,
                groundTruthCell,
                powerCell,
                result.actionableCycles(),
                result.opportunityCycles(),
                result.actionsProposed(),
                result.actionsExecuted(),
                conversionRateCell,
                status);
    }

}