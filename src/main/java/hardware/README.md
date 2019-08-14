## Hardware key pusher control
For interfacing with the keyboard key-pushing robot. Note that it is faster and easier to send key presses in 
software. However, it's a better spectacle to have a robot do it, and it demonstrates the controller's robustness 
against time delays.

#### About the key-presser robot
[add picture]
#####Mechanical design and tuning
The robot consists of 4 small solenoids positioned above the q, w, o, and p keys of the keyboard. The solenoids are 
mounted through a plexiglass sheet with height-adjustable standoffs in the corners. The standoffs connect rigidly to 
a board that acts as the base. The unaltered computer keyboard sits on top of the base, with the solenoid plungers 
resting lightly on the keys from above. The exact adjustment of the solenoid height is the key to having quick 
key presses. 

If the solenoid is too high, then the plunger may bottom out before the key is pressed enough to 
activate. A symptom of a too-high solenoid is fast, but unreliable keypresses. If the solenoid is too low, then the 
plunger will rest on the unpressed key with the top of the plunger sticking out of the top of the coil too far. Only 
a small part of the plunger will be in alignment with the electromagnetic coil, and the force exerted will be lower. 
Also, the keyboard key will probably bottom-out (often LOUDLY -- can't be good for the keyboard). The symptoms of a 
too-low solenoid are consistent, but sluggish key-presses and stronger impact sounds as the keys bottom-out.

The solenoid pulls the plunger down when current passes through the coil. When the current flow stops, the plunger is
 pushed back up by the keyboard key (and its internal spring). I previously put my own return springs on the plunger, 
 but I didn't manage to tune these enough to get a real speed improvement. The press and release times are already very 
 close to each other, so it may be that the keyboard springs are just about right on their own. Initially, the 
 plunger had no hard stop in the upward direction. When the current to the solenoid was switched off, the plunger 
 would sometimes fly partway out of the coil housing. If the key needed to reactivate before the plunger had a chance
  to settle down, that next keypress would be slower. I added hard stops by adding screws through the heatsink above 
  each plunger. Potentially, the height of these screws could use some adjustment. They were only eyeballed.  

#####Electrical design and tuning
  The solenoids are switched on and off by bipolar junction transistors (BJTs) that are activated by an Arduino Due. 
  I may switch to some beefy MOSFETs acquired since then. The solenoids are rated at 12-24v; the solenoids appear 
  identical from the outside, but are marked with different voltage ratings. The rating differences were not an 
  intentional choice, but rather the result of dumpster-dive components. Either way, the input voltage seems VERY 
  flexible as long as the heat can be dissipated. I bought heatsinks that were intended to go around small laser 
  modules. After increasing the bore of the hole, the solenoids fit snugly. With heatsinks and a desk fan pointed 
  their way, I have increased the input voltage as high as 40V without issue (TODO hook a scope up and see if that is
   indeed what the solenoids are seeing). Cranking the voltage past the solenoid ratings increases the keypress speed
    substantially. Experimentally, there are diminishing returns though, and I didn't see substantial benefit above 
    24V. Some kind of saturation? When I took the demo setup to Dynamic Walking 2019, I stuck to a fixed-voltage 24VDC 
    power supply. The contraption ran for over an hour non-stop, so it seems to be handling the heat ok. The 
    controller helps a little with this though (see below).
   
#####Control from the microcontroller side
The transistor bases (the switching current inputs) connect to PWM enabled pins on an Arduino Due. The 
microcontroller listens for serial commands to tell it which keys to press. 0-9 tell it which of the valid key 
combinations to press. The Due was selected for its "native USB port." On most Arduinos, you select a baud rate, 
usually up to 115,200. Above this, the connection starts getting less reliable. People have all sorts of tricks 
that can raise the max rate maybe as high as 2Mbps. With the native interface, supposedly you can get up to 4.8 Gbps. 
Considering how few characters are being sent, I can't imagine that the serial speed has a substantial effect. 
Honestly, $10 for a <del>new toy</del> Due felt like a steal. 
 
One feature of solenoids is that it takes higher current to move the plunger than it does to hold it. So, when the 
command to push a button is received, the microcontroller sends a constant high logic level to the transistor. After 
a few milliseconds (?? How long ??), the microcontroller changes to a pulse-width modulated (PWM) signal at about a 
60% duty cycle. This small tweak makes heat dissipation requirements lower. 

TODO another thing to look into: I'd imagine that having the holding current lower would make the solenoid release 
faster. Due to inductance, the magnetic field won't instantly go away. If the current is lower in the first place, 
then it shouldn't take as long for the magnetic field to get low enough for the plunger to release up.

####This code, and from the computer side

* `KeypusherSerialConnection` attempts to identify the correct COM port and open the serial connection the the 
microcontroller. Having multiple Arduinos connected to the computer might confuse it. The serial connection uses the 
`jSerialComm` library. Once successfully connected, `KeypusherSerialConnection` acts as a `IGameCommandTarget`, a 
sink for QWOP commands just like some of the other game implementations. This is done so hardware, Flash game, and 
simulation can be swapped out as seamlessly as possible.
* `TimeTester` tests the press and release times of the keys. It measures the "back-and-forth" time, from serial 
command send to keystroke received. Hence, it is measuring the full latency of the hardware.

##### About the hardware latency
I have gotten the total hardware latency down to about 15-20ms consistently. I think, with some improvement, I could 
reduce it to about 10ms. Before several improvements, the latency was 40-50ms.
It's important to note that this latency is more than just the solenoid actuation time. The serial communication time, the solenoid 
activation time, the keyboard's debouncing filter time, and the operating system's polling rate all factor into this 
latency. 
* TODO *Serial communcation time* -- With the native USB port, the communication time should be very fast. However, 
we're sending very small units of information. It could be high-bandwidth, but with some fixed latency tacked on. 
Also, the libraries that handle the communication could have various inefficiencies. I need to test this.
* TODO *Solenoid activation time* -- DO SOME MAGNETISM CALCULATIONS. 
* TODO *Keyboard debounce filter time* -- When a button is pressed, it may seem like one uniform motion, but for many
 buttons, there is a small "bounce" when the switch bottoms out. This can cause the button to activate several times 
 in short succession. The standard solution is to filter the button presses in one way or another. The filtering gets
  rid of the noise in the button activations, but introduces a time delay. Contrary to what I thought, this debounce 
  filtering is done by a controller on the keyboard itself, not by the operating system. Hence, the manufacturer sets
   this time, and it is typically not possible to change. The keyboard usually introduces a filtering delay between 
   8-20ms, roughly. This is really substantial when game inputs are being polled at 30 Hz! Unfortunately, it's hard 
   to isolate the keyboard's filtering time from other delay sources. I've seen someone hook probes to the contacts 
   on the keyboard switch to measure the initial press time (TODO more on this). Testing a few keyboards in the lab, 
   I found drastic differences in the total keypress latency, while other factors remained largely the same. A 
   modern-looking Apple keyboard was easily 15-20ms slower than an older, beat up Apple model. I ended up buying a 
   gaming keyboard with optical switches, the Wooting One. They claim to be able to get a 1ms response time. So far, 
  I have no reason to believe they are exaggerating. Even as a gamer myself, I find it funny how gaming hardware 
  tries to increase speeds far past the level of human perception. All the same, I understand the draw :)
  
