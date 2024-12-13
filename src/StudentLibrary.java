import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 Клас Library моделює студентську бібліотеку з обмеженою кількістю копій книг.
 Використовується Semaphore для обмеження кількості студентів, які можуть одночасно "тримати" книги.
 */
class Library {
    private final Semaphore bookCopies;
    private volatile boolean isOpen;

    /**
     * @param copies - кількість доступних копій книги.
     */
    public Library(int copies) {
        this.bookCopies = new Semaphore(copies, true);
        this.isOpen = true; // бібліотека відкрита
    }

    /**
      Метод для отримання книги студентом.
      Якщо бібліотека закрита, кинути виняток.
     */
    public void takeBook(String studentName) throws IllegalStateException {
        if (!isOpen) {
            throw new IllegalStateException("Бібліотека зачинена. " + studentName + " не може взяти книгу.");
        }

        try {
            // Очікуємо доступ до книги
            bookCopies.acquire();
            System.out.println(studentName + " отримав(ла) книгу.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Процес " + studentName + " перервано під час очікування книги.");
        }
    }

    /**
     Метод для повернення книги. Якщо бібліотека закрита, можна не повертати, але це варіативно.
     За логікою тут можна повернути, якщо книга була взята до закриття. Але за умовою завдання, після закриття не можна ні брати, ні повертати книгу.
     Тому, якщо бібліотека вже закрита, вважаємо що студент не може повернути книгу (наприклад, поверне наступного дня).
     */
    public void returnBook(String studentName) throws IllegalStateException {
        if (!isOpen) {
            throw new IllegalStateException("Бібліотека зачинена. " + studentName + " не може повернути книгу зараз.");
        }

        bookCopies.release();
        System.out.println(studentName + " повернув(ла) книгу.");
    }

    /**
     Закрити бібліотеку. Після цього неможливо брати або повертати книги.
     */
    public void closeLibrary() {
        isOpen = false;
        System.out.println("Бібліотека зачинена! Подальші операції неможливі.");
    }

    public boolean isOpen() {
        return isOpen;
    }
}

/**
 Клас Student реалізує інтерфейс Runnable і описує поведінку студента, який у випадковий момент часу бере книгу, тримає її певний час, а потім повертає.
 */
class Student implements Runnable {
    private final String name;
    private final Library library;
    private final Random random;

    public Student(String name, Library library) {
        this.name = name;
        this.library = library;
        this.random = new Random();
    }

    @Override
    public void run() {
        try {
            // Чекаємо випадковий час перед спробою взяти книгу (імітація реальної поведінки)
            Thread.sleep(random.nextInt(2000) + 500);

            // Спроба взяти книгу
            try {
                library.takeBook(name);
            } catch (IllegalStateException e) {
                System.out.println(e.getMessage());
                return; // якщо не взяли книгу через закриття, студент закінчує роботу
            }

            // Студент "читає" книгу певний час
            Thread.sleep(random.nextInt(3000) + 1000);

            // Спроба повернути книгу
            try {
                library.returnBook(name);
            } catch (IllegalStateException e) {
                System.out.println(e.getMessage());
                // Якщо не вдалось повернути, студент залишається з книгою (симулюємо цю ситуацію)
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Студент " + name + " був перерваний.");
        }
    }
}

/**
 * У основному класі виконуємо запуск потоків студентів, імітуємо робочий час бібліотеки, а потім закриваємо її.
 */
public class StudentLibrary {
    public static void main(String[] args) {
        // Кількість копій книги
        int copies = 3;
        // Кількість студентів
        int studentCount = 10;
        // Час роботи бібліотеки (мс)
        long openTime = 8000;

        Library library = new Library(copies);
        List<Thread> studentThreads = new ArrayList<>();

        // Створюємо потоки-студенти
        for (int i = 1; i <= studentCount; i++) {
            Thread t = new Thread(new Student("Студент №" + i, library));
            studentThreads.add(t);
            t.start();
        }

        // Даємо можливість бібліотеці працювати певний час
        try {
            Thread.sleep(openTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Головний потік перервано під час роботи.");
        }

        // Закриваємо бібліотеку
        library.closeLibrary();

        // Чекаємо завершення усіх потоків (якщо вони ще працюють)
        for (Thread t : studentThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                System.err.println("Не вдалося дочекатися завершення потоку " + t.getName());
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Усі студенти завершили свої спроби взяти або повернути книги.");
    }
}
