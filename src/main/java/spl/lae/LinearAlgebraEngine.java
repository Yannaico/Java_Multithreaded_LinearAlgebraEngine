package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.List;
import java.util.Vector;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        this.executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        if(computationRoot == null){
            throw new IllegalArgumentException("Computation root cannot be null");
        }
        // CASE 1: Root is already a matrix - nothing to compute
        if (computationRoot.getNodeType() == ComputationNodeType.MATRIX)
            return computationRoot;


        // Preprocess the computation tree to group associative operations
        computationRoot.associativeNesting();


        // Repeatedly find and compute resolvable nodes until the root is a matrix
        while(computationRoot.getNodeType() != ComputationNodeType.MATRIX){
           ComputationNode resolvable =computationRoot.findResolvable();
           loadAndCompute(resolvable);

        }
        return computationRoot;// Return Matrix node

    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: create compute tasks & submit tasks to executor
        leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix());
        rightMatrix.loadRowMajor(node.getChildren().get(1).getMatrix());
        List<Runnable> tasks;
        try{
            switch (node.getNodeType()) {
                case ADD:
                    tasks = createAddTasks();
                    executor.submitAll(tasks);
                    break;
                case MULTIPLY:
                    tasks = createMultiplyTasks();
                    executor.submitAll(tasks);
                    break;
                case NEGATE:
                    tasks = createNegateTasks();
                    executor.submitAll(tasks);
                    break;  
                case TRANSPOSE:
                    tasks = createTransposeTasks();
                    executor.submitAll(tasks);
                    break;    
                default:
                    throw new IllegalArgumentException("Unsupported operation: " + node.getNodeType());
                }
            }
            catch(IllegalArgumentException e){
                throw e;
            }
        }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        return null;
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        return null;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        return null;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        return null;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return null;
    }
}
