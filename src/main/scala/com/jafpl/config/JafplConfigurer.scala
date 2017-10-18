package com.jafpl.config

/** A class for configuring a [[com.jafpl.config.Jafpl]] instance.
  *
  * The trait serves as an extension point. New Jafpl instances will be configured
  * by calling a configurer.
  */

trait JafplConfigurer {
  /** Configure the Jafpl instance.
    *
    * @param jafpl The Jafpl object that shall be configured.
    */
  def configure(jafpl: Jafpl): Unit
}
