package com.uber.nullaway.benchmark;

import com.google.errorprone.ErrorProneCompiler;
import com.uber.nullaway.NullAway;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/** harness for benchmarking NullAway for some javac task */
public class NullAwayBenchmarkHarness {

  /**
   * If true, we just add NullAway to the processorpath but otherwise leave the javac args
   * unmodified (see {@link #justRun(String[])}). If false, we use the logic of {@link
   * #addNullAwayArgsAndRun(String[])}
   */
  private static boolean JUST_RUN = true;

  private static boolean DEBUG = false;
  private static int warmupRuns = 0;
  private static int realRuns = 10;

  private static void processBenchmarkingArgs(List<String> args) {
    boolean check = true;
    while (check) {
      check = true;
      switch (args.get(0)) {
        case "-w":
        case "-warmupRuns":
          warmupRuns = Integer.parseInt(args.remove(1));
          break;
        case "-r":
        case "-realRuns":
          realRuns = Integer.parseInt(args.remove(1));
          break;
        case "-na":
        case "-enableNullAway":
          JUST_RUN = false;
          break;
        case "-debug":
          DEBUG = true;
          break;
        default:
          check = false;
      }
      if (check) args.remove(0);
    }
  }

  public static void main(String[] args) {
    List<String> javacArgs = new ArrayList<String>(Arrays.asList(args));
    processBenchmarkingArgs(javacArgs);
    boolean result;
    if (JUST_RUN) {
      result = justRun(javacArgs);
    } else {
      result = addNullAwayArgsAndRun(javacArgs);
    }
    if (!result) System.exit(1);
  }

  /**
   * Some recommendations for this mode.
   *
   * <ul>
   *   <li>Disable all other checks but NullAway by passing {@code -XepDisableAllChecks
   *       -Xep:NullAway:WARN} after other EP options
   *   <li>If you want to just benchmark the baseline without NullAway, only pass {@code
   *       -XepDisableAllChecks} (you'll have to do this in a different run)
   * </ul>
   */
  private static boolean justRun(List<String> args) {
    System.out.println("Running...");
    return runCompile(args);
  }

  /**
   * Here we assume that the javac command has no existing processorpath and no other error prone
   * flags are being passed. In this case, we assume the annotated packages are passed as the first
   * argument and the remaining javac args as the rest. We run two configs, one with NullAway added
   * in a warning-only mode and one with no NullAway.
   *
   * @param args
   */
  private static boolean addNullAwayArgsAndRun(List<String> args) {
    String nullawayJar = getJarFileForClass(NullAway.class).getFile();
    String annotPackages = "-XepOpt:NullAway:AnnotatedPackages=com.uber";
    Iterator<String> argi = args.iterator();
    while (argi.hasNext()) {
      String arg = argi.next();
      if (arg.startsWith("-XepOpt:NullAway:AnnotatedPackages=")) {
        argi.remove();
        annotPackages = arg;
        break;
      }
    }
    args.remove("-XepDisableAllChecks");
    List<String> nullawayArgs =
        new ArrayList<String>(
            Arrays.asList("-XepDisableAllChecks", "-Xep:NullAway:WARN", annotPackages));
    int index = args.lastIndexOf("-processorpath");
    if (index != -1) args.set(index + 1, nullawayJar + ":" + args.get(index + 1));
    else nullawayArgs.addAll(Arrays.asList("-processorpath", nullawayJar));
    nullawayArgs.addAll(args);
    System.out.println("Running with NullAway...");
    return runCompile(nullawayArgs);
  }

  private static boolean runCompile(List<String> fixedArgs) {
    String[] finalArgs = fixedArgs.toArray(new String[fixedArgs.size()]);
    if (DEBUG) System.out.println("[DEBUG] compile args: " + String.join(" ", finalArgs));
    for (int i = 0; i < warmupRuns; i++) {
      System.out.println("Warmup Run " + (i + 1));
      double startTime = System.nanoTime();
      ErrorProneCompiler.compile(finalArgs);
      double endTime = System.nanoTime();
      System.out.println("Running time " + (((double) endTime - startTime) / 1000000000.0));
    }
    double totalRunningTime = 0;
    boolean allOK = true;
    for (int i = 0; i < realRuns; i++) {
      System.out.println("Real Run " + (i + 1));
      double startTime = System.nanoTime();
      boolean compileStatus = ErrorProneCompiler.compile(finalArgs).isOK();
      allOK &= compileStatus;
      double endTime = System.nanoTime();
      double runTime = endTime - startTime;
      System.out.println("Running time " + (((double) runTime) / 1000000000.0));
      totalRunningTime += runTime;
    }
    System.out.println(
        "Average running time "
            + String.format("%.2f", ((double) totalRunningTime / 1000000000.0) / realRuns));
    return allOK;
  }

  private static URL getJarFileForClass(Class<?> klass) {
    return klass.getProtectionDomain().getCodeSource().getLocation();
  }
}
