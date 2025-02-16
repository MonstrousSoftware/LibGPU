# Application chaining

It is possible to run a number of applications in sequence by setting the next ApplicationListener to use after
the current application is exited.  

The second parameter can be set to true to make the restart the current application after the new application was exited.

        ApplicationListener next = new TestApp();

        LibGPU.app.setNextListener(next, true);
        LibGPU.app.exit();



Note: This does not work if the user closes the window is closed.

Note 2: The use of `Game` and `Screen` classes may be preferable if you have an application consisting of multiple screens, but perhaps sharing data.