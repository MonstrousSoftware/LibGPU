
## Application Lifecycle


### create()
Called on application start-up    

### resize(int w, int h)
Called on application start-up (after create) and on resize.
Is not called on minimize.

### render()
Called once per frame while application is running.
Not called when minimized.

### pause() 
Called on window minimize and on application exit (before dispose()).

### resume()
Called on restoring a minimized window.

### dispose()
Called on application exit.