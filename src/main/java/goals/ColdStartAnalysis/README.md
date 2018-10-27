## How much does the warm-start matter?

#### Background

QWOP's physics engine, Box2D (or [JBox2D](http://www.jbox2d.org/) for my Java version), enforces all constraints with
 impulses. At every timestep, these impulses apply gravity, keep the joints from drifting apart, and keep the feet from 
 sinking through the ground. Box2D has to solve for impulses which simultaneously satisfy all these constraints.

As is true for most games, a reliably fast solution is more important than a super-accurate one (see 
[*anytime 
algorithms*](https://en.wikipedia.org/wiki/Anytime_algorithm)). QWOP's physics 
are 
stepped forward in time with the game's visual updates. We don't want the game's framerate to be slow or have 
occasional spikes. 

Box2D uses an iterative solver, [Projected Gauss-Seidel](https://en.wikipedia.org/wiki/Gauss%E2%80%93Seidel_method), 
to find all the impulses which approximately satisfy physical constraints. QWOP, in particular, only runs a few 
iterations off this solver (Recall that QWOP needed to run in realtime in 2008 Internet Explorer.). The result isn't 
as bad as it might sound. In between two timesteps, the active constraints probably don't change much. The solution 
from the previous timestep can be a great starting point for solving the constraints at the current timestep. This 
use of a previous solution is called *warm-starting* the solver (as opposed to *cold-starting*). Over many timesteps, 
the solver gets close to the 
true solution, and chases it around pretty well, as long as the constraints don't change too rapidly.

Since QWOP's constraint solver only runs a few iterations, its result is *highly dependent* on the solution at the 
previous timestep, which is highly dependent on the timestep before that. And so on. In a sense, these solver 
solutions act like state variables, evolving over time with their own dynamics. These additional "states" are 
particularly annoying since they can't be directly observed on the screen. We could potentially infer these hidden 
solver states from the entire history of previous observable states and actions, but we don't know how the 
hidden states behave, at least not without modelling aspects of Box2D that I don't care to. We also can't set these 
states without hacking Box2D more than I care to.

As a result, if two QWOP runners look like they are in the same state and we apply the same action to both, they will
 only have the same final state if both runners have the same state/action history. When searching for 
 actions, we cannot just arbitrarily set the runner state and assume that the actions we find to locomote will 
 generalize to all ways that the runner ended up in that (observable) state. However, the problem cannot be hopeless.
  Humans can play QWOP (albeit, not very well). It seems reasonable that people might use a small amount of state 
  history to decide when to hit buttons, but it's hard to believe that people have 10 or 20 previous steps memorized.
  
  So, we know that the internal physics engine states have an effect, which can be thought of as a state history 
  dependence. Here, we are trying to figure out how large this effect is, or perhaps, how long is the time horizon of
   this history dependence. Can we completely neglect this effect, or is it vital that we pay attention to the 
   state/action history or try to estimate the hidden state?
 
#### Approaches
1. Find a "good" run using one of the tree searches.
2. Simulate the good run's actions partway.
3. Introduce a new runner at the other's observable state, but with a cold start for the constraint solver. 
4. Continue to simulate both runners with the same actions.
5. See how long, and it what way, the cold-start runner diverges.
6. Investigate when the sensitivity is highest/lowest. When, throughout the gait, are the warm-start's effects most 
significant?

##### Faking warm starts

There is a middle ground between a completely cold-start and a warm-start which is the result of an entire history of
 actions. We can fake a warm start by simulating a cold-start runner in some generic way as an approximation of a 
 full warm start. I can see two approaches:
 1. (easier) Start a new runner. Simulate it with no control actions for 10-100 timesteps. Use this warm-start.
 2. (harder) Start a new runner. Use a previously-generated run to warm-start the runner. Stop the warm-start 
 simulation in the same general part of the gait cycle, or the same contact configuration,  as in the place we want to 
 apply this simulation.

#### Results
TBD. Seems like divergence happens really quickly, maybe 
1.5 - 3 steps.

