// QWOP hardware receive commands from serial.

int QPin = 8;
int WPin = 9;
int OPin = 10;
int PPin = 11;

int QWOP[] = {0,0,0,0};
int previousQWOP[] = {0,0,0,0};
int lastTransition[] = {0,0,0,0};
int cycleThreshold = 800;
int holdingStrength = 140;

volatile int input = 0;

void setup() {
  pinMode(QPin,OUTPUT);
  pinMode(WPin,OUTPUT);
  pinMode(OPin,OUTPUT);
  pinMode(PPin,OUTPUT);
  // Default PWM is 490hz which is loud and annoying. Make this much bigger.
  TCCR2B=(TCCR2B&248)|1; // Pins 9 and 10
  TCCR1B=(TCCR1B&248)|1; // Pin 11
  TCCR4B=(TCCR4B&248)|1; // Pin 8
  Serial.begin(115200); 
}

void loop() {
  switch(input) {
    case 48: // 0
      QWOP[0] = 0; // None
      QWOP[1] = 0;
      QWOP[2] = 0;
      QWOP[3] = 0;
      break;
    case 49: // 1
      QWOP[0] = 1; // Q
      QWOP[1] = 0;
      QWOP[2] = 0;
      QWOP[3] = 0;
      break;
    case 50: // 2
      QWOP[0] = 0;
      QWOP[1] = 1; // W
      QWOP[2] = 0;
      QWOP[3] = 0;
      break;
    case 51: // 3
      QWOP[0] = 0;
      QWOP[1] = 0;
      QWOP[2] = 1; // O
      QWOP[3] = 0;
      break;
    case 52: // 4
      QWOP[0] = 0;
      QWOP[1] = 0;
      QWOP[2] = 0;
      QWOP[3] = 1; // P
      break;

    case 53: // 5
      QWOP[0] = 1; // QO
      QWOP[1] = 0;
      QWOP[2] = 1;
      QWOP[3] = 0;
      break;
    case 54: // 6
      QWOP[0] = 1; // QP
      QWOP[1] = 0;
      QWOP[2] = 0;
      QWOP[3] = 1;
      break;
    case 55: // 7
      QWOP[0] = 0;
      QWOP[1] = 1; // WO
      QWOP[2] = 1;
      QWOP[3] = 0;
      break;
    case 56: // 8
      QWOP[0] = 0;
      QWOP[1] = 1; // WP
      QWOP[2] = 0;
      QWOP[3] = 1;
      break;
  }

  if (QWOP[0] != previousQWOP[0]){
    if (QWOP[0] == 1){
        analogWrite(QPin, 255);
    }else if(QWOP[0] == 0){
        analogWrite(QPin, 0);
    }
    previousQWOP[0] = QWOP[0];
    lastTransition[0] = 0;
  } else {
    if (lastTransition[0] < cycleThreshold) {
      lastTransition[0]++;
    } else {
       if (QWOP[0] == 1){
        analogWrite(QPin, holdingStrength);
      }else if(QWOP[0] == 0){
        analogWrite(QPin, 0);
      }
    }
  }
  
  if (QWOP[1] != previousQWOP[1]){
    if (QWOP[1] == 1){
        digitalWrite(WPin, HIGH);
    }else if(QWOP[1] == 0){
        digitalWrite(WPin, LOW);
    }
    previousQWOP[1] = QWOP[1];
    lastTransition[1] = 0;
  } else {
    if (lastTransition[1] < cycleThreshold) {
      lastTransition[1]++;
    } else {
       if (QWOP[1] == 1){
        analogWrite(WPin, holdingStrength);
      }else if(QWOP[1] == 0){
        analogWrite(WPin, 0);
      }
    }
  }
  
  if (QWOP[2] != previousQWOP[2]){
    if (QWOP[2] == 1){
        digitalWrite(OPin, HIGH);
    }else if(QWOP[2] == 0){
        digitalWrite(OPin, LOW);
    }
    previousQWOP[2] = QWOP[2];
    lastTransition[2] = 0;
  } else {
    if (lastTransition[2] < cycleThreshold) {
      lastTransition[2]++;
    } else {
       if (QWOP[2] == 1){
        analogWrite(OPin, holdingStrength);
      }else if(QWOP[2] == 0){
        analogWrite(OPin, 0);
      }
    }
  }
  
  if (QWOP[3] != previousQWOP[3]){
    if (QWOP[3] == 1){
        digitalWrite(PPin, HIGH);
    }else if(QWOP[3] == 0){
        digitalWrite(PPin, LOW);
    }
    previousQWOP[3] = QWOP[3];
    lastTransition[3] = 0;
   } else {
    if (lastTransition[3] < cycleThreshold) {
      lastTransition[3]++;
    } else {
       if (QWOP[3] == 1){
        analogWrite(PPin, holdingStrength);
      }else if(QWOP[3] == 0){
        analogWrite(PPin, 0);
      }
    }
  }
}

void serialEvent() {
  while (Serial.available()) {
    // get the new byte:
    input = Serial.read(); 
  }
}

