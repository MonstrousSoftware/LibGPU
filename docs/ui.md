
Layout is done with Cells and Widgets.  A Cell can have zero or one Widgets inside.
The padding and alignment attributes of the Cell are used to position the widget inside the Cell.

A Table contains a 2d matrix of Cells. Cells in the same column have the same width and Cells in the same row
have the same height.

Table#pack() calculates the size of each cell based on the preferred size of each cell.

Table size is determined by its content. (apart from tables where we use the fill parent option).  This means table size must be determined bottom up.
Table itself can also be padded within its parent cell.