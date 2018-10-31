## Goals i.e. MAIN scripts
All the various MAIN files which run various simulations, tree searches, control tests, analysis, etc. These are the 
runnable files which use the rest of the library.

#### Packages
1. **cold_start_analysis** - There are internal, hidden state variables inside the Box2D physics engine which runs 
QWOP. This package contains various analyses of the effects of the hidden states.

2. **perturbation_analysis** - Analyzing the sensitivity of open loop QWOP running trajectories to perturbations in 
action and (TBD) in state.

3. **phase_variable_testing** - Trying to find a good way of quantifying the progress through a single step, so steps
 can be compared at the same phase along.
 
4. **playback** - Animating previously-saved runs.
