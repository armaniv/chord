# Chord
This project implements "Chord: A Scalable Peer-to-peer Lookup Protocol for Internet Applications" algorithm. All the informations about protocol specifications can be found in the original paper. Moreover if you want to have a detailed view about the architecture of this implementation please read this [report](https://github.com/armaniv/chord/blob/master/docs/ImplementationReport.pdf).

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
When the simulator is running, a GUI is showed to the user which presents some input boxes that can be used to modify the parameters of the simulation. Such parameters are:

* Number of Nodes
* Probability of single node crash
* Number of Lookups per Stabilization Round
* Number of Lookups/40 ticks

More on the parameters in the [report](https://github.com/armaniv/chord/blob/master/docs/ImplementationReport.pdf).


## Test
To evaluate that in our implementation the communication cost and the state maintained  by  each  node  scale  logarithmically  with  the  number  of  nodes,  we performed some experiments.  Results of the are computed before at the end of a stabilization round.  We basically analyze the following 3 probability distributions:
* Number of Keys per Node - from which we calculate the mean, 1stand 99th percentiles.
* Lookup Path Length - from which we compute mean
* Number of Timeouts per Lookup - from which we compute the mean

If the reader of this report desires to test such computed measures, it has to execute the code in development mode and inspect its IDE's console. An example of the output is the following:
        
```
########## SIMULATION 1578266879519##########
Nodes: [459282, 1053995, 14729669, 14948457, 16542290, ...]
Space Dimension; Num. of Keys; Mean Num. of Keys; 1st percentile; 99th percentile
2147483647;1000;2147483.647;16272;9780939
Num. of Keys per Node Distribution: [1065;1096;3224;3307;4776;5803;7669;10324;....]
Mean Path Length: 6.7025779917745005
Mean Num. of Timeouts: 0.003912127595546194
```

## Reference

Ion Stoica, Robert Tappan Morris, David Liben-Nowell, David R. Karger, M. Frans Kaashoek, Frank Dabek, Hari Balakrishnan: Chord: a scalable peer-to-peer lookup protocol for internet applications.IEEE/ACM Trans. Netw. 2003: 17-32


## Authors

* **Valentino Armani (203290)** - [armaniv](https://github.com/armaniv)
* **Marian Alexandru Diaconu (203316)** - [neboduus](https://github.com/neboduus)
