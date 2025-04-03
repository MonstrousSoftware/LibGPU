
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



# Application chaining

It is also possible to run a number of applications in sequence by setting the next ApplicationListener to use after
the current application is exited.

        ApplicationListener next = new TestApp();

        LibGPU.app.setNextListener(next, false);
        LibGPU.app.exit();      // switch to new application listener

The second parameter can be set to true to restart the current application after the new application was exited. 


Note: This does not work if the user closes the window.

Note 2: The use of `Game` and `Screen` classes may be preferable if you have an application consisting of multiple screens, but perhaps sharing data.