package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        this.vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        this.vectors = new SharedVector[matrix.length];
        loadRowMajor(matrix);
    }

    public void loadRowMajor(double[][] matrix) {
        if(matrix == null || matrix.length == 0){
            this.vectors = new SharedVector[0];
            return;
        }
        int numRows = matrix.length;
        SharedVector[] newVectors = new SharedVector[numRows];
        for (int i = 0; i < numRows; i++) {
            newVectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }
        this.vectors = newVectors;
    }

    public void loadColumnMajor(double[][] matrix) {
        if(matrix == null || matrix.length == 0){
            this.vectors = new SharedVector[0];
            return;
        }

        int numRows = matrix.length;
        int numCols = matrix[0].length;

        SharedVector[] newVectors = new SharedVector[numCols];
        for(int i=0;i< numCols;i++){
            double[] tempColVector = new double[numRows];
            for(int j=0;j< numRows;j++){
                tempColVector[j]=matrix[j][i];
            }
            newVectors[i] = new SharedVector(tempColVector, VectorOrientation.COLUMN_MAJOR);
        }
        this.vectors = newVectors;
    }

    public double[][] readRowMajor() {
        // TODO: return matrix contents as a row-major double[][]
        boolean isColumnMajor = false;//Read coulumn major convert to row major
        
        if(this.vectors.length == 0){
            return new double[0][0];
        }

        int numCols;
        int numRows;
        if(vectors[0].getOrientation() == VectorOrientation.COLUMN_MAJOR){
            numCols = this.vectors.length;
            numRows = this.vectors[0].length();
            isColumnMajor = true;
        }
        else{
            numRows = this.vectors.length;
            numCols = this.vectors[0].length();
        }

        acquireAllVectorReadLocks(vectors);
        double[][] result;
        try{
            result = new double[numRows][numCols];
            for(int i=0;i<numRows;i++){
                for(int j=0;j<numCols;j++){

                    if(isColumnMajor)
                        result[i][j] = this.vectors[j].get(i);
                    else{
                        result[i][j] = this.vectors[i].get(j);
                    }
                    
                }
            }
        }
        finally{
            releaseAllVectorReadLocks(vectors);
        }
        return result;
    }

    public SharedVector get(int index) {
        return this.vectors[index];
    }

    public int length() {
        return this.vectors.length;
    }

    public VectorOrientation getOrientation() {
        if(this.vectors == null || this.length() == 0)
            throw new IllegalArgumentException();
        return this.vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        for(int i=0;i<vecs.length;i++)
        {
            vecs[i].readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
         for(int i=0;i<vecs.length;i++)
        {
            vecs[i].readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
         for(int i=0;i<vecs.length;i++)
        {
            vecs[i].writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        
         for(int i=0;i<vecs.length;i++)
        {
            vecs[i].writeUnlock();
        }
    }
}
