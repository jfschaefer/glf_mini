package info.kwarc.glf

import info.kwarc.mmt.api.MPath
import info.kwarc.mmt.api.frontend.{Extension, Run}
import info.kwarc.mmt.api.modules.{Theory, View}
import info.kwarc.mmt.api.objects._
import info.kwarc.mmt.api.symbols.{Constant, FinalConstant}
import info.kwarc.mmt.api.uom.SimplificationUnit

import scala.collection.immutable.HashMap

class GlfExtension(gfParser: GfParser, language : String, equivalentTheory : MPath,
                   semanticsView : Option[MPath]) extends Extension {
    println("MPATH: " + equivalentTheory)
    override def logPrefix: String = "gf"
    def present(tm : Term) : String = controller.presenter.asString(tm)

    lazy val theory : Theory = controller.getO(equivalentTheory) match {
        case Some(th : Theory) => th
        case None => ???
        case _ => ???
    }

    lazy val view : Option[View] = semanticsView.map(controller.getO(_) match {
        case Some(v : View) => {
            controller.simplifier.apply(v)
            v
        }
        case None => ???
        case _ => ???
    })

    lazy val theorymap : Map[String, Constant] = {
        controller.simplifier(theory)
        val consts = theory.getConstants ::: theory.getIncludesWithoutMeta.map(controller.get).collect {
            case t : Theory =>
                controller.simplifier(t)
                t.getConstants
        }.flatten
        HashMap(consts.map(c => (c.name.toString,c)):_*)
    }


    private val trav = new StatelessTraverser {
        override def traverse(t: Term)(implicit con: Context, state: State): Term = t match {
            case OMS(gn) => controller.getO(gn) match {
                case Some(fc : FinalConstant) if fc.df.isDefined => Traverser(this,fc.df.get)
                case _ => t
            }
            case _ => Traverser(this,t)
        }
    }

    def gf2mmt(sentence : String, cat : String, simplify : Boolean = true) : List[Term] = {
        gfParser.parse(sentence, language, cat)
                .map(_.toOMDocRec(theorymap))
                .map(t => view match {
                    case Some(v) => controller.library.ApplyMorphs(t, v.toTerm)
                    case None => t })
                .map(t => if (simplify) controller.simplifier(t,
                        SimplificationUnit(theory.getInnerContext,expandDefinitions=true,fullRecursion=true)) else t)
                .distinct
    }
}
