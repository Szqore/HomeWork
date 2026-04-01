import java.util.Scanner;

class MatrixOperations19 {

    public static void printMatrix(int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.printf("%5d", matrix[i][j]);
            }
            System.out.println();
        }
    }

    public static int sumOfDigits(int number) {
        int sum = 0;
        number = Math.abs(number);
        while (number > 0) {
            sum += number % 10;
            number /= 10;
        }
        return sum;
    }

    public static int countNumbersOnAntiDiagonal(int[][] matrix, int sum0) {
        int count = 0;
        int n = matrix.length;
        for (int i = 0; i < n; i++) {
            int element = matrix[i][n - 1 - i];
            if (sumOfDigits(element) < sum0) {
                count++;
            }
        }
        return count;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int[][] matrix = {
                {123, 45, 67, 89},
                {234, 56, 78, 90},
                {345, 67, 89, 12},
                {456, 78, 90, 13}
        };

        System.out.println("Матрица:");
        printMatrix(matrix);

        System.out.print("\nВведите SUM0: ");
        int sum0 = scanner.nextInt();

        int result = countNumbersOnAntiDiagonal(matrix, sum0);
        System.out.println("\nКоличество чисел на побочной диагонали, сумма цифр которых меньше " + sum0 + ": " + result);

        scanner.close();
    }
}