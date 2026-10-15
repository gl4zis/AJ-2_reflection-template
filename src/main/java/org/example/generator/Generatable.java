package org.example.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Разрешение на генерацию пользовательского типа. Смысл зависит от того, где стоит аннотация.
 *
 * <ul>
 *   <li><b>Конкретный класс</b>: генерировать можно только пользовательские классы с {@code @Generatable}.
 *       Запрос класса без аннотации (своего или из JDK, например {@code Thread})
 *       → {@link GenerationException}.</li>
 *   <li><b>Интерфейс / абстрактный класс</b>: аннотация разрешает запрашивать сам тип. Генератор ищет
 *       неабстрактные классы, которые совместимы с ним, помечены {@code @Generatable} и лежат в пакете
 *       базового типа или его подпакетах, и выбирает один случайно. Реализации без аннотации игнорируются.
 *       Если подходящих нет → {@link GenerationException}.</li>
 *   <li><b>Конструктор</b>: если в классе есть конструкторы с {@code @Generatable}, используются только они.
 *       Иначе выбирается случайный публичный конструктор.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR})
public @interface Generatable {
}
