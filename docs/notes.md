# Notes

Edges connect nodes in the graph.

## Steps

A step is either an atomic step or a container step.

### Atomic steps

An atomic step is a node in the graph with input and output edges.

Atomic steps publish a set of input ports on which they expect to
receive documents, a set of binding ports on which they expect to
receive bindings, and a set of output ports to which they may send
documents.

Atomic steps expect the following messages:

* Document(portName, SeqNo, data)
* Close(portName)
* Binding(name, value)
* Start()
* Stop()
* Reset()
* Terminate()

Atomic steps produce the following messages:

* Document(portName, SeqNo, data)
* Close(portName)
* Finished()
* Error()

### Splitter

A `Splitter` is an Atomic step that publishes a single input and
one or more outputs. When `Start()` is received, it copies each document
that arrives on the input port to each of the output ports.

When its input port is closed, it closes its output port and sends
`Finished()`.

### Buffer

A `Buffer` is an Atomic step that publishes a single input and a
single output. When `Start()` is received, it copies each document
that arrives on the input port to the output port. It also keeps a copy
of each document that is copied. When the input port is closed, it closes
its output port and sends `Finished()`.

When `Reset()` is received, it prepares to send all of the buffered
documents again. If another `Start()` is received it does so.

When `Stop()` is received, it discards the buffered inputs.

### Container steps

Containers consist of a ContainerStart, a ContainerEnd, and one or more
contained steps.

#### ContainerStart steps

ContainerStart steps publish a set of input ports on which they expect
to receive documents, a set of binding ports on which they expect to
receive bindings, and a set of output ports to which they may send
documents.

A ContainerStart expects the same messages as an Atomic step plus one
additional message ContainerFinished().

In a vacuous container, when the ContainerStart receives a `Start()`, it
sends `Start()` to all the steps it contains and its ContainerEnd.

A ContainerStart sends Finished() when it receives a
ContainerFinished() message from its ContainerEnd.

#### ContainerEnd steps

ContainerEnd steps publish a set of input ports on which they expect
to receive documents and a set of output ports to which they may
send documents.

A ContainerEnd expects the same messages as an Atomic step.

ContainerEnd steps are essentially passthrough steps. They receive
documents on input ports and send documents to output ports.

When all of the input ports to a ContainerEnd have been closed, the
ContainerEnd sends a ContainerFinished() message to its
ContainerStart.

### GuardedContainer steps

A `GuardedContainer` step publishes an input port named `context` and
zero or more bindings.

When a `GuardedContainer` receives a `TestGuard()` message, it decides
whether or not the guard passes and sends a `GuardPass()` or
`GuardFail()` message.

### Choose steps

A `Choose` is a container step with one or more `GuardedContainer` steps.

When a `Choose` step receives a `Start()` message arrives, it sends a
`TestGuard()` message to each of it’s contained `GuardedContainer`
steps in turn. It sends `Start()` to the first and only the first
container that returns `GuardPass()`.

### ForEach steps

A `ForEach` is a container step. It publishes a `source` input port and a
`current` output port.

`ForEach` iterates over the documents received on the `source` port, sending
them one at a time to the steps it contains.

After `Start()` has been received, the `ForEach` writes a single
document to the `current` port and sends `Start()` to all the steps it
contains and its `ContainerEnd`.

When it receives a `ContainerFinished` message from its `ContainerEnd`, if
there are more documents to process, it sends `Reset()` to all the steps
it contains and its `ContainerEnd`.

If there are no more documents, it sends `Stop()` to all the steps it
contains and its `ContainerEnd`. Then it sends `Finished()`.

### TryCatch steps

A `TryCatch` is a container step with two container children.

When `Start()` is received, it runs the first container. If no error is
received, that’s the result of the container. Otherwise, the second container
is run.
