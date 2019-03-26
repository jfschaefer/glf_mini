package info.kwarc.glf

import info.kwarc.mmt.api
import info.kwarc.mmt.api.frontend.{Logger, Run}
import info.kwarc.mmt.api.ontology.{DeclarationTreeExporter, DependencyGraphExporter, PathGraphExporter}
import info.kwarc.mmt.api.web.JSONBasedGraphServer

import scala.concurrent.Future

class Runner(run : () => Unit,
                      archivepath : String,
                      logprefixes : List[String] = Nil,
                      alignmentspath : String = "",
                      serverport : Option[Int] = None,
                      gotoshell : Boolean = true,
                      logfile : Option[String] = None) extends Logger {
  val controller = Run.controller
  def logPrefix = "user"
  def report = controller.report

  // If you want to log additional stuff, just put it in this list

  controller.handleLine("log console")
  if (logfile.isDefined) controller.handleLine("log html " + logfile.get)// /home/raupi/lmh/mmtlog.txt")
  ("test" :: logprefixes) foreach (s => controller.handleLine("log+ " + s))
  controller.handleLine("extension info.kwarc.mmt.lf.Plugin")
  controller.handleLine("extension info.kwarc.mmt.odk.Plugin")
  // controller.handleLine("extension info.kwarc.mmt.pvs.Plugin")
  //   controller.handleLine("extension info.kwarc.mmt.metamath.Plugin")
  controller.handleLine("mathpath archive " + archivepath)
  // controller.handleLine("extension info.kwarc.mmt.api.ontology.AlignmentsServer " + alignmentspath)


  def doFirst : Unit = {}

  // def run : Unit

  /*
  def log(s : String) = {
    controller.report("user",s)
    controller.report.flush
  }
  */

  def launch(): Unit = try {

    controller.extman.addExtension(new DependencyGraphExporter)
    controller.extman.addExtension(new DeclarationTreeExporter)
    controller.extman.addExtension(new JSONBasedGraphServer)
    controller.extman.addExtension(new PathGraphExporter)
    doFirst
    if (serverport.isDefined) {
      //controller.handleLine("clear")
      controller.handleLine("server on " + serverport.get)
    }
    if (gotoshell) {
      Future {Run.main(Array())}(scala.concurrent.ExecutionContext.global)
      Thread.sleep(1000)
    }

    // Windows-only fix:
    // Explicity load the native library jpgf and *all* its dependent native libraries
    // in advance before jpgf.jar loads "jpgf" alone.
    // This fixes the error "Can't find dependent libraries" on Windows when code within
    // jpgf.jar tries to load the native lib "jpgf" without its dependencies beforehand.
    // https://stackoverflow.com/questions/1087054/jni-dependent-libraries/2906862#2906862.
    if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
      System.loadLibrary("libgu-0")
      System.loadLibrary("libpgf-0")
      System.loadLibrary("libsg-0")
      System.loadLibrary("jpgf")
    }

    run()
  } catch {
    case e: api.Error => println(e.toStringLong)
      sys.exit
  }

  def hl(s : String) = controller.handleLine(s)
}

