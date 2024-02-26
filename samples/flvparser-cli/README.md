# FLVParser cli

In this directory, you can find a simple command line interface (CLI) for the `flv` library. This
CLI allows you to parse FLV files and extract information about the tags contained within them.

## Installation

Run the `gradlew` command to install:

```bash
./gradlew --quiet ":samples:flvparser-cli:installDist"
```

## Usage

Run the `flvparser-cli` command with the path to the FLV file you want to parse. For example:

```bash
./samples/flvparser-cli/build/install/flvparser-cli/bin/flvparser-cli -i /path/to/your/file.flv
```
