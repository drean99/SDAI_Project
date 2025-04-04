# 🚗 Multi-Agent System for Intelligent Traffic Management

This project implements a Multi-Agent System (MAS) for intelligent management of urban traffic. It integrates the agent platform JADE (Java Agent Development Framework) and the open-source traffic simulator SUMO (Simulation of Urban MObility) using the TraCI4J Java library.

## 📌 Project Overview

The project features two main types of agents interacting within a simulated environment:

* Vehicle Agents (standard and aggressive): Represent vehicles navigating towards intersections, requesting permission to cross.

* Intersection Agents: Responsible for managing intersection traffic flow, ensuring safe and efficient vehicle crossings by handling vehicle requests dynamically.

Communication among agents is based on ACL (Agent Communication Language) messages, ensuring responsive and coordinated interactions

## 🚦 Main Components

* TestSimulation.java: Main entry point to initialize JADE runtime, start SUMO connection, and instantiate all agents.

* VehicleAgent.java and VehicleAggressiveAgent.java: Represent vehicle agents with distinct driving behaviors.

* IntersectionAgent.java: Manages vehicle crossing requests and coordinates intersection traffic dynamically.

* SimStepAgent.java: Advances simulation steps within SUMO.

* SumoConnector.java: Handles real-time interaction with SUMO via the TraCI4J library.

* Environment.java, VehicleHelper.java: Utility classes managing map and vehicle data.

## ⚙️ Technical Requirements

* JADE Framework
[JADE Official Website](https://jade.tilab.com/)

* SUMO (Simulation of Urban MObility)
[SUMO Official Website](https://eclipse.dev/sumo)

* TraCI4J (Java interface for SUMO's TraCI protocol)
[TraCI Documentation](https://github.com/egueli/TraCI4J)

* Java (JDK 11 or later)
