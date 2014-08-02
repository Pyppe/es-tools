package fi.pyppe.estools

import com.typesafe.scalalogging.Logger

class ProgressLogger(val message: String, val totalNumber: Number, val logPercentIncrement: Int = 5)(implicit logger: Logger) {

  assert(logPercentIncrement > 0 && logPercentIncrement <= 100)

  private val logSteps = collection.mutable.Map() ++
    (0 to 100 by logPercentIncrement).map(percent => (percent, true)).toMap

  def logProgress(currentNumber: Number): Unit = {
    val percentDone = (currentNumber.doubleValue / totalNumber.doubleValue) * 100
    val step = round(percentDone.toInt)
    if (logSteps.getOrElse(step, false)) {
      logSteps.put(step, false)
      logger.info(s"${message}: ${step}% done")
    }
  }

  private def round(n: Int) = n / logPercentIncrement * logPercentIncrement

}