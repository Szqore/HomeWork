import java.util.List;

public class AnimalUtils {

    //Копирует собак в конец списка животных
    public static void copyAnimals(List<? extends Dog> src, List<? super Animal> dst) {
        dst.addAll(src);
    }

    //Очищает список и заполняет тремя новыми кошками
    public static void fillWithCats(List<? super Cat> dst) {
        dst.clear();
        dst.add(new Cat("cat1"));
        dst.add(new Cat("cat2"));
        dst.add(new Cat("cat3"));
    }

    //Универсальный безопасный перенос элементов из src в dst
    public static <T> void safeTransfer(List<? extends T> src, List<? super T> dst) {
        dst.addAll(src);
        src.clear();
    }
}
