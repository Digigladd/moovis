# MOOVIS: RT Socket listener 

This service listen to real time TVI data on port 7002, you need an AVAYA RT_Socket session.

# Persistence

Real Time data are saved in order to restart from a fresh snapshot (< 24h old). 
Create the volume 'listener' on your docker environment.

