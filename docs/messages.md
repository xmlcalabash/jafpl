# Messages

FIXME: this documentation is out-of-date.

Everything in JAFPL is coordinated with messages. There is a central
`GraphMonitor` that sends messages to individual steps. All steps send
messages through the `GraphMonitor`.

The implementations of steps are wholly isolated from the message passing.
If all you care about is step implementation, there’s nothing else in here
that you need to care about.

## Atomic steps

The messages for atomic steps are straightforward. Each atomic step will
receive:

* `NInitialize` which runs step’s `initialize()` method.
* `NInput` which runs the step’s `input(port, item)` method,
   passing an item on the port.
* `NClose` which runs the step’s `close(port)` method, closing an input port.
* `NRun` which runs the step’s `run(port)` method.
* `NReset` which runs the step’s `reset()` method.
* `NStop` which runs the step’s `teardown()` method.

In order to shield the step implementation from timing issues, the
`run()` method will only be called after all of the inputs have been
closed. A step may perform arbitrary processing on each input, as it
arrives, but it cannot be sure that no additional input will arrive
until the `run()` method is called.

## Containers

The messages for containers are slightly more complicated. Each container is
divided into three parts, a “ContainerStart”, a “ContainerEnd”, and one or
more contained steps.

Some types of containers have special messages (for example, to test a
guard expression), these will be discussed elsewhere.

From the outside, a container is a single, atomic unit, just like any
other step. It has inputs, performs some processing, and has outputs.

From the inside, a container has a start and an end. The start and end,
like atomic steps, also have inputs and outputs. It works like this:

The inputs to the container can be read from the outputs of the
container start.

The inputs to the container end collect together the items that will
be the output from the container as a whole.


