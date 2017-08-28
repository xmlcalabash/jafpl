package com.jafpl.util

import com.jafpl.messages.{ItemMessage, Metadata}

class PipelineMessage(override val item: Any, override val metadata: Metadata)
  extends ItemMessage(item, metadata) {
}
