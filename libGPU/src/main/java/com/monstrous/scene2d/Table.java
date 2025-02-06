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
    int[] colX;
    int[] rowY;

    public Table() {
        cells = new ArrayList<>();
        widgets = new ArrayList<>();            // should widgets be accessed via Cell?
        clear();
    }

    public void clear(){
        widgets.clear();
        cells.clear();
        numCols = 0;
        colNr = 0;
        numRows = 1;
        rowNr = 0;
        // todo dispose?
    }

    public Cell add( Widget widget ){
        Cell cell = addCell();
        widget.setCell(cell);
        widgets.add(widget);

        return cell;
    }

    private Cell addCell(){
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
    public void setPosition(){
        super.setPosition();

        // position all the cells now that we have the table position
        for(Cell cell : cells) {
            // note y goes up, but row numbers go down
            // the position of a cell is for the bottom left corner
            //
            cell.setPosition(parentCell.x + x + colX[cell.col], parentCell.y + y + rowY[cell.row] - cell.h);
        }

        // position each widget within its cell depending e.g. on its alignment and padding values
        for(Widget widget : widgets) {
            widget.setStage(getStage());   // propagate downwards so that each widget can find the stage
            widget.setPosition();
        }
    }

    @Override
    public void pack(){
        if(numCols == 0 || numRows == 0)    // empty table, don't divide by zero
            return;

        for(Widget widget : widgets) {
            widget.pack();              // and recurse down
        }

        int[] colWidths = new int[numCols];
        colX = new int[numCols];
        int totalWidth = 0;
        for(int col = 0; col < numCols; col++){
            colWidths[col] = getPreferredColWidth(col);
            totalWidth += colWidths[col];
        }
        if(fillParent)
            this.w = parentCell.w;
        else
            this.w = totalWidth;

        // distribute the remainder evenly over all columns
        // to add: expand
        int remainder = w - totalWidth;

        int extra = remainder/numCols;
        colX[0] = 0;
        for(int col = 0; col < numCols; col++){
            colWidths[col] += extra;
            if(col > 0)
                colX[col] = colX[col-1] + colWidths[col-1];
            setColumnWidth(col, colWidths[col]);
        }

        int[] rowHeights = new int[numRows];

        int totalHeight = 0;
        for(int row = 0; row < numRows; row++){
            rowHeights[row] = getPreferredRowHeight(row);
            totalHeight += rowHeights[row];
        }
        if(fillParent)
            this.h = parentCell.h;
        else
            this.h = totalHeight;

        remainder = h - totalHeight;
        int extraHeight = remainder/numRows;
        rowY = new int[numRows];
        rowY[0] = h;
        for(int row = 0; row < numRows; row++){
            rowHeights[row] += extraHeight;
            if(row > 0)
                rowY[row] = rowY[row-1] - rowHeights[row-1];
            setRowHeight(row, rowHeights[row]);
        }
    }

    private int getPreferredColWidth(int col){
        //get maximum preferred width of all cells in this column
        int max = 0;
        for(Widget widget : widgets) {
            if(widget.parentCell.col == col) {
                int padded = widget.w + widget.parentCell.padLeft + +widget.parentCell.padRight;
                int width = Math.max(parentCell.w, padded);
                if (width > max)
                    max = width;
            }
        }
        return max;
    }

    private void setColumnWidth(int col, int width){
        // set width of all cells in this column
        for(Cell cell : cells){
            if(cell.col == col)
                cell.setWidth(width);
        }
    }
    private int getPreferredRowHeight(int row){
        int max = 0;
        for(Widget widget : widgets) {
            if(widget.parentCell.row == row) {
                int padded = widget.h+ widget.parentCell.padTop + +widget.parentCell.padBottom;
                int height = Math.max(0, padded);
                if (height > max)
                    max = height;
            }
        }
        return max;
    }

    private void setRowHeight(int row, int height){
        // set width of all cells in this column
        for(Cell cell : cells){
            if(cell.row == row)
                cell.setHeight(height);
        }
    }


    @Override
    public Widget hit(float mx, float my){
        if(mx < x+parentCell.x || my < y+parentCell.y || mx > x+parentCell.x+w || my >  y+parentCell.y+h)
            return null;
        for(Widget widget : widgets){
            Widget found = widget.hit(mx, my);
            if(found != null)
                return found;
        }
        return null;
    }

    public void draw(SpriteBatch batch){
        for(Widget widget : widgets)
            widget.draw(batch);
    }

    @Override
    public void debugDraw(ShapeRenderer sr){
        // draw cell outlines
        sr.setColor(debugCellColor);
        sr.setLineWidth(1f);
        for(Cell cell : cells)
            sr.box(cell.x, cell.y, cell.x+cell.w, cell.y+cell.h);

        // now recursively debugDraw the child widgets
        for(Widget widget : widgets)
            widget.debugDraw(sr);
    }
}
