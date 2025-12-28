package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.ArrayList;
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
        List<ComputationNode> children = node.getChildren();
        ComputationNodeType opType = node.getNodeType();

        if(opType == ComputationNodeType.NEGATE || opType == ComputationNodeType.TRANSPOSE)
        {
            if(children.size() != 1)
                throw new IllegalArgumentException("Unary operator " + opType + " requires exactly 1 operand, got " + children.size());
            leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix());
        }    
        else{
            if(children.size() < 2)
                throw new IllegalArgumentException("Binary operator " + opType + " requires at least 2 operands, got " + children.size());
            leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix());
            rightMatrix.loadRowMajor(node.getChildren().get(1).getMatrix());
        }
            
        // TODO: create compute tasks & submit tasks to executor
        List<Runnable> tasks;
        try{
            switch (node.getNodeType()) {
                case ADD:
                    tasks = createAddTasks();
                    break;
                case MULTIPLY:
                    tasks = createMultiplyTasks();
                    break;
                case NEGATE:
                    tasks = createNegateTasks();
                    break;  
                case TRANSPOSE:
                    tasks = createTransposeTasks();
                    break;    
                default:
                    throw new IllegalArgumentException("Unsupported operation: " + node.getNodeType());
                }
            }
            catch(IllegalArgumentException e){
                throw e;
            } 
            // Submit all tasks and wait for completion
            executor.submitAll(tasks);
            double[][]result = leftMatrix.readRowMajor();
            node.resolve(result);
        }

    public List<Runnable> createAddTasks() {
        if(leftMatrix.length() != rightMatrix.length())
            throw new IllegalArgumentException("Matrices must have the same number of rows to add");

        List<Runnable> tasks = new ArrayList<>();
        
        for(int i=0;i<leftMatrix.length();i++){
            final int rowIndex = i;
            Runnable newTask = () -> {
                SharedVector leftRow = leftMatrix.get(rowIndex);
                SharedVector rightRow = rightMatrix.get(rowIndex);

                leftRow.writeLock();
                rightRow.readLock();
                try{
                    leftRow.add(rightRow);
                }catch(IllegalArgumentException e)
                {
                    throw e;
                }
                finally{
                    rightRow.readUnlock();
                    leftRow.writeUnlock();
                }
            };
            tasks.add(newTask);
        }

        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        List<Runnable> tasks = new ArrayList<>();
        
        for(int i=0;i<leftMatrix.length();i++){
            final int rowIndex = i;
            Runnable newTask = () -> {
                SharedVector leftRow = leftMatrix.get(rowIndex);

                leftRow.writeLock();
                for(int k=0;k<rightMatrix.length();k++){
                    rightMatrix.get(k).readLock();
                }

                try{
                    leftRow.vecMatMul(rightMatrix);
                }catch(IllegalArgumentException e)
                {
                    throw e;
                }
                finally{
                    for(int k=rightMatrix.length()-1;k>=0;k--){
                    rightMatrix.get(k).readUnlock();
                }
                    leftRow.writeUnlock();
            }
        };
            tasks.add(newTask);
        }

        return tasks;
    }

    public List<Runnable> createNegateTasks() {
       List<Runnable> tasks = new ArrayList<>();
        
        for(int i=0;i<leftMatrix.length();i++){
            final int rowIndex = i;
            Runnable newTask = () -> {
                SharedVector leftRow = leftMatrix.get(rowIndex);

                leftRow.writeLock();
                try{
                    leftRow.negate();
                }
                finally{
                    leftRow.writeUnlock();
                }
            };
            tasks.add(newTask);
        }

        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        List<Runnable> tasks = new ArrayList<>();
        
        double[][] currentData = leftMatrix.readRowMajor();
        leftMatrix.loadColumnMajor(currentData);
        for(int i=0;i<leftMatrix.length();i++){
            final int rowIndex = i;
            Runnable newTask = () -> {
                SharedVector leftRow = leftMatrix.get(rowIndex);

                leftRow.writeLock();
                try{
                    leftRow.transpose();
                }
                finally{
                    leftRow.writeUnlock();
                }
            };
            tasks.add(newTask);
        }

        return tasks;
    }
    }

    public String getWorkerReport() {
        return executor.getWorkerReport();
    }
}
