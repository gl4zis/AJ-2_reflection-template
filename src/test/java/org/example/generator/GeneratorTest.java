package org.example.generator;

import org.example.classes.BinaryTreeNode;
import org.example.classes.Cart;
import org.example.classes.Example;
import org.example.classes.Product;
import org.example.classes.Rectangle;
import org.example.classes.Shape;
import org.example.classes.Triangle;
import org.example.fixtures.Fixtures;
import org.example.fixtures.Fixtures.Animal;
import org.example.fixtures.Fixtures.Box;
import org.example.fixtures.Fixtures.Broken;
import org.example.fixtures.Fixtures.Cat;
import org.example.fixtures.Fixtures.Color;
import org.example.fixtures.Fixtures.Dog;
import org.example.fixtures.Fixtures.EmptyEnum;
import org.example.fixtures.Fixtures.EvenNumber;
import org.example.fixtures.Fixtures.Holder;
import org.example.fixtures.Fixtures.Impossible;
import org.example.fixtures.Fixtures.NoPublicConstructor;
import org.example.fixtures.Fixtures.NotAnnotated;
import org.example.fixtures.Fixtures.Orphan;
import org.example.fixtures.Fixtures.Point;
import org.example.fixtures.Fixtures.TwoConstructors;
import org.example.fixtures.Fixtures.Zoo;
import org.example.fixtures.extra.Parrot;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class GeneratorTest {
    private static final int RUNS = 200;

    private final Generator generator = new Generator();

    private <T> List<T> many(Supplier<T> supplier) {
        return Stream.generate(supplier).limit(RUNS).toList();
    }

    @Nested
    class SimpleTypes {
        static Stream<Arguments> primitivesAndWrappers() {
            return Stream.of(
                    arguments(int.class, Integer.class), arguments(Integer.class, Integer.class),
                    arguments(long.class, Long.class), arguments(Long.class, Long.class),
                    arguments(short.class, Short.class), arguments(Short.class, Short.class),
                    arguments(byte.class, Byte.class), arguments(Byte.class, Byte.class),
                    arguments(double.class, Double.class), arguments(Double.class, Double.class),
                    arguments(float.class, Float.class), arguments(Float.class, Float.class),
                    arguments(boolean.class, Boolean.class), arguments(Boolean.class, Boolean.class),
                    arguments(char.class, Character.class), arguments(Character.class, Character.class));
        }

        @ParameterizedTest
        @MethodSource("primitivesAndWrappers")
        void primitiveOrWrapperGivesBoxedValue(Class<?> requested, Class<?> expected) {
            assertInstanceOf(expected, generator.generateValueOfType(requested));
        }

        @ParameterizedTest
        @ValueSource(classes = {int.class, double.class, boolean.class, char.class, String.class})
        void valuesAreRandomNotConstant(Class<?> type) {
            Set<Object> distinct = new HashSet<>(many(() -> generator.generateValueOfType(type)));
            assertTrue(distinct.size() > 1, "Генератор всё время возвращает одно и то же: " + distinct);
        }

        @Test
        void stringIsNonEmptyAndBounded() {
            for (String s : many(() -> generator.generateValueOfType(String.class))) {
                assertFalse(s.isEmpty());
                assertTrue(s.length() <= Generator.MAX_STRING_LENGTH);
            }
        }

        @Test
        void enumGivesEveryConstantEventually() {
            assertEquals(Set.of(Color.values()), new HashSet<>(many(() -> generator.generateValueOfType(Color.class))));
        }

        @Test
        void typedApiWorksWithoutCasts() {
            Integer number = generator.generateValueOfType(Integer.class);
            String text = generator.generateValueOfType(String.class);
            assertNotNull(number);
            assertNotNull(text);
        }
    }

    @Nested
    class ArraysAndCollections {
        @Test
        void primitiveArray() {
            int[] numbers = generator.generateValueOfType(int[].class);
            assertSizeInRange(numbers.length);
        }

        @Test
        void stringEnumAndObjectArrays() {
            String[] strings = generator.generateValueOfType(String[].class);
            Color[] colors = generator.generateValueOfType(Color[].class);
            Rectangle[] rectangles = generator.generateValueOfType(Rectangle[].class);
            Shape[] shapes = generator.generateValueOfType(Shape[].class);
            for (Object[] array : List.of(strings, colors, rectangles, shapes)) {
                assertSizeInRange(array.length);
                for (Object element : array) {
                    assertNotNull(element);
                }
            }
        }

        @Test
        void twoDimensionalArray() {
            int[][] matrix = generator.generateValueOfType(int[][].class);
            assertSizeInRange(matrix.length);
            for (int[] row : matrix) {
                assertSizeInRange(row.length);
            }
        }

        @Test
        void rawListIsNonEmptyListOfIntegers() {
            List<?> items = generator.generateValueOfType(List.class);
            assertSizeInRange(items.size());
            items.forEach(item -> assertInstanceOf(Integer.class, item));
        }

        @Test
        void rawCollectionIsAlsoSupported() {
            assertSizeInRange(generator.generateValueOfType(Collection.class).size());
        }

        @Test
        void rawMapIsNonEmptyMapOfStringToInteger() {
            Map<?, ?> entries = generator.generateValueOfType(Map.class);
            assertFalse(entries.isEmpty());
            assertTrue(entries.size() <= Generator.MAX_SIZE);
            entries.forEach((k, v) -> {
                assertInstanceOf(String.class, k);
                assertInstanceOf(Integer.class, v);
            });
        }

        @Test
        void genericElementTypesComeFromConstructorSignature() {
            Cart cart = generator.generateValueOfType(Cart.class);
            assertSizeInRange(cart.getItems().size());
            for (Object item : cart.getItems()) {
                Product product = assertInstanceOf(Product.class, item);
                assertFalse(product.getName().isEmpty());
            }
        }
    }

    @Nested
    class UserObjects {
        @Test
        void templateClasses() {
            assertInstanceOf(Example.class, generator.generateValueOfType(Example.class));
            assertTrue(generator.generateValueOfType(Rectangle.class).getArea() > 0);
            assertTrue(generator.generateValueOfType(Triangle.class).getPerimeter() > 0);
        }

        @Test
        void annotatedConstructorIsPreferred() {
            for (TwoConstructors obj : many(() -> generator.generateValueOfType(TwoConstructors.class))) {
                assertEquals("annotated", obj.getUsedConstructor());
            }

            for (Product product : many(() -> generator.generateValueOfType(Product.class))) {
                assertTrue(product.getPrice() != Double.MIN_VALUE);
            }
        }

        @Test
        void recursiveStructureIsFiniteAndNonTrivial() {
            BinaryTreeNode root = generator.generateValueOfType(BinaryTreeNode.class);
            assertNotNull(root);
            assertEquals(Generator.MAX_DEPTH + 1, height(root));
        }

        @Test
        void recordIsSupported() {
            assertInstanceOf(Point.class, generator.generateValueOfType(Point.class));
        }

        @Test
        void newComplexClassWithoutSpecialSupport() {
            Zoo zoo = generator.generateValueOfType(Zoo.class);
            assertFalse(zoo.getTitle().isEmpty());
            assertSizeInRange(zoo.getAnimals().size());
            zoo.getAnimals().forEach(a -> assertInstanceOf(Animal.class, a));
            assertFalse(zoo.getPlaces().isEmpty());
            zoo.getPlaces().forEach((k, v) -> {
                assertInstanceOf(Color.class, k);
                assertInstanceOf(Point.class, v);
            });
            assertSizeInRange(zoo.getPalette().length);
        }

        private int height(BinaryTreeNode node) {
            return node == null ? 0 : 1 + Math.max(height(node.getLeft()), height(node.getRight()));
        }
    }

    @Nested
    class AbstractTypes {
        @Test
        void shapeGivesEveryAnnotatedImplementation() {
            Set<Class<?>> kinds = new HashSet<>();
            for (int i = 0; i < RUNS; i++) {
                Shape shape = generator.generateValueOfType(Shape.class);
                assertInstanceOf(Shape.class, shape);
                kinds.add(shape.getClass());
            }
            assertEquals(Set.of(Rectangle.class, Triangle.class), kinds);
        }

        @Test
        void abstractClassUsesOnlyAnnotatedImplementationsIncludingSubpackages() {
            Set<Class<?>> kinds = new HashSet<>();
            for (int i = 0; i < RUNS; i++) {
                kinds.add(generator.generateValueOfType(Animal.class).getClass());
            }

            // No Wolf
            assertEquals(Set.of(Dog.class, Cat.class, Parrot.class), kinds);
        }

        @Test
        void interfaceWithoutImplementationsFails() {
            GenerationException e = assertThrows(GenerationException.class,
                    () -> generator.generateValueOfType(Orphan.class));
            assertTrue(e.getMessage().contains("не найдено"), e.getMessage());
        }
    }

    @Nested
    class DomainConstraints {
        @Test
        void rectangleSidesArePositive() {
            for (Rectangle r : many(() -> generator.generateValueOfType(Rectangle.class))) {
                assertTrue(r.getArea() > 0);
            }
        }

        @Test
        void triangleSidesSatisfyTriangleInequality() {
            for (Triangle t : many(() -> generator.generateValueOfType(Triangle.class))) {
                double area = t.getArea();
                assertFalse(Double.isNaN(area));
                assertTrue(area > 0);
            }
        }

        @Test
        void constraintsOfNewClassesAreRespectedToo() {
            for (EvenNumber n : many(() -> generator.generateValueOfType(EvenNumber.class))) {
                assertEquals(0, n.getValue() % 2);
            }
        }

        @Test
        void unsatisfiableConstraintFailsAfterAttemptLimit() {
            GenerationException e = assertThrows(GenerationException.class,
                    () -> generator.generateValueOfType(Impossible.class));
            assertTrue(e.getMessage().contains(String.valueOf(Generator.MAX_ATTEMPTS)), e.getMessage());
            assertInstanceOf(IllegalArgumentException.class, e.getCause());
        }
    }

    @Nested
    class Failures {
        @Test
        void innerNonStaticClassIsRejected() {
            GenerationException e = assertThrows(GenerationException.class,
                    () -> generator.generateValueOfType(Fixtures.InnerSample.class));
            assertTrue(e.getMessage().contains("не static"), e.getMessage());
        }

        @Test
        void arrayDimensionsAreLimited() {
            assertNotNull(generator.generateValueOfType(int[][][][][].class));
            GenerationException e = assertThrows(GenerationException.class,
                    () -> generator.generateValueOfType(int[][][][][][].class));
            assertTrue(e.getMessage().contains(String.valueOf(Generator.MAX_ARRAY_DIMENSIONS)), e.getMessage());
        }

        @Test
        void classWithoutAnnotationIsRejected() {
            GenerationException e = assertThrows(GenerationException.class,
                    () -> generator.generateValueOfType(NotAnnotated.class));
            assertTrue(e.getMessage().contains("@Generatable"), e.getMessage());
        }

        @Test
        void jdkClassWithoutSupportIsRejected() {
            assertThrows(GenerationException.class, () -> generator.generateValueOfType(Thread.class));
        }

        @Test
        void messageShowsWhereNestedGenerationFailed() {
            GenerationException e = assertThrows(GenerationException.class,
                    () -> generator.generateValueOfType(Holder.class));
            assertTrue(e.getMessage().contains(Holder.class.getName()), e.getMessage());
            assertTrue(e.getMessage().contains(NotAnnotated.class.getName()), e.getMessage());
        }

        @Test
        void constructorFailureIsWrappedWithCause() {
            GenerationException e = assertThrows(GenerationException.class,
                    () -> generator.generateValueOfType(Broken.class));
            assertInstanceOf(IllegalStateException.class, e.getCause());
        }

        @Test
        void unknownGenericTypeIsRejected() {
            assertThrows(GenerationException.class, () -> generator.generateValueOfType(Box.class));
            assertThrows(GenerationException.class, () -> generator.generateValueOfType(Object.class));
        }

        @ParameterizedTest
        @ValueSource(classes = {void.class, Void.class, EmptyEnum.class, NoPublicConstructor.class})
        void impossibleRequestsFailWithMeaningfulException(Class<?> type) {
            GenerationException e = assertThrows(GenerationException.class, () -> generator.generateValueOfType(type));
            assertFalse(e.getMessage().isBlank());
        }

        @Test
        void nullClassIsRejected() {
            assertThrows(NullPointerException.class, () -> generator.generateValueOfType(null));
        }
    }

    private static void assertSizeInRange(int size) {
        assertTrue(IntStream.rangeClosed(Generator.MIN_SIZE, Generator.MAX_SIZE).anyMatch(s -> s == size),
                "Размер вне диапазона: " + size);
    }
}
