package me.kyren223.echomobile.utils;

public class Label {
    private final String name;
    private final String color;
    public Label(String name, String color) {
        this.name = name;
        this.color = color;
    }
    public String getColor() {
        return color;
    }
    public String getName() {
        return name;
    }
}
