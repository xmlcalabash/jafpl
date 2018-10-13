package com.jafpl.steps

trait ManifoldSpecification {
  /** The names of the input ports this step requires.
    *
    * This method returns the names of the input ports that the step requires.
    *
    * @group IOSpecs
    * @return The list of required port names.
    */
  def inputSpec: PortSpecification

  /** The names of the output ports this step requires.
    *
    * This method returns the names of the output ports that the step requires.
    *
    * @group IOSpecs
    * @return The list of required port names.
    */
  def outputSpec: PortSpecification

}
