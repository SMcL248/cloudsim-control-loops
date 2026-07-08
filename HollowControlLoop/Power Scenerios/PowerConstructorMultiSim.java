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
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyLeastFullByCapacity;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.VmAllocationWithSelectionPolicy;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;

/**
 * TIMING / SCALING NOTES (2026-07-03)
 *
 * This version adds three things on top of the original, specifically so a run of this
 * file can answer "how long will 10,000 combos x N scenarios actually take" with real
 * data instead of a linear extrapolation from a single small sample:
 *
 *   1. Scenario seeding is now a real parameter, not hardcoded. The original createVM/
 *      createCloudlet both did `new Random(42)` unconditionally, so every one of the 375
 *      permutations in the original sweep ran against the literal same VM/cloudlet layout.
 *      "10 vs 100 random scenarios" wasn't actually a runnable experiment until this changed.
 *      Scenario loop is OUTERMOST (see main()), so every module combination is evaluated
 *      against the identical set of scenario seeds -- a blocked/common-random-numbers design,
 *      not independently-sampled scenarios per combo. scenarioSeed is passed unmodified to
 *      BOTH createVM and createCloudlet (an earlier revision offset the cloudlet seed to
 *      "decorrelate" the two draws; that was unnecessary -- separate Random instances don't
 *      share state just because they're given the same seed -- and it broke reproducibility
 *      of the original hardcoded-42 scenario's results. Fixed: scenario_seed=42 now
 *      reproduces the original run's VM and cloudlet layout exactly).
 *
 *   2. Per-run wall-clock timing (System.nanoTime around runSimulation only -- excludes
 *      JVM startup/classloading, which is a one-time cost you don't pay again at scale).
 *      Written to the CSV per row and summarised (mean/median/p95/max) at the end, so you
 *      get a real distribution instead of trusting "total time / run count".
 *
 *   3. Streaming CSV writes instead of buffering all SimulationResults and writing at the
 *      end. At 375 runs, deferred writing was harmless. At 100,000-1,000,000 runs, it means
 *      a crash on run 900,000 loses everything and you get zero visibility into progress
 *      while it's running. Results are now flushed periodically and progress is printed.
 *
 * Suggested use: set NUM_SCENARIOS small first (e.g. 20-50) and time the run. That gives
 * you ~7,500-19,000 runs at today's 5-module-variant scale -- enough to see whether
 * per-run cost is actually flat (watch the printed progress rate: it should stabilise,
 * not drift upward from GC pressure) before committing laptop time to the full sweep at
 * 10 variants/module x 10 or 100 scenarios.
 */
public class PowerConstructorMultiSim {

    public static HollowedControl<double[], LoadState[], int[]> broker;

    private static List<Cloudlet> cloudletList;
    private static List<PowerVm> vmlist;

    static PrintWriter csvWriter;

    private static final int[] MIPS_TIERS = {250, 500, 1000};

    // How many distinct scenarios to run every module combination against. Set to 1 to
    // reproduce the original single-scenario behaviour exactly (scenario seed = BASE).
    private static final int NUM_SCENARIOS = 10;
    private static final long SCENARIO_SEED_BASE = 42L;

    // NOTE (2026-07-03): VM and cloudlet generation each use their own `new Random(seed)`
    // instance, so feeding both the same scenarioSeed does NOT correlate their draws --
    // each Random object's internal state only evolves from calls made to that object.
    // An earlier version of this file applied a CLOUDLET_SEED_OFFSET here to "decorrelate"
    // them; that was unnecessary and had the side effect of making scenario_seed=42 draw
    // different cloudlets than the original hardcoded-Random(42) run, silently breaking
    // reproducibility of the historical 6149.35 baseline. Removed -- scenarioSeed is now
    // passed unmodified to both generators, matching the original file's behaviour exactly
    // when NUM_SCENARIOS=1 / scenarioSeed=42.

    // Flush + progress-print cadence. Flushing every row is safe but adds I/O overhead at
    // six-figure run counts; flushing every N rows is a reasonable crash-safety/throughput
    // tradeoff.
    private static final int PROGRESS_INTERVAL = 500;

