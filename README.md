# T2_CMKE_2025W
Topic 2 RBS&amp;FOL in Smart Mobility Concept CMKE_2025W

Project Summary – Smart Mobility Decision System (Java + Drools + FOL)

The system recommends the most suitable transportation method within a smart-mobility context (e.g., walking, biking, public transport).

Core technologies: Java backend, Drools (DRL files) for rule-based reasoning, and JPL (Java-Prolog bridge) for first-order logic evaluation.

Frontend: simple HTML/CSS/JavaScript interface for collecting user input.

Required user inputs include:
weather conditions (manually entered)
approximate route length (in meters or kilometers)
ecological sustainability preference
preferred transport modes
ticket or transportation option ownership
transport modes the user wants to avoid

External real data sources: Wiener Linien Routing Service (public, no API key required) used as the single real API.

All other data (weather, bike availability, etc.) is randomly simulated through static JSON or XML files.

Workflow:

1. Frontend gathers user input and sends JSON or XML to the Java backend.
2. Java backend optionally queries the Wiener Linien routing API for simple route/line information.
3. Backend assembles a complete "input JSON/XML" containing user preferences, route length, weather, and routing data.
4. First-order logic module (via JPL) computes environmental or logical factors.
5. Drools receives all facts and applies rule-based reasoning to select the recommended transportation method.
6. The system returns an “output JSON/XML" containing the final transport recommendation.

Architecture is stateless (no database).

Testing can be done by sending fake JSON/XML requests directly to the backend (via Podman or curl) without the UI.
