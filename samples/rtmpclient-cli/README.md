# RTMPServer cli

In this directory, you can find a command line interface (CLI) for the `RtmpClient`. This CLI allows
you to run the RTMPServer in a standalone mode, which is useful for testing and debugging purposes.

## Installation

Run the `gradlew` command to install:

```bash
./gradlew --quiet ":samples:rtmpclient-cli:installDist"
```

## Usage

Run the `rtmpclient-cli` command with the listening address. For example:

```bash
./samples/rtmpclient-cli/build/install/rtmpclient-cli/bin/rtmpclient-cli 192.168.1.11:1935 -i /path/to/your/file.flv
```
