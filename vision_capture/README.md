## QWOP frames and state data
Data for training a vision-based QWOP state estimator.

#### About the file structure
Within the vision_capture directory, each individual simulation is given its own subdirectory, labeled `run0`, `run1`, `run2`, etc. Within these subdirectories, each frame has a 640x400 PNG named `ts0.png`, `ts1.png`, etc. The first frame, `ts0.png`, is always the runner at the start line. The last frame is always at the moment of a failure or win condition. 

#### About the data structure
Within each run directory, there is a tab-delimited text file (poses.dat) containing the runner's state at each game frame. Each row contains:
```
[png file name, 36 body positions and angles, 36 body velocities and angular rates]
```
In more detail:
```
[png name, torso x,y,theta, head x,y,theta, RThigh x,y,theta, LThigh x,y,theta, RCalf x,y,theta, LCalf x,y,theta, 
RFoot x,y,theta, LFoot x,y,theta, RUpperArm x,y,theta, LUpperArm x,y,theta, RLowerArm x,y,theta, LLowerArm x,y,theta, ...same but for dx, dy, dtheta]
```
##### An important detail:
Torso x is given in the world frame (somewhere off screen, likely). The x of all other coordinates is relative to the torso x. You will probably want to ignore torso x, since it can't be easily estimated from a single frame. I would like to estimate torso velocity in the x-direction, but that is probably a separate task.

#### About the data generation
All the games are run with the same controller but with noise added to the control signal. Hence, some games fall 
early and some don't fall at all. All games start at the same state. There may be duplicates of some frames early in 
the game. Once the games start to diverge, they should remain different.

#### Notes about the graphics
- Game coordinate to pixel scaling is 1:40
- At failure, a little yellow circle appears at the point of impact. This should only ever be seen in the last frame within a run directory. So, if these are problematic, just throw out the highest numbered `tsX.png` in each run.
- A white line appears on the ground marking the furthest the runner has gone since loading the game.
- The game has a hurdle placed halfway along the track. I don't need to estimate anything about it currently.
- At the end of the track, there is a yellow-ish sand pit.
- The QWOP buttons at the top right and top left of the game get bigger/smaller as the keys are pressed. These should not be relevant to the current pose (may be relevant if you are doing anything predicting the future).
- Current distance traveled is listed at the top of the screen. This distance is scaled from the body's x coordinate (I think it's 1/10th the body x?). It is only to one decimal place, so it probably isn't very useful. On the other hand, the ground texture at the bottom of the game 'moves' and could be useful for estimating forward velocity.