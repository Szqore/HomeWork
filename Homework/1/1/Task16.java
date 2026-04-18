import java.util.Random; //task 16

public class MatrixOperations {

    public static int[] findMinMaxInArray(int[] array) {
        int min = array[0];
        int max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) min = array[i]; //обнволяем если мин нагли меньше
            if (array[i] > max) max = array[i];//обнволяем если мах нагли больше
        }
        return new int[]{min, max}; //массив
    }

    public static int[] findMinMaxInMatrix(int[][] matrix) {
        int globalMin = Integer.MAX_VALUE; // Начинаем с максимально возможного числа
        int globalMax = Integer.MIN_VALUE; // Начинаем с минимально возможного числа
        for (int[] row : matrix) {  // Перебираем каждую строку матрицы
            int[] rowMinMax = findMinMaxInArray(row); // Находим min/max в строке
            if (rowMinMax[0] < globalMin) globalMin = rowMinMax[0]; //обнв min
            if (rowMinMax[1] > globalMax) globalMax = rowMinMax[1]; //обнв max
        }
        return new int[]{globalMin, globalMax};
    }
// Метод для красивого вывода матрицы на экран
    public static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) { // Проходим по строкам
            for (int value : row) { // Проходим по элементам строки
                System.out.printf("%4d", value); // Печатаем число, выделяя 4 позиции
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        Random rand = new Random();
        int[][] matrix = new int[4][5];
        for (int i = 0; i < matrix.length; i++) { // Проходим по столбцам
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] = rand.nextInt(100);
            }
        }

        System.out.println("Матрица:");
        printMatrix(matrix);

        int[] result = findMinMaxInMatrix(matrix); // Находим min и max в матрице
        System.out.println("\nНаименьший элемент: " + result[0]);
        System.out.println("Наибольший элемент: " + result[1]);
    }
}
