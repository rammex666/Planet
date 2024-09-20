package fr.rammex.planet.object;

import fr.rammex.planet.utils.Vector;

public class SchematicBlock {

    private final Vector location;

    private final int id;

    private final byte data;

    public SchematicBlock(Vector location, int id, byte data) {
        this.location = location;
        this.id = id;
        this.data = data;
    }

    public Vector getLocation() {
        return location;
    }

    public int getID() {
        return id;
    }

    public byte getData() {
        return data;
    }
}