import java.util.Random;

class MatrixOperations {

    public static int[] findMinMaxInArray(int[] array) {
        int min = array[0];
        int max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) min = array[i];
            if (array[i] > max) max = array[i];
        }
        return new int[]{min, max};
    }

    public static int[] findMinMaxInMatrix(int[][] matrix) {
        int globalMin = Integer.MAX_VALUE;
        int globalMax = Integer.MIN_VALUE;
        for (int[] row : matrix) {
            int[] rowMinMax = findMinMaxInArray(row);
            if (rowMinMax[0] < globalMin) globalMin = rowMinMax[0];
            if (rowMinMax[1] > globalMax) globalMax = rowMinMax[1];
        }
        return new int[]{globalMin, globalMax};
    }

    public static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            for (int value : row) {
                System.out.printf("%4d", value);
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        Random rand = new Random();
        int[][] matrix = new int[4][5];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] = rand.nextInt(100);
            }
        }

        System.out.println("Матрица:");
        printMatrix(matrix);

        int[] result = findMinMaxInMatrix(matrix);
        System.out.println("\nНаименьший элемент: " + result[0]);
        System.out.println("Наибольший элемент: " + result[1]);
    }
}