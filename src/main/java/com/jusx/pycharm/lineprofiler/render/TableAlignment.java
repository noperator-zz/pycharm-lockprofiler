package com.jusx.pycharm.lineprofiler.render;


/**
 * Object that is shared by renderers
 *
 * With this object a shared alignment can be decided and kept in memory
 */
public class TableAlignment {
    private int x = 0;

    private int maxX;

    public TableAlignment(int maxX) {
        this.maxX = maxX;
    }

    public int align(int withX) {
        if (withX > maxX) {
            // Break alignment because the input X exceeds the max X
            return withX;
        } else if (withX > this.x) {
            this.x = withX;
        }
        return this.x;
    }

    public int getX() {
        return x;
    }
}
