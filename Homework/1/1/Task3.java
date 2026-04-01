import java.util.Scanner;

class SequencesCount {

    public static int countSequences(int a, int b) {
        if (a > b + 1) {
            return 0;
        }
        return combination(b + 1, a);
    }

    public static int combination(int n, int k) {
        if (k < 0 || k > n) {
            return 0;
        }
        if (k == 0 || k == n) {
            return 1;
        }
        int numerator = 1;
        int denominator = 1;
        for (int i = 1; i <= k; i++) {
            numerator *= (n - k + i);
            denominator *= i;
        }
        return numerator / denominator;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Введите количество нулей (a): ");
        int a = scanner.nextInt();

        System.out.print("Введите количество единиц (b): ");
        int b = scanner.nextInt();

        int result = countSequences(a, b);
        System.out.println("Количество последовательностей: " + result);

        scanner.close();
    }
}