# RTMPServer cli

In this directory, you can find a command line interface (CLI) for the `RtmpServer`. This CLI allows
you to run the RTMPServer in a standalone mode, which is useful for testing and debugging purposes.

## Installation

Run the `gradlew` command to install:

```bash
./gradlew --quiet ":samples:rtmpserver-cli:installDist"
```

## Usage

Run the `rtmpserver-cli` command with the listening address. For example:

```bash
./samples/rtmpserver-cli/build/install/rtmpserver-cli/bin/rtmpserver-cli 0.0.0.0:1935
```
