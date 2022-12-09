package multiversion.loggers

import java.time.Clock

import moped.progressbars.ProgressRenderer
import moped.progressbars.ProgressStep
import moped.progressbars.Timer
import multiversion.diagnostics.MultidepsEnrichments.XtensionSeq
import multiversion.outputs.Docs
import org.typelevel.paiges.Doc

class ResolveProgressRenderer(
    maxRootDependencies: Long,
    clock: Clock,
    isTesting: Boolean
) extends ProgressRenderer {
  private val maxRootDependenciesWidth = maxRootDependencies.toString().length()
  val loggers =
    new CoursierLoggers(isArtifactDownload = false, _.endsWith(".pom"))
  private lazy val timer = new Timer(clock)
  override def renderStop(): Doc = {
    if (isTesting) Doc.empty
    else {
      // Force loggers to report
      loggers.getActiveLoggers()
      Docs.emoji.success + Doc.text(
        s"Resolved ${loggers.totalRootDependencies} root dependencies and ${loggers.totalTransitiveDependencies} transitive dependencies in ${timer.format()}"
      )
    }
  }
  override def renderStep(): ProgressStep = {
    val activeLoggers =
      loggers.getActiveLoggers().sortByCachedFunction(-_.totalArtifactCount())
    if (activeLoggers.isEmpty) ProgressStep.empty
    else {
      val currentTransitiveCount =
        activeLoggers.iterator.map(_.totalArtifactCount).sum
      val totalTransitive =
        loggers.totalTransitiveDependencies + currentTransitiveCount
      val remaining = maxRootDependencies - loggers.totalRootDependencies
      val header = Doc.text(
        List[String](
          "Resolving:",
          timer.formatPadded(),
          s"$remaining/$maxRootDependencies remaining",
          s"(${Words.transitiveDendencies.formatPadded(totalTransitive)})"
        ).mkString(" ")
      )
      val rows = Doc.tabulate(
        ' ',
        " ",
        activeLoggers.take(12).map { logger =>
          logger.name -> Doc.text(
            Words.transitiveDendencies.format(logger.totalArtifactCount())
          )
        }
      )
      val table = header + Doc.line + rows + Doc.line
      ProgressStep().withDynamic(table)
    }
  }

}
