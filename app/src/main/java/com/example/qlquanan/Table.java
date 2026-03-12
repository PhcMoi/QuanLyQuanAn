package com.example.qlquanan;

public class Table {
private int id;
private String name;
private boolean isOccupied;

public Table(int id, String name, boolean isOccupied) {
    this.id = id;
    this.name = name;
    this.isOccupied = isOccupied;
}

public int getId() { return id; }
public String getName() { return name; }
public boolean isOccupied() { return isOccupied; }
public void setOccupied(boolean occupied) { isOccupied = occupied; }
}
