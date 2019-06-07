# ORAM

ORAM is short for Oblivious RAM, which is a cryptographic primitive used to simulate RAM and hide the access pattern. It can be used when data is stored on an online server, and you want to make sure the server can't tell what block of you are accessing or if you are reading or writing. It is an additional layer of security put on top when conventional encryption is not enough.

This repository contains the code used for experiments in connections with my work on my Master's Thesis. This is the client-side software that works together with the [server-side](https://github.com/christofferj1/oram_server).

### Run with gradle

If you have gradle installed, you can build and run it with the commands *gradle build* and *gradle run*.

If you do not have gradle installed, you can use the wrapper scripts, *gradlew* for Unix based systems and *gradlew.bat* for Windows based. These scripts should also be used with the "build" and "run" commands. They are build with gradle version 5.3.

### Using the software

Before starting the client, set up the server-side. When running the software, you use a command line interface to

1. choose how many layers of ORAM you want to run. If you choose 1, you run an ORAM with a local position map (and you decide the size).
2. choose which kind of ORAM you would like to run, Lookahead ORAM, Path ORAM or Trivial ORAM. If you have chosen to run multiple layers of recursive ORAM, you must choose an ORAM for each layer, starting with the biggest. The sizes are predefined. If you choose either Trivial or the specialized Lookahead ORAM, no more layers need to be chosen, as the ORAMs don't need further layers for the position map. ([l/lt/p/t] means you should write e.g. 'l' and hit enter.)
3. choose how many numbers of rounds to run (the number of accesses).
4. choose the IP address to connect to, the port is hard coded to 59595. If you run the server-side software on a local machine, just use 127.0.0.1. Make sure the server-side software is set up before connecting to it.