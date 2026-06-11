package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Runs both broker scenarios across 100 seeds and writes results to CSV.
 *
 * Output columns: seed, makespan_rr, makespan_cb, delta (rr - cb)
 * A positive delta means ControlBroker finished faster.
 *
 * Suggested statistical analysis (Python/R):
 *   - Paired t-test or Wilcoxon signed-rank on the delta column
 *   - Histogram of deltas to visualise the distribution
 *   - Mean and std of each broker's makespan
 */
public class ExperimentRunner {

    private static final int    NUM_SEEDS   = 100;
    private static final long   SEED_OFFSET = 1000L; // seeds: 1000, 1001, ..., 1099
    private static final String OUTPUT_FILE = "experiment_results.csv";

    public static void main(String[] args) {

        // Suppress CloudSim's per-run console noise
        Log.disable();

        BaseScenario roundRobin    = new RoundRobinScenario();
        BaseScenario controlBroker = new ControlBrokerScenario();

        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_FILE))) {

            writer.println("seed,makespan_rr,makespan_cb,delta");

            for (int i = 0; i < NUM_SEEDS; i++) {
                long seed = SEED_OFFSET + i;

                double rrMakespan = runSafely(roundRobin,    seed, i);
                double cbMakespan = runSafely(controlBroker, seed, i);
                double delta      = rrMakespan - cbMakespan;

                writer.printf("%d,%.2f,%.2f,%.2f%n", seed, rrMakespan, cbMakespan, delta);

                // Re-enable briefly for progress, then suppress again
                Log.enable();
                System.out.printf("Seed %4d | RR: %10.2f | CB: %10.2f | Delta: %+.2f%n",
                        seed, rrMakespan, cbMakespan, delta);
                Log.disable();
            }

            System.out.println("\nResults written to: " + OUTPUT_FILE);

        } catch (IOException e) {
            System.err.println("Failed to write output file: " + e.getMessage());
        }
    }

    /**
     * Runs a scenario, catches any exception so one bad seed doesn't abort the experiment.
     * Returns -1.0 on failure so it's visible as an outlier in the CSV.
     */
    private static double runSafely(BaseScenario scenario, long seed, int runIndex) {
        try {
            return scenario.run(seed);
        } catch (Exception e) {
            System.err.printf("Error in %s run %d (seed %d): %s%n",
                    scenario.getName(), runIndex, seed, e.getMessage());
            return -1.0;
        }
    }
}
