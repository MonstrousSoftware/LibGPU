package com.monstrous.scene2d;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.List;

public class Table extends Widget {
    static public Color debugTableColor = new Color(0, 0, 1, 1);
    static public Color debugCellColor = new Color(1, 0, 0, 1);
    static public Color debugActorColor = new Color(0, 1, 0, 1);

    private int colNr;
    private int rowNr;
    public int numCols, numRows;
    private List<Cell> cells;
    private List<Widget> widgets;

    public Table() {
        colNr = 0;
        rowNr = 0;
        cells = new ArrayList<>();
        widgets = new ArrayList<>();
        numRows = 1;
        numCols = 0;
    }

    public Cell add( Widget widget ){
        Cell cell = addCell();
        widget.setCell(cell);
        widgets.add(widget);
        return cell;
    }

    public Cell addCell(){
        Cell cell = new Cell();
        cell.row = rowNr;
        cell.col = colNr;
        cells.add(cell);
        colNr++;
        if(colNr > numCols)
            numCols = colNr;

        return cell;
    }

    public void row(){
        rowNr++;
        if(rowNr + 1 > numRows)
            numRows = rowNr + 1;
        colNr = 0;
    }

    @Override
    public void pack(){
        // assume same size for all cells...
        int colWidth = parentCell.w / numCols;
        int rowHeight = parentCell.h / numRows;
        for(Cell cell : cells){
            cell.setSize(colWidth, rowHeight);
            cell.setPosition(cell.col * colWidth, ((numRows-1)-cell.row) * rowHeight);
        }
        for(Widget widget : widgets) {
            widget.pack();
            widget.setPosition();
        }
    }

    public void draw(SpriteBatch batch){
        for(Widget widget : widgets)
            widget.draw(batch);
    }

    @Override
    public void debugDraw(ShapeRenderer sr){
        sr.setColor(debugCellColor);
        sr.setLineWidth(1f);
        for(Cell cell : cells)
            sr.box(cell.x, cell.y, cell.x+cell.w, cell.y+cell.h);
        sr.setColor(debugActorColor);
        for(Widget widget : widgets)
            sr.box(widget.x, widget.y, widget.x+widget.w, widget.y+widget.h);
        for(Widget widget : widgets)
            widget.debugDraw(sr);
    }
}
