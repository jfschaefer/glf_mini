package info.kwarc.glf

import info.kwarc.glf
import info.kwarc.mmt.api.DPath
import info.kwarc.mmt.api.frontend.Run
import info.kwarc.mmt.api.utils.URI
import info.kwarc.mmt.api.objects.{OMS, Term}
import info.kwarc.mmt.lf.ApplySpine

object TableauExample extends App {
  val archivepath = System.getProperty("user.dir") + "/MathHub"
  val pgfpath = "COMMA/glf/source/lfmtp2019/LifeExtendedLex.pgf"  // relative to archivepath
  val language = "LifeExtendedLexEng"    // name of the concrete grammar used for parsing
  val dpath = DPath(URI.http colon "mathhub.info") / "COMMA" / "GLF" / "lfmtp2019"

  def getSentence() : String = {
    scala.io.StdIn.readLine("Please enter a sentence: ")
  }

  def run() : Unit = {
    val server = new glf.GfServer(archivepath)
    val parser = new ServerGfParser(server, pgfpath)
    val bridge = new GlfExtension(parser, language, dpath ? "LifeExtendedLex", Some(dpath ? "LifeExtendedLexSemantics"))
    Run.controller.extman.addExtension(bridge)

    // The knowledge is represented as a set of different possible sets of facts about the world.
    // For example, (A and not B) or C would be represented as {{(A, true), (B, false)}, {(C, true)}}
    // It is updated with new knowledge from every sentence.
    var knowledge : Set[Set[(Term, Boolean)]] = Set(Set())
    while (true) {
      val input = getSentence()
      val trees = bridge.gf2mmt(input, "Stmt", true, true)

      if (trees.isEmpty)   // no parse tree obtained
        println("I don't understand you! (I couldn't parse the sentence with the grammar)")
      else {
        println("Here are my interpretations:")
        for (tree <- trees) {
          println("  " + bridge.present(tree))
        }

        val result = nextSentence(trees, knowledge)  // `nextSentence` returns updated knowledge and a comment.
        knowledge = result._1
        println(result._2)
      }

      println("Here is my belief state:")
      var counter = 1
      for (option <- knowledge) {
        println("  Option " + counter + ": " + option.map(t => (if (t._2) "" else "NOT ") + bridge.present(t._1)).mkString(" ;  "))
        counter = counter + 1
      }
      // println("I know already: " + knowledge.map(_.map(t => bridge.present(t._1) + (if (t._2) "ᵗ" else "ᶠ"))))
    }
  }



  // The update algorithm (`nextSentence`) adds the information of a sentence to the knowledge set.
  // The knowledge is represented as a set of different possible sets of facts about the world.
  // For example, (A and not B) or C would be represented as {{(A, true), (B, false)}, {(C, true)}}
  // A, B and C would be 'atomic' facts like "loath(Fiona, Berti)", i.e. propositions without logical connectives.


  // `addReading` is a helper function that recursively decomposes a term to add it to the knowledge set.
  // The boolean value indicates, whether the sentence is supposed to be true or false.
  def addReading(reading : (Term, Boolean), knowledge : Set[Set[(Term, Boolean)]]) : Set[Set[(Term, Boolean)]] = {
    reading match {
      case (not(x), b) => addReading((x, !b), knowledge)
      // if a AND b is true, then add first a to the knowledge set and then b.
      case (and(a, b), true) => addReading((b, true), addReading((a, true), knowledge))
      // if a AND b is false, then add a to the knowledge set and add b to a copy of the knowledge set and take the union
      case (and(a, b), false) => addReading((a, false), knowledge) ++ addReading((b, false), knowledge)
      // add atomic terms and the truth value to every set in the knowledge set. remove those sets that contain a contradiction
      case atomicterm => removeContradictions(knowledge.map(s => s + atomicterm))

      // Note: implication and disjunction are defined through conjunction and negation in the mmt theory.
      // As these definitions are expanded, we do not need to cover them here anymore.
    }
  }

  // Removes contradicting sets from the knowledge set.
  // Contradictions occur, if both (A, true) and (A, false) occur in the same set in the knowledge set.
  def removeContradictions(knowledge : Set[Set[(Term, Boolean)]]) : Set[Set[(Term, Boolean)]] = {
    knowledge.filter(s => !s.exists(t => s.contains(t._1, !t._2)))
  }

  // Returns a knowledge set that incorporates the readings of a sentence along with a comment.
  def nextSentence(readings : List[Term], knowledge : Set[Set[(Term, Boolean)]]) : (Set[Set[(Term, Boolean)]], String) = {
    // for each reading an updated knowledge set is created and the union of all of them is taken.
    val newKnowledge = readings.flatMap(reading => addReading((reading, true), knowledge))
      .toSet

    if (newKnowledge == knowledge) {  // no changes in knowledge
      (newKnowledge, "That's obvious. (My belief state is unchanged")
    } else if (newKnowledge.isEmpty) {    // there must have been a contradiction (no consistent set of facts remaining)
      (Set(Set()), "That doesn't make any sense! Let's start from the beginning.")
    } else {  // new knowledge obtained
      (newKnowledge, "That's interesting. (I've updated my belief state)")
    }
  }

  // These objects allow us to use `and` and `not` for the decomposition of terms as done in `addReading`.
  object not {
    val path = dpath ? "PropLogicSyntax" ? "neg"

    def unapply(tm: Term) = tm match {
      case ApplySpine(OMS(`path`), List(tm1)) => Some(tm1)
      case _ => None
    }

    def apply(tm1: Term) = ApplySpine(OMS(path), tm1)
  }

  object and {
    val path = dpath ? "PropLogicSyntax" ? "and"
    def unapply(tm : Term) = tm match {
      case ApplySpine(OMS(`path`),List(tm1,tm2)) => Some((tm1,tm2))
      case _ => None
    }
    def apply(tm1 : Term, tm2 : Term) = ApplySpine(OMS(path),tm1,tm2)
  }


  // The code below sets up mmt and then calls the `run` method.
  // Hopefully, you won't have to change anything here.
  val runner = new Runner(() => run(),
    archivepath,
    List("gf"),
    "",
    Some(8080),
    false,
    None
  )

  runner.launch()
}
