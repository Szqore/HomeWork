import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Function;

public class CustomPriorityQueue<T> {
    private final PriorityQueue<Element<T>> heap;
    private final Function<T, Integer> priorityExtractor;
    private long idCounter = 0;

    private static class Element<E> {
        final E value;
        final int priority;
        final long id;

        Element(E value, int priority, long id) {
            this.value = value;
            this.priority = priority;
            this.id = id;
        }
    }

    public CustomPriorityQueue(Function<T, Integer> priorityExtractor) {
        this.priorityExtractor = priorityExtractor;

        // Приоритеты по убыванию. Если равны  по возрастанию id (FIFO)
        Comparator<Element<T>> comparator = (a, b) -> {
            if (b.priority != a.priority) {
                return Integer.compare(b.priority, a.priority);
            }
            return Long.compare(a.id, b.id);
        };

        this.heap = new PriorityQueue<>(comparator);
    }

    public void add(T item) {
        int priority = priorityExtractor.apply(item);
        heap.add(new Element<>(item, priority, idCounter++));
    }

    public T poll() {
        return heap.isEmpty() ? null : heap.poll().value;
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }
}
