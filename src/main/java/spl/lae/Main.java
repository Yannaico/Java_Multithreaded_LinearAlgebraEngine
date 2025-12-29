package spl.lae;
import java.io.IOException;
import java.text.ParseException;

import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
      if (args.length != 3) {
        System.err.println("Usage: java -jar lga-1.0.jar <numThreads> <inputFile> <outputFile>");
        System.exit(1);
      }
      int numThreads;
      try{
        numThreads = Integer.parseInt(args[0]);
        if(numThreads <= 0){
          System.err.println("Error: Number of threads must be positive");
          System.exit(1);
        }
      }catch(NumberFormatException e){
        System.err.println("Error: Invalid number of threads: " + args[0]);
        System.exit(1);
        return;
      }

      String input = args[1];
      String output = args[2];

      LinearAlgebraEngine engine = new LinearAlgebraEngine(numThreads);
      InputParser ip = new InputParser();
      ComputationNode root;
      try {
        root = ip.parse(input);

        ComputationNode result = engine.run(root);
        double[][] resultMatrix = result.getMatrix();
        OutputWriter.write(resultMatrix, output);

        System.out.println(engine.getWorkerReport());

      } catch (java.text.ParseException e) {
            System.err.println("Parse error: " + e.getMessage());
            try {
                OutputWriter.write("Parse error: " + e.getMessage(), output);
            } catch (IOException ioException) {
                System.err.println("Failed to write error: " + ioException.getMessage());
            }
            System.exit(1);
            
        } catch (IllegalArgumentException e) {
            System.err.println("Computation error: " + e.getMessage());
            try {
                OutputWriter.write("Illegal operation: dimensions mismatch", output);
            } catch (IOException ioException) {
                System.err.println("Failed to write error: " + ioException.getMessage());
            }
            System.exit(1);
            
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            try {
                OutputWriter.write("Error: " + e.getMessage(), output);
            } catch (IOException ioException) {
                System.err.println("Failed to write error: " + ioException.getMessage());
            }
            System.exit(1);
            
        } finally {
            try {
              if(engine != null)
                engine.shutdown();
            } catch (InterruptedException e) {
                System.err.println("Interrupted during shutdown");
                Thread.currentThread().interrupt();
            }
        }
    }
}