package com.jafpl.messages

class ExceptionMessage(cause: Throwable) extends ItemMessage(cause, Metadata.EXCEPTION) {

}
