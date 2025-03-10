# JLay




## Widget

`setSize(float width, float height)`

Parameters can either be a fixed size (number of pixels), the value Widget.GROW or the value Widget.FIT.
GROW means the widget will grow to the available space.
FIT (only valid for container widgets) means the widget will size to fit its children.

`setColor(Color color)`

Set background color.


## Group

This is a container subclass of Widget. A group can have child widgets which are either arranged horizontally or vertically.

`clear()`

Remove all children.

`add(Widget widget)`

Add a child widget.

`setVertical()`
`setHorizontal()`

Sets the direction that the child widgets are layed out. Left to right or top to bottom.

`setPadding(float pad)`

`setPadding(float top, float left, float bottom, float right)`

Add padding to the inside of the group sides.

`setGap(float gap)`

Set space between children


## Stack
This is a collection class where all children are shown overlapping, not next to each other.  The last child added will be shown on top.


## Box
This is a simple rounded rectangle with a background colour and a corner radius.

`setCornerRadius(float radius)`
Set radius in pixels of corners.

"How Clay's UI Layout Algorithm Works" by Nic Barker
https://www.youtube.com/watch?v=by9lQvpvMIc&t=2320s