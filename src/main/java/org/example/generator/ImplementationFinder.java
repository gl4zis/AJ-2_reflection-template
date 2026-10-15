package org.example.generator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

final class ImplementationFinder {

    private ImplementationFinder() { }

    static List<Class<?>> find(Class<?> base) {
        ClassLoader loader = base.getClassLoader();
        String packagePath = base.getPackageName().replace('.', '/');
        Set<Class<?>> result = new LinkedHashSet<>();
        try {
            for (URL root : Collections.list(loader.getResources(packagePath))) {
                for (String className : classNames(root, packagePath)) {
                    Class<?> candidate = load(className, loader);
                    if (candidate != null
                            && base.isAssignableFrom(candidate)
                            && !Modifier.isAbstract(candidate.getModifiers())
                            && candidate.isAnnotationPresent(Generatable.class)) {
                        result.add(candidate);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось просканировать пакет " + base.getPackageName(), e);
        }
        return new ArrayList<>(result);
    }

    private static List<String> classNames(URL root, String packagePath) throws IOException {
        return switch (root.getProtocol()) {
            case "file" -> fromDirectory(root, packagePath);
            case "jar" -> fromJar(root, packagePath);
            default -> List.of();
        };
    }

    private static List<String> fromDirectory(URL root, String packagePath) throws IOException {
        Path dir;
        try {
            dir = Path.of(root.toURI());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        try (Stream<Path> files = Files.walk(dir)) {
            return files
                    .filter(p -> p.toString().endsWith(".class"))
                    .map(p -> packagePath + "/" + dir.relativize(p).toString().replace('\\', '/'))
                    .map(ImplementationFinder::toClassName)
                    .toList();
        }
    }

    private static List<String> fromJar(URL root, String packagePath) throws IOException {
        JarURLConnection connection = (JarURLConnection) root.openConnection();
        connection.setUseCaches(false);
        try (JarFile jar = connection.getJarFile()) {
            return jar.stream()
                    .map(JarEntry::getName)
                    .filter(name -> name.startsWith(packagePath + "/") && name.endsWith(".class"))
                    .map(ImplementationFinder::toClassName)
                    .toList();
        }
    }

    private static String toClassName(String resourcePath) {
        String name = resourcePath.substring(0, resourcePath.length() - ".class".length()).replace('/', '.');
        return name.startsWith(".") ? name.substring(1) : name;
    }

    private static Class<?> load(String className, ClassLoader loader) {
        try {
            return Class.forName(className, false, loader);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }
}
