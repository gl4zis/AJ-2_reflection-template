package org.example.fixtures.extra;

import org.example.fixtures.Fixtures;
import org.example.generator.Generatable;

@Generatable
public class Parrot extends Fixtures.Animal {
    public Parrot(String name) {
        super(name);
    }
}
