package com.jafpl.messages

class PipelineMessage(override val item: Any, override val metadata: Metadata)
  extends ItemMessage(item, metadata) {
}
