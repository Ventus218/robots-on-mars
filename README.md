# Robots on Mars

## Abstract

### Rovers

Some new rovers just arrived on Mars to explore the planet, their goals are:

- building a map of the area marking all points of interest
- collect samples of specific rocks which may need drilling the terrain
- transport samples at the main base where they will be stored long term

Each rover is equipped with:

- a 360° camera
- wheels
- short range communication antenna
- a battery

Some of the rovers are "scientists" and therefore they are also equipped with:

- drills for mining rocks samples
- robotic arms to collect samples
- limited sample storage

### Environment

The environment will be generated randomly and will be modeled as a grid where
each cell can represent:

- a rover
- flat empty surface
- rock obstacle
- mining spot
- sample to collect
- base

### Exploration

Rovers should prioritize mapping areas closer to the base.

They can only move one cell at a time following the cardinal directions. While
moving they also map the environment thanks to their cameras.

### Samples collection

Some samples are immediately collectable while others needs to be drilled.

Mining spots only need to be drilled once. Rovers are able to recognise spots
that were already drilled thanks to their cameras.

Each sample occupies the same amount of space in the storage.

### Energy management

- moving has a relatively low energy consumption
- mining rocks consumes a lot of energy
- rovers can charge their batteries at the main base

### Communication

There won't be any network infrastructure so the rovers will be able to exchange
informations only when they are in range of their antennas.

The base is also equipped with an antenna and can store data as well as the
rovers can.
