class Animal {
    String name;
    Animal(String name) { this.name = name; }
}

class Dog extends Animal {
    Dog(String name) { super(name); }
    void bark() { System.out.println(name + " гавкает!"); }
}

class Cat extends Animal {
    Cat(String name) { super(name); }
    void meow() { System.out.println(name + " мяукает!"); }
}
