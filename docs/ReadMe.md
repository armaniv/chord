# Chord
This project implements "Chord: A Scalable Peer-to-peer Lookup Protocol for Internet Applications" algorithm. All the informations about protocol specifications can be found in the original paper.

## Requirements
* Java 11 
* [Eclipse IDE Version: 2019-06 (4.12.0)](https://www.eclipse.org/downloads/packages/release/2019-06)
* [Symphony Version: 2.7](https://repast.github.io/)
* [gnuplot](http://www.gnuplot.info/) (OPTIONAL)

## Simulator configuration
Our simulator is based on Repast Symphony, an agent-based modeling toolkit
and  cross-platform  Java-based  modeling  system.   This  technology  allows
represent graphically a complex system with the usage of efficient visualization
features. Data for statistics is collected using Repast and analyzed with gnuplot.

## Installation
* Clone the repository
* Import the project as a Repast project into Eclipse IDE
* Run the project

or
 
* Download the jar from [here](https://drive.google.com/file/d/1cFAPJimTmByIpYYuX9FHccJoei_pfYK7/view?usp=sharing)
* Install the project following the procedure
* Run the project

## Simulation parameters
When the simulator is running, a GUI is showed to the user which presents some input boxes that can be used to modify the parameters of the simulation. These parameters are well described in the report.


## Test
In order to test the implementation we have four types of statistics which can be computed and analyzed:
* Load balance: composted of the average number and the the probability density function (PDF) of the number of keys per node that a node is responsible for. 
* Simultaneous Node Failures:  we evaluate the ability to performing lookups in case of a massive failure of nodes by computing the average path length.   
* Lookups During Stabilization: we evaluate the ability to performing lookups when nodes are continuously joining and crashing by computing the average path length.

All these experiments are computed in one shot at a specified tick count, outputted using Java System.out.println(), collected and used to plot graphics using gnuplot. Hence, in order to output the results of the experiments, you have to check the standard output of the java program and analyze the numbers and use gnuplot or a similar tools to plot charts.

## Reference

Ion Stoica, Robert Tappan Morris, David Liben-Nowell, David R. Karger, M. Frans Kaashoek, Frank Dabek, Hari Balakrishnan: Chord: a scalable peer-to-peer lookup protocol for internet applications.IEEE/ACM Trans. Netw. 2003: 17-32


## Authors

* **Valentino Armani (203290)** - [armaniv](https://github.com/armaniv)
* **Marian Alexandru Diaconu (203316)** - [neboduus](https://github.com/neboduus)
