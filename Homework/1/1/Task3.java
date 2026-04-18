import java.util.Scanner; //task 3

public class MatrixOperations {

    public static int countSequences(int a, int b) { } 
        if (a > b + 1) {
            return 0;  //Если нулей больше чем промежутков ,не возможно
        
        return combination(b + 1, a); 
    }

    public static int combination(int n, int k) {
        if (k < 0 || k > n) {
            return 0; //некоротеные аргументы
        }
        if (k == 0 || k == n) { // C(n,0) =C (n,n) =1
            return 1;
        }
        int numerator = 1;
        int denominator = 1; //числ и знам
        for (int i = 1; i <= k; i++) { .. //(n-k+1)*...*N / (1*2*...*K)
            numerator *= (n - k + i);
            denominator *= i;
        }
        return numerator / denominator; //резул всег целый
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in); //чит keyboard

        System.out.print("Введите количество нулей (a): "); //сохр 0
        int a = scanner.nextInt();

        System.out.print("Введите количество единиц (b): "); // сохр 1
        int b = scanner.nextInt();

        int result = countSequences(a, b); 
        System.out.println("Количество последовательностей: " + result);

        scanner.close();
    }
}
