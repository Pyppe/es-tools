package fi.pyppe.estools

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

trait LoggerSupport { self =>

    implicit val logger: Logger =
      Logger(LoggerFactory.getLogger(self.getClass.getSimpleName.replaceAll("\\$$", "")))

}
