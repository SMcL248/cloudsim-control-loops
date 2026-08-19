package org.cloudbus.cloudsim.examples;

import java.util.Scanner;

/**
 * Generates a MOSS command-line invocation for comparing a batch of
 * generated module variants against their shared skeleton/base file(s).
 *
 * Skeleton convention: one flat skeleton per module type, <type>_skel.java.
 * Variant files: <type>_v1.java ... <type>_vN.java
 *
 * Usage:
 *   java MossPromptGenerator <type> <numVariants>
 *   java MossPromptGenerator                        (interactive prompt)
 *
 * Example:
 *   java MossPromptGenerator monitor 10
 *   -> perl moss.pl -l java -m 1000000 -c "Monitor v1-v10, skeleton excluded" -b monitor_skel.java monitor_v1.java monitor_v2.java ... monitor_v10.java
 *
 * -m is forced to a large fixed value rather than left at MOSS's default (10).
 * MOSS's -m caps how many submissions a shared code segment can appear in
 * before it's treated as "too common to be interesting" and silently dropped
 * from every pairwise report -- a sensible default for classroom plagiarism
 * detection (shared starter code isn't a cheating signal), but wrong moihere:
 * a pattern recurring across many variants IS the signal this comparison is
 * meant to surface. Left at the default, batches at or above ~10-11 files
 * start silently losing legitimate shared-pattern matches, which would alsoanalyser
 * make similarity scores incomparable across different batch sizes.
 */
public class mossPromptGenerator {

    private static final int MAX_MATCHES = 1000000;

    public static void main(String[] args) {
        String type;
        int numVariants;

        if (args.length >= 2) {
            type = args[0].trim().toLowerCase();
            numVariants = Integer.parseInt(args[1]);
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Module type (monitor, analyser, planner, executor): ");
            type = scanner.nextLine().trim().toLowerCase();
            System.out.print("Number of variants: ");
            numVariants = Integer.parseInt(scanner.nextLine().trim());
            scanner.close();
        }

        if (type.isEmpty()) {
            System.err.println("Module type must not be empty.");
            return;
        }
        if (numVariants < 1) {
            System.err.println("Number of variants must be at least 1.");
            return;
        }

        String skeleton = type + "_skel.java";

        String capitalizedType = Character.toUpperCase(type.charAt(0)) + type.substring(1);
        String comment = capitalizedType + " v1-v" + numVariants + ", skeleton excluded";

        StringBuilder command = new StringBuilder();
        command.append("perl moss.pl -l java -m ").append(MAX_MATCHES);
        command.append(" -c \"").append(comment).append("\"");
        command.append(" -b ").append(skeleton);

        for (int i = 1; i <= numVariants; i++) {
            command.append(" ").append(type).append("_v").append(i).append(".java");
        }

        System.out.println(command.toString());
    }
}