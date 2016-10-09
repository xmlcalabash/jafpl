package com.jafpl.messages

import com.jafpl.items.GenericItem

/**
  * Created by ndw on 10/3/16.
  */
class ItemMessage(val port: String, val senderId: Long, val sequenceNo: Long, val item: GenericItem) {

}
