## Investigating a phase variable
It is convenient to be able to quantify exactly how far the runner is through a given step, e.g. starting at 0 and
finishing the step at 1. We can see how much variance there is between the same part of different steps. We can 
reason about an "average" running gait. We may be able to infer properties of stability.

A good measure of "progress" or phase through one step should have some properties. I'd like a single scalar value, 
monotonically increasing during one step, and preferably linear with time for a "perfect gait." Ideally, it should 
work like a crank on a clockwork version of the perfect runner. You turn the crank (advance the phase variable), and 
all other motions of the runner are slaved to this one value. 

The tricky thing is that steps in QWOP can be wildly different. For some simple walking models, the phase could be the
 angle that the stance leg makes with the ground. Each time a foot hits the ground, the phase resets. In QWOP, it's 
 very common for the trailing foot to drag or bounce a little bit as it swings through. Hence, it's hard to come up 
 with a simple description of the end of one step and the beginning of the next.
 
 #### Some ideas
 
 1. Single state variables, e.g. torso angle relative to the world. All the body angles and heights should be roughly
  cyclical. Which ones are actually good descriptions of the phase through one step? My intuition says torso angle is
   pretty good. When the runner is leaned too far back, he needs to wait until he tilts back forward to continue to 
   the next step. I've noticed in plots of torso angle that it looks a bit like a sine wave whose mean keeps drifting
    around. This might not be so good.
 2. Combinations of state variables, e.g. hip scissor angle. The main issue I see with this candidate is that the 
 scissor angle maxes out for a portion of the step at the joint limits.
 3. "Smart" combinations of state variables:
    1. Principal component analysis (PCA). Could try using the first principal component as a phase variable.
    2. Autoencoders. Using a neural network which reduces the state to one number in such a way that it can be 
    unpacked back into a faithful restoration of the full state.
    
 4. Higher dimensions? Using more than one phase variable, sort of like the loops in phase space that I see in 
 Strogatzy books?
 
 #### Results
 * Torso angle isn't bad, but it drifts around some.
 * The 72 to 1 autoencoder works pretty well, although it isn't monotonically increasing. Each step looks like a 
 steep checkmark plotted. See MAIN_SingleVarAutoencoder for plotting. Also, I don't know offhand how to train an 
 autoencoder to FORCE it to be strictly increasing.
 
 #### TODO
 * Top priority: *find a good test case.*
 * Try PCA.
 * Try hip scissor angle.