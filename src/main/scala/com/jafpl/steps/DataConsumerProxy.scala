package com.jafpl.steps

/** A data consumer.
  *
  * This class is used to provide outputs from the pipeline. An instance of this class will
  * be provided after the runtime is constructed. The user can call `setProvider` to specify
  * where the data should be delivered.
  *
  */
trait DataConsumerProxy {
  def setConsumer(provider: DataConsumer): Unit
}
