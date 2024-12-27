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
        w = parentCell.w;   // fill parent by default (for now)
        h = parentCell.h;
        for(Cell cell : cells){
            cell.setSize(colWidth, rowHeight);
            // note y goes up, but row numbers go down
            // the position of a cell is for the bottom left corner
            cell.setPosition(cell.col * colWidth, ((numRows-1)-cell.row) * rowHeight);
        }
        for(Widget widget : widgets) {
            widget.pack();
            widget.setPosition();
        }
    }



    @Override
    public Widget hit(float mx, float my, float xoff, float yoff){
        if(mx < x+parentCell.x+xoff || my < y+parentCell.y+yoff || mx > x+parentCell.x+w+xoff || my >  y+parentCell.y+h+yoff)
            return null;
        for(Widget widget : widgets){
            Widget found = widget.hit(mx, my, parentCell.x+xoff, parentCell.y+yoff);
            if(found != null)
                return found;
        }
        return null;
    }

    public void draw(SpriteBatch batch, int xoffset, int yoffset){
        for(Widget widget : widgets)
            widget.draw(batch, xoffset+parentCell.x, yoffset+parentCell.y);
    }

    @Override
    public void debugDraw(ShapeRenderer sr, int xoffset, int yoffset){
        sr.setColor(debugCellColor);
        sr.setLineWidth(1f);
        for(Cell cell : cells)
            sr.box(xoffset+cell.x+parentCell.x, xoffset+cell.y+parentCell.y, cell.x+cell.w, cell.y+cell.h);
        for(Widget widget : widgets)
            widget.debugDraw(sr, xoffset+parentCell.x, yoffset+parentCell.y);
    }
}