    // Kept raw: these registries only ever get .getClass()'d, so there's nothing
    // to gain from fighting Java's "generic array creation" restriction here.
    static Monitor[] monitorDict = {
        new monitor_v1(), 
        new monitor_v2(), 
        new monitor_v3(), 
        new monitor_v4(), 
        new monitor_v5()
    };
    static Analyser[] analyserDict = {
        new analyser_v1(), 
        new analyser_v2(), 
        new analyser_v3(), 
        new analyser_v4(), 
        new analyser_v5()
    };
    static Planner[] plannerDict = {
        new planner_v1(), 
        new planner_v2(), 
        new planner_v3(), 
        new planner_v4(), 
        new planner_v5()
    };
    static Executor[] executorDict = {
        new executor_v1()
    };

    public static void main (String[] args) throws Exception{

        int compatibleCounter = 0;
        int failCounter = 0;
        int compatibleAndFailCounter = 0;
        int notCompatibleAndSucceededCounter = 0;

        int combosPerScenario = monitorDict.length * analyserDict.length * plannerDict.length * executorDict.length;
        int numCombinations = combosPerScenario * NUM_SCENARIOS;

        initCsv();

        // Timing bookkeeping. Kept separate from SimulationResult (lighter than storing
        // every full result object) so a 1,000,000-run sweep doesn't need to hold much more
        // than a long[] in memory for the final percentile summary.
        long[] runtimesNanos = new long[numCombinations];
        int runIndex = 0;

        long sweepStartNanos = System.nanoTime();

        for (int scenarioIdx = 0; scenarioIdx < NUM_SCENARIOS; scenarioIdx++) {

            long scenarioSeed = SCENARIO_SEED_BASE + scenarioIdx;

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

                            long runStartNanos = System.nanoTime();
                            SimulationResult result = runSimulation(mFresh, aFresh, pFresh, eFresh, scenarioSeed);
                            long runNanos = System.nanoTime() - runStartNanos;
                            runtimesNanos[runIndex] = runNanos;

                            logResult(result, scenarioSeed, runNanos);

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

                            runIndex++;

                            if (runIndex % PROGRESS_INTERVAL == 0 || runIndex == numCombinations) {
                                csvWriter.flush();
                                printProgress(runIndex, numCombinations, sweepStartNanos);
                            }
                        }
                    }
                }
            }
        }

        csvWriter.close();

        long sweepTotalNanos = System.nanoTime() - sweepStartNanos;
        printTimingSummary(runtimesNanos, sweepTotalNanos);

        Log.printlnConcat((double) compatibleCounter/(numCombinations) * 100, "% of combinations are semantically compatible.");
        Log.printlnConcat((double) failCounter/(numCombinations) * 100, "% of combinations could not be deployed.");
        if (compatibleCounter > 0) {
            Log.printlnConcat((double) compatibleAndFailCounter / compatibleCounter * 100, "% of semantically compatible combinations could not be deployed.");
        }
        if (numCombinations - compatibleCounter > 0) {
            Log.printlnConcat((double) notCompatibleAndSucceededCounter / (numCombinations-compatibleCounter) * 100, "% of non-semantically compatible combinations can be deployed.");
        }

    }

    private static void printProgress(int done, int total, long sweepStartNanos) {
        double elapsedSec = (System.nanoTime() - sweepStartNanos) / 1e9;
        double rate = done / elapsedSec; // runs/sec
        double remainingSec = (total - done) / rate;
        System.out.printf(
            "[progress] %d/%d runs (%.1f%%) | elapsed=%.1fs | rate=%.1f runs/s | ETA=%.1fs%n",
            done, total, 100.0 * done / total, elapsedSec, rate, remainingSec);
    }

    private static void printTimingSummary(long[] runtimesNanos, long sweepTotalNanos) {
        long[] sorted = runtimesNanos.clone();
        Arrays.sort(sorted);

        int n = sorted.length;
        double meanMs = Arrays.stream(sorted).average().orElse(0) / 1e6;
        double medianMs = sorted[n / 2] / 1e6;
        double p95Ms = sorted[(int) (n * 0.95)] / 1e6;
        double maxMs = sorted[n - 1] / 1e6;
        double minMs = sorted[0] / 1e6;
        double totalSec = sweepTotalNanos / 1e9;

        System.out.println();
        System.out.println("=== Timing summary (per-run wall clock, excludes JVM startup) ===");
        System.out.printf("  runs:    %d%n", n);
        System.out.printf("  min:     %.2f ms%n", minMs);
        System.out.printf("  mean:    %.2f ms%n", meanMs);
        System.out.printf("  median:  %.2f ms%n", medianMs);
        System.out.printf("  p95:     %.2f ms%n", p95Ms);
        System.out.printf("  max:     %.2f ms%n", maxMs);
        System.out.printf("  total sweep wall time: %.1f s (%.2f min)%n", totalSec, totalSec / 60.0);
        System.out.println();
        System.out.println("Extrapolation guide (this machine, single-threaded, steady state):");
        System.out.printf("  10,000 combos x 10  scenarios = 100,000 runs  ~= %.1f min%n", (meanMs * 100_000) / 60_000.0);
        System.out.printf("  10,000 combos x 100 scenarios = 1,000,000 runs ~= %.1f min%n", (meanMs * 1_000_000) / 60_000.0);
        System.out.println("  (Use mean if runtime looks flat across the run; use p95/max instead");
        System.out.println("   if a subset of module variants are visibly more expensive per cycle.)");
    }

    // The one place in the file that asserts "reflection + registry membership
    // is sufficient proof of type" — narrowly scoped, nothing else hides behind it.
    @SuppressWarnings("unchecked")
    private static <T> T instantiate(Class<?> clazz) throws Exception {
        return (T) clazz.getDeclaredConstructor().newInstance();
    }

    static void initCsv() throws IOException {
        csvWriter = new PrintWriter(new FileWriter("simulation_results_power_multi.csv"));
        csvWriter.println("compatible,monitor,analyser,planner,executor,scenario_seed,makespan,average_cpu_demand_variance,average_power,actionable_cycles,opportunity_cycles,actions_proposed,actions_executed,conversion_rate,runtime_ms,status");
    }
   
    // == Run simulaiton ==
    // Infrastrucure:
    // - 1 Datacenter
    // - 6 quad-core hosts (1000 MIPS per PE)
    // - 12 VMs (250, 500, 1000 MIPS)
    // - 60 Cloudlets (10,000 - 500,000 Length)
    // - Utilisation Model: Full
    // - Power Model: Linear
    // - VM Allocation Policy: Least Full
    // - Cloudlet Allocation Policy: Round robin
    // - Cloudlet submission times: 60 @ t=0
    static SimulationResult runSimulation(Monitor<double[]> m, Analyser<double[], LoadState[]> a,
                                          Planner<LoadState[], int[]> p, Executor<int[]> e, long scenarioSeed) {

        boolean compatible = m.outputGuid().equals(a.inputGuid()) &&
            a.outputGuid().equals(p.inputGuid()) &&
            p.outputGuid().equals(e.inputGuid());

        // Reset before the attempt so a caught exception can never read a stale broker left
        // over from the PREVIOUS run (broker is a shared static field; this was flagged as a
        // pre-existing risk in the original code and matters more now that failures, even at
        // a low rate, become more likely simply from running orders of magnitude more combos).
        broker = null;

		try {

            Log.disable();

			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;

			CloudSim.init(num_user, calendar, trace_flag);

			PowerDatacenter datacenter0 = createDatacenter("Datacenter_0", 6, 4);

            datacenter0.setDisableMigrations(true);

			broker = new HollowedControl<>(
                "broker_0",
                100,
                m,
                a,
                p,
                e,
                diagnosis -> hasAny(diagnosis, LoadState.BALANCED),
                diagnosis -> countOf(diagnosis, LoadState.BALANCED) >= 2,
                actions -> !Arrays.equals(actions, new int[]{-1, -1})
            );

			int brokerId = broker.getId();

			vmlist = createVM(brokerId, 12, 0, scenarioSeed);
			cloudletList = createCloudlet(brokerId, 60, 0, scenarioSeed);

			broker.submitGuestList(vmlist);
			broker.submitCloudletList(cloudletList);

			CloudSim.startSimulation();

			List<Cloudlet> newList = broker.getCloudletReceivedList();

            double makespan = newList.stream().mapToDouble(Cloudlet::getExecFinishTime).max().orElse(-1);

			CloudSim.stopSimulation();

            Log.enable();

            return new SimulationResult(
                m.getClass().getSimpleName(),
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

            int ic = (broker != null) ? broker.getImbalanceCycles() : 0;
            int oc = (broker != null) ? broker.getOpportunityCycles() : 0;
            int ap = (broker != null) ? broker.getActionsProposed() : 0;
            int ae = (broker != null) ? broker.getActionsExecuted() : 0;
            double gtv = (broker != null) ? broker.getGroundTruthAvgVariance() : 0;

            return new SimulationResult(m.getClass().getSimpleName(),
                a.getClass().getSimpleName(),
                p.getClass().getSimpleName(),
                e.getClass().getSimpleName(),
                ic, 
                oc, 
                ap, 
                ae,
                -1, 
                compatible, 
                gtv,
                -1
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
            datacenter = new PowerDatacenter(
                name, characteristics, 
                new VmAllocationWithSelectionPolicy(hostList, new SelectionPolicyLeastFullByCapacity<>()),
                new LinkedList<>(), 
                1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private static List<PowerVm> createVM(int userId, int vms, int idShift, long scenarioSeed) {
		LinkedList<PowerVm> list = new LinkedList<>();

        Random rng = new Random(scenarioSeed);

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

	private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift, long scenarioSeed){
		LinkedList<Cloudlet> list = new LinkedList<>();

        Random random = new Random(scenarioSeed);

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

    // Checks for at least one occurance of a given LoadState in diagnosis
    private static boolean hasAny(LoadState[] arr, LoadState target) {
        for (LoadState s : arr) if (s == target) return true;
        return false;
    }

    // Count number of occurances of a given LoadState in diagnosis
    private static int countOf(LoadState[] arr, LoadState target) {
        int i = 0;
        for (LoadState s : arr){
            if (s == target){
                i++;
            }
        }
        return i;
    }

    static void logResult(SimulationResult result, long scenarioSeed, long runtimeNanos) {

        boolean failed = result.makespan() == -1;
        boolean inert  = !failed && result.actionableCycles() == 0;
        double conversionRate = (!failed && result.actionableCycles() > 0)
                ? (double) result.actionsExecuted() / result.actionableCycles()
                : -1;

        if (failed) {
            Log.printlnConcat(
                    "Compatible: ", result.compatible(), " [",
                    result.monitorId(), " + ", result.analyserId(), " + ",
                    result.plannerId(), " + ", result.executorId(), "] scenario=",
                    scenarioSeed, " FAILED");

        }

        String status = failed ? "FAILED" : inert ? "INERT" : "ACTIVE";
        double conversionRateCell = (conversionRate >= 0) ? conversionRate : 0;
        double makespanCell = failed ? 0 : result.makespan();
        double groundTruthCell = failed ? 0 : result.groundTruthAvgVariance();
        double powerCell = failed ? 0 : result.energy()/makespanCell;
        double runtimeMs = runtimeNanos / 1e6;

        csvWriter.printf("%b,%s,%s,%s,%s,%d,%.2f,%.6f,%.2f,%d,%d,%d,%d,%.6f,%.3f,%s%n",
                result.compatible(),
                result.monitorId(),
                result.analyserId(),
                result.plannerId(),
                result.executorId(),
                scenarioSeed,
                makespanCell,
                groundTruthCell,
                powerCell,
                result.actionableCycles(),
                result.opportunityCycles(),
                result.actionsProposed(),
                result.actionsExecuted(),
                conversionRateCell,
                runtimeMs,
                status);
    }

}