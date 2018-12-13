## MATLAB interface to QWOP
Gradually adding basic support for running the game in MATLAB.

### What's available

Basically all non-graphical classes can used in MATLAB. This includes TFRecord loading and game simulation. A few 
important ones:
* `GameSingleThread` - basic interface to the QWOP game. Use `stepGame(Q, W, O, P)` to advance the simulation by a 
timestep. Use `getDebugVertices` to return a structure containing collision shape location information for drawing 
purposes. Use `makeNewWorld` to reset the game.
* `Action` and `ActionQueue` can make it easier to put in commands. 
* `State` (containing `StateVariable`) can be returned from the game with `getCurrentState`. Since a hierarchy of 
objects can be painful in MATLAB, you can call `myState.flattenState` to get an array of the 72 numbers contained.

### How it works

Java classes can be used (relatively) easily in MATLAB. The classes need to be in the `javaclasspath` of MATLAB, 
which can be done with `javaaddpath`. These can be `.jar` or `.class` files. Once on the path, specific classes can 
be imported with the `import` command. Note that specific packages must be referenced correctly, e.g. 
`import actions.Action`. After being imported, these classes can be used directly. For example `action = Action(10, 
false, false, false, false);` in MATLAB is equivalent to `Action action = new Action(10, false, false, false, false);
`. If a method returns an object, then you can use it in MATLAB the same as you would in Java. If it returns a 
primitive type or primitive array, MATLAB will convert it into its own primitive types, e.g. Java `float` to MATLAB 
`single`. 

##### Pitfalls
There are some quirks with using Java in MATLAB. To save future pain, here are a few:

1. As of MATLAB 2018b, MATLAB does not support Java versions above 1.8 (Java 8). If you try to import classes 
compiled with a higher (or probably different) version of Java, a super-generic `...cannot be found or cannot be 
imported...` error will occur. For this project, make sure that Maven is building with 1.8 and not 11 as usual.
2. Trying to use `import` and `javaaddpath` inside a function may fail with an unhelpful message. A workaround is to 
use `eval('import foo.Bar);`. Also, creating a separate script to do imports and calling it from within another 
function does not work.
3. (Likely very fixable) Maven does not package jbox2d.jar in with the other dependencies. You'll need to add that to
 the classpath separately.