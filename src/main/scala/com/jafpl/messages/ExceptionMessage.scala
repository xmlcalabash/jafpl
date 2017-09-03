package com.jafpl.messages

private[jafpl] class ExceptionMessage(cause: Throwable) extends ItemMessage(cause, Metadata.EXCEPTION) {

}
