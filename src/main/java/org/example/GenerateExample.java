package org.example;


import org.example.classes.Cart;
import org.example.classes.Example;
import org.example.classes.Shape;
import org.example.generator.Generator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GenerateExample {
    public static void main(String[] args) {
        var gen = new Generator();
        System.out.println(gen.generateValueOfType(Example.class));
        System.out.println(gen.generateValueOfType(String.class));
        System.out.println(Arrays.toString(gen.generateValueOfType(int[].class)));
        System.out.println(gen.generateValueOfType(List.class));
        System.out.println(gen.generateValueOfType(Map.class));

        Shape shape = gen.generateValueOfType(Shape.class);
        System.out.println(shape.getClass().getSimpleName() + ", площадь = " + shape.getArea());

        Cart cart = gen.generateValueOfType(Cart.class);
        cart.getItems().forEach(p -> System.out.println(p.getName() + ": " + p.getPrice()));
    }
}
