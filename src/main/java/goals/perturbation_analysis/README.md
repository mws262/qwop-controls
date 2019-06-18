## Run perturbations

For now, this is about perturbing open-loop trajectories to see how quickly they decay. In the future, this will be 
extended to closed-loop also. Action perturbations (e.g. starting game.actions too early, missing a timestep of an action,
 etc.) are implemented via the `IActionPerturber` interface. Perturbations to state and torque/force disturbances can
  be done too through `GameLoader`, and impulse disturbances are used in `MAIN_PerturbationImpulse`.