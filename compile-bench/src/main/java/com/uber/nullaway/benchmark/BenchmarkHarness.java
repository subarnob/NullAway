package com.uber.nullaway.benchmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** harness for benchmarking some javac task */
public class BenchmarkHarness {
  private static boolean DEBUG = false;
  private static int warmupRuns = 0;
  private static int realRuns = 10;

  private static void processBenchmarkingArgs(List<String> args) {
    boolean foundArg = true;
    while (foundArg) {
      switch (args.get(0)) {
        case "-w":
        case "-warmupRuns":
          warmupRuns = Integer.parseInt(args.remove(1));
          break;
        case "-r":
        case "-realRuns":
          realRuns = Integer.parseInt(args.remove(1));
          break;
        case "-debug":
          DEBUG = true;
          break;
        default:
          foundArg = false;
      }
      if (foundArg) args.remove(0);
    }
  }

  public static void main(String[] args) {
    List<String> javacArgs = new ArrayList<String>(Arrays.asList(args));
    processBenchmarkingArgs(javacArgs);
    if (!runCompile(javacArgs)) System.exit(1);
  }

  private static boolean runCompile(List<String> fixedArgs) {
    System.out.println("Running...");
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    String[] finalArgs = fixedArgs.toArray(new String[fixedArgs.size()]);
    if (DEBUG) System.out.println("[DEBUG] compile args: " + String.join(" ", finalArgs));
    for (int i = 0; i < warmupRuns; i++) {
      if (DEBUG) System.out.println("Warmup Run " + (i + 1));
      double startTime = System.nanoTime();
      compiler.run(System.in, System.out, System.err, finalArgs);
      double endTime = System.nanoTime();
      if (DEBUG)
        System.out.println("Running time " + (((double) endTime - startTime) / 1000000000.0));
    }
    double totalRunningTime = 0;
    boolean allOK = true;
    for (int i = 0; i < realRuns; i++) {
      if (DEBUG) System.out.println("Real Run " + (i + 1));
      double startTime = System.nanoTime();
      allOK &= compiler.run(System.in, System.out, System.err, finalArgs) == 0;
      double endTime = System.nanoTime();
      double runTime = endTime - startTime;
      if (DEBUG) System.out.println("Running time " + (((double) runTime) / 1000000000.0));
      totalRunningTime += runTime;
    }
    System.out.println(
        "Average running time "
            + String.format("%.2f%n", ((double) totalRunningTime / 1000000000.0) / realRuns));
    return allOK;
  }
}
