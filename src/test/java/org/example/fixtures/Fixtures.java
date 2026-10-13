package org.example.fixtures;

import org.example.generator.Generatable;

import java.util.List;
import java.util.Map;

public final class Fixtures {

    private Fixtures() {}

    public enum Color { RED, GREEN, BLUE }

    public enum EmptyEnum { }

    @Generatable
    public abstract static class Animal {
        private final String name;

        protected Animal(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Generatable
    public static class Dog extends Animal {
        public Dog(String name) {
            super(name);
        }
    }

    @Generatable
    public static class Cat extends Animal {
        private final int lives;

        public Cat(String name, int lives) {
            super(name);
            this.lives = lives;
        }

        public int getLives() {
            return lives;
        }
    }

    public static class Wolf extends Animal {
        public Wolf(String name) {
            super(name);
        }
    }

    @Generatable
    public interface Orphan {
    }

    @Generatable
    public record Point(int x, int y) {
    }

    @Generatable
    public static class Zoo {
        private final String title;
        private final List<Animal> animals;
        private final Map<Color, Point> places;
        private final Color[] palette;

        public Zoo(String title, List<Animal> animals, Map<Color, Point> places, Color[] palette) {
            this.title = title;
            this.animals = animals;
            this.places = places;
            this.palette = palette;
        }

        public String getTitle() {
            return title;
        }

        public List<Animal> getAnimals() {
            return animals;
        }

        public Map<Color, Point> getPlaces() {
            return places;
        }

        public Color[] getPalette() {
            return palette;
        }
    }

    @Generatable
    public static class TwoConstructors {
        private final String usedConstructor;

        @Generatable
        public TwoConstructors(int value) {
            this.usedConstructor = "annotated";
        }

        public TwoConstructors(String value) {
            this.usedConstructor = "plain";
        }

        public String getUsedConstructor() {
            return usedConstructor;
        }
    }

    @Generatable
    public static class EvenNumber {
        private final int value;

        public EvenNumber(int value) {
            if (value % 2 != 0) {
                throw new IllegalArgumentException("Нечётное: " + value);
            }
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static class NotAnnotated {
        public NotAnnotated() {
        }
    }

    @Generatable
    public static class Holder {
        public Holder(NotAnnotated value) {
        }
    }

    @Generatable
    public static class Impossible {
        public Impossible(int value) {
            throw new IllegalArgumentException("Никакое значение не подходит");
        }
    }

    @Generatable
    public static class Broken {
        public Broken() {
            throw new IllegalStateException("Сломано");
        }
    }

    @Generatable
    public static class NoPublicConstructor {
        private NoPublicConstructor() {
        }
    }

    @Generatable
    public static class Box<T> {
        public Box(T value) {
        }
    }

    @Generatable
    public class InnerSample {
        public InnerSample(List<String> items) {
        }
    }
}
