## Run playback

Previously-saved QWOP games can be viewed in several ways here. The data could be stored as a serialized Java object 
e.g. `SavableSingleGame` or as a TFRecord protobuf `SequenceExample`. If the data is saved sparsely, i.e. states are 
not recorded at every timestep, then the game.actions are re-simulated to play back the run. If the data is saved densely,
 i.e. states recorded at every timestep, then the state data is just played-back movie style. The playback of the 
 TFRecord dense data does both to verify that the previously-saved states match the result of re-simulating the game.actions.