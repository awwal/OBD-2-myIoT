# OBD-2-myIoT

This project parses obd car data logged using the TouchScan App to the iot product - https://my.iot-ticket.com. 

1. Buy a OBD Adapter bluetooth adapter

2. Get https://play.google.com/store/apps/details?id=OCTech.Mobile.Applications.TouchScan&hl=en

3. Create a free account at https://my.iot-ticket.com ,  if you don't have already.

4. Get a dropbox account,  if you don't have already.

5. Follow the instruction here http://ericlathrop.com/2014/02/logging-car-data-on-android/

6.Run java -jar project.jar path\to\appfolder\in\dropbox carName myusername mypassword

Example:  java -jar com.lawal.thesis-1.0-SNAPSHOT-jar-with-dependencies.jar C:\Users\Lawal\Dropbox\Apps\TouchScan\CsvLogs\FP31881DF87D1 ToyotaCorolla smartuser sM8pswd

Download jar https://github.com/awwal/OBD-2-myIoT/releases/download/v1.0.0/com.lawal.thesis-1.0-SNAPSHOT-jar-with-dependencies.jar
