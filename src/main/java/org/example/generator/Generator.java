package org.example.generator;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

public class Generator {
    static final int MAX_DEPTH = 4;
    static final int MAX_ATTEMPTS = 100;
    static final int MAX_ARRAY_DIMENSIONS = 5;
    static final int MIN_SIZE = 1;
    static final int MAX_SIZE = 5;
    static final int MAX_STRING_LENGTH = 10;
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final Random random;
    private final Map<Class<?>, Supplier<Object>> simpleValues = new HashMap<>();

    public Generator() {
        this(new Random());
    }

    public Generator(Random random) {
        this.random = Objects.requireNonNull(random, "random");
        register(random::nextBoolean, boolean.class, Boolean.class);
        register(() -> (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE), byte.class, Byte.class);
        register(() -> (short) random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE), short.class, Short.class);
        register(() -> random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE), int.class, Integer.class);
        register(() -> random.nextLong(Long.MIN_VALUE, Long.MAX_VALUE), long.class, Long.class);
        register(() -> random.nextFloat(Float.MIN_VALUE, Float.MAX_VALUE), float.class, Float.class);
        register(() -> random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE), double.class, Double.class);
        register(() -> ALPHABET.charAt(random.nextInt(ALPHABET.length())), char.class, Character.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T generateValueOfType(Class<T> clazz) {
        Objects.requireNonNull(clazz, "clazz");
        return (T) generate(clazz, 0);
    }

    private Object generate(Type type, int depth) {
        type = resolveBound(type);
        if (type == Object.class) {
            throw new GenerationException(
                    "Невозможно определить конкретный тип: запрошен Object (или generic-параметр без ограничения)"
            );
        }
        Class<?> raw = rawClass(type);

        if (raw == void.class || raw == Void.class) {
            throw new GenerationException("У типа void нет значений");
        }
        Supplier<Object> simple = simpleValues.get(raw);
        if (simple != null) {
            return simple.get();
        }
        if (raw == String.class) {
            return randomString();
        }
        if (raw.isEnum()) {
            return enumConstant(raw);
        }
        if (raw.isArray()) {
            int dimensions = 0;
            for (Class<?> c = raw; c.isArray(); c = c.getComponentType()) {
                dimensions++;
            }
            if (dimensions > MAX_ARRAY_DIMENSIONS) {
                throw new GenerationException("Массив " + raw.getTypeName() + " имеет размерность " + dimensions
                        + ", поддерживается не больше " + MAX_ARRAY_DIMENSIONS);
            }
            return array(type, raw, depth);
        }
        if (Collection.class.isAssignableFrom(raw) && raw.isAssignableFrom(ArrayList.class)) {
            return list(type, depth);
        }
        if (Map.class.isAssignableFrom(raw) && raw.isAssignableFrom(LinkedHashMap.class)) {
            return map(type, depth);
        }
        return object(raw, depth);
    }

    private String randomString() {
        int length = random.nextInt(1, MAX_STRING_LENGTH + 1);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private Object enumConstant(Class<?> enumClass) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants.length == 0) {
            throw new GenerationException("Перечисление " + enumClass.getName() + " не содержит констант");
        }
        return constants[random.nextInt(constants.length)];
    }

    private Object array(Type type, Class<?> raw, int depth) {
        Type component = type instanceof GenericArrayType g ? g.getGenericComponentType() : raw.getComponentType();
        int length = randomSize();
        Object array = Array.newInstance(rawClass(resolveBound(component)), length);
        for (int i = 0; i < length; i++) {
            Array.set(array, i, generate(component, depth));
        }
        return array;
    }

    private List<Object> list(Type type, int depth) {
        Type element = typeArgument(type, 0, Integer.class);
        int size = randomSize();
        List<Object> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(generate(element, depth));
        }
        return list;
    }

    private Map<Object, Object> map(Type type, int depth) {
        Type key = typeArgument(type, 0, String.class);
        Type value = typeArgument(type, 1, Integer.class);
        int size = randomSize();
        Map<Object, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(generate(key, depth), generate(value, depth));
        }
        return map;
    }

    private Object object(Class<?> clazz, int depth) {
        if (!clazz.isAnnotationPresent(Generatable.class)) {
            throw new GenerationException("Тип " + clazz.getName()
                    + " не поддерживается: класс должен быть помечен @Generatable");
        }
        if (depth > MAX_DEPTH) {
            return null;
        }
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            List<Class<?>> implementations = ImplementationFinder.find(clazz);
            if (implementations.isEmpty()) {
                throw new GenerationException("Для " + clazz.getName() + " не найдено ни одной "
                        + "реализации с @Generatable в пакете " + clazz.getPackageName() + " и его подпакетах");
            }
            clazz = implementations.get(random.nextInt(implementations.size()));
        }
        return instantiate(clazz, depth);
    }

    private Object instantiate(Class<?> clazz, int depth) {
        if (clazz.getEnclosingClass() != null && !Modifier.isStatic(clazz.getModifiers())) {
            throw new GenerationException("Класс " + clazz.getName() + " — внутренний (не static): "
                    + "для создания нужен объект внешнего класса, такие классы не поддерживаются");
        }
        List<Constructor<?>> constructors = candidateConstructors(clazz);
        IllegalArgumentException lastRejection = null;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Constructor<?> constructor = constructors.get(random.nextInt(constructors.size()));
            Object[] args = arguments(clazz, constructor, depth);
            try {
                return constructor.newInstance(args);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof IllegalArgumentException rejection) {
                    lastRejection = rejection;
                    continue;
                }
                throw new GenerationException("Конструктор " + constructor + " выбросил исключение: " + e.getCause(),
                        e.getCause());
            } catch (ReflectiveOperationException e) {
                throw new GenerationException("Не удалось вызвать конструктор " + constructor, e);
            }
        }
        throw new GenerationException("Не удалось создать " + clazz.getName() + " за " + MAX_ATTEMPTS
                + " попыток: конструктор отклоняет сгенерированные аргументы (" + lastRejection.getMessage() + ")",
                lastRejection);
    }

    private static List<Constructor<?>> candidateConstructors(Class<?> clazz) {
        Constructor<?>[] all = clazz.getConstructors();
        List<Constructor<?>> marked = Arrays.stream(all)
                .filter(c -> c.isAnnotationPresent(Generatable.class))
                .toList();
        if (!marked.isEmpty()) {
            return marked;
        }
        if (all.length == 0) {
            throw new GenerationException("У класса " + clazz.getName() + " нет публичных конструкторов");
        }
        return List.of(all);
    }

    private Object[] arguments(Class<?> clazz, Constructor<?> constructor, int depth) {
        Type[] types = constructor.getGenericParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            try {
                args[i] = generate(types[i], depth + 1);
            } catch (GenerationException e) {
                throw new GenerationException("Не удалось создать " + clazz.getName() + ": параметр #" + i
                        + " (" + types[i].getTypeName() + ") → " + e.getMessage(), e);
            }
        }
        return args;
    }

    private int randomSize() {
        return random.nextInt(MIN_SIZE, MAX_SIZE + 1);
    }

    private static Type typeArgument(Type type, int index, Class<?> fallback) {
        if (type instanceof ParameterizedType parameterized) {
            Type argument = resolveBound(parameterized.getActualTypeArguments()[index]);
            return argument == Object.class ? fallback : argument;
        }

        return fallback;
    }

     // ? extends X -> X
     // T extends X → X
     // ? super X → X
    private static Type resolveBound(Type type) {
        while (true) {
            if (type instanceof WildcardType wildcard) {
                Type[] lower = wildcard.getLowerBounds();
                type = lower.length > 0 ? lower[0] : wildcard.getUpperBounds()[0];
            } else if (type instanceof TypeVariable<?> variable) {
                type = variable.getBounds()[0];
            } else {
                return type;
            }
        }
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterized) {
            return (Class<?>) parameterized.getRawType();
        }
        if (type instanceof GenericArrayType array) {
            // Not more 255 recursive calls
            return rawClass(resolveBound(array.getGenericComponentType())).arrayType();
        }
        throw new GenerationException("Неподдерживаемый вид типа: " + type.getTypeName());
    }

    private void register(Supplier<Object> supplier, Class<?>... types) {
        for (Class<?> type : types) {
            simpleValues.put(type, supplier);
        }
    }
}
