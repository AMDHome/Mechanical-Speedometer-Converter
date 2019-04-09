void setup() {
Serial.begin(9600);
 pinMode(LED_BUILTIN, OUTPUT); // put your setup code here, to run once:
 }

void loop() {// put your main code here, to run repeatedly:
 if(Serial.available()>0)
   {     
      char data= Serial.read(); // reading the data received from the bluetooth module
      switch(data)
      {
        case 'a': digitalWrite(LED_BUILTIN, HIGH);break; // when a is pressed on the app on your smart phone
        case 'b': digitalWrite(LED_BUILTIN, LOW);break; // when d is pressed on the app on your smart phone
        default : break;
      }
      Serial.println('z');
   }
   delay(50);
}