* TODO *Keyboard polling time* -- Everything is just computers all the way down. A controller in the keyboard has to 
check which keys are pressed in order to send a message through the cable to the operating system (and do the 
debounce filtering, as discussed above). A naive controller onboard would have one input pin for each 
button. Every button could be independently checked at the same time. To avoid that many inputs and that many 
independent wires, the keys are multiplexed. [Explain further] This means that the onboard controller can't check all
 the keys at the same time and must cycle through checking them. This introduces some amount of delay based on the 
 speed of the keyboard's controller. [need to look into further] The Wooting keyboard claims to have multiple regions 
 that are multiplexed so it can check certain parts of the keyboard at the same time speeding this polling up.
* TODO *Operating system polling time* -- In yesteryear, keyboard keypresses were registered as interrupts to the 
computer. There were those separate PS/2 ports for keyboards. The time for the operating system to realize 
that a key was pressed was lightning fast. Then USB came along and gave us a single port at the cost of specialization. 
When a USB device is connected, a boatload of information and settings are exchanged about how often this device 
needs to be polled, etc. For keyboards, the operating system can decide how often to poll. This value is something 
like 100-1000 Hz, usually much towards the low end. The defaults I've found tend to range between 100-250 Hz, or 4-10
 ms between polls. When we're trying to reduce a total hardware latency from 20 ms, then even 4 ms is substantial.  
 Unfortunately, operating systems are pretty cagey about providing a way to change the keyboard polling rate. Ubuntu 
 supposedly provided a way in some configuration file, but it seems that the setting was being ignored. Windows 
 provided even less support for changing the polling rate, but it seemed to be higher by default. I need to look into
  this further. 
  
 * Controller latency is related and I need to talk about this more somewhere.
  