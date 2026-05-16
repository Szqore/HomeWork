import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Тест 1: Приоритетная очередь с FIFO ===");
        CustomPriorityQueue<String> pq = new CustomPriorityQueue<>(String::length);

        pq.add("A");
        pq.add("Второй");
        pq.add("Тест1");   // Одинаковый приоритет (длина 5)
        pq.add("Тест2");   // Одинаковый приоритет (длина 5)

        // Ожидаемый порядок вывода: "Второй" -> "Тест1" -> "Тест2" -> "A"
        while (!pq.isEmpty()) {
            System.out.println("Извлечено: " + pq.poll());
        }

        System.out.println("\n=== Тест 2: copyAnimals ===");
        List<Dog> dogs = new ArrayList<>(List.of(new Dog("Шарик"), new Dog("Бобик")));
        List<Animal> animals = new ArrayList<>(List.of(new Cat("Мурка")));

        AnimalUtils.copyAnimals(dogs, animals);
        System.out.println("Размер списка animals после копирования: " + animals.size());

        System.out.println("\n=== Тест 3: fillWithCats ===");
        AnimalUtils.fillWithCats(animals);
        System.out.println("Заполнили animals кошками. Размер: " + animals.size());

        List<Cat> catsOnly = new ArrayList<>();
        AnimalUtils.fillWithCats(catsOnly);
        System.out.println("Заполнили catsOnly кошками. Размер: " + catsOnly.size());

        System.out.println("\n=== Тест 4: safeTransfer ===");
        List<Dog> stable = new ArrayList<>(List.of(new Dog("Рекс"), new Dog("Мухтар")));
        List<Animal> clinic = new ArrayList<>();

        AnimalUtils.safeTransfer(stable, clinic);
        System.out.println("Размер stable после переноса: " + stable.size());
        System.out.println("Размер clinic после переноса: " + clinic.size());
    }
}
