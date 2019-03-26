package info.kwarc.glf

import info.kwarc.glf
import info.kwarc.mmt.api.DPath
import info.kwarc.mmt.api.frontend.Run
import info.kwarc.mmt.api.utils.URI
import info.kwarc.mmt.api.objects.{OMS, Term}
import info.kwarc.mmt.lf.ApplySpine

object Main extends App {
    // The `archivepath` specifies, where mmt theories are stored locally.
    val archivepath = "/home/jfs/git/github_com/jfschaefer/glf_mini/MathHub"

    /*
      The pgf file contains the GF grammars. It can be generated with `gf -make LifeLexEng.gf`.
      For technical reasons, only a relative path can be provided.
      The following path is relative to the `archivepath` above.
      (If you haven't done so yet, you should clone the repository into the mmt content directory)
     */
    val pgfpath = "COMMA/gfbridge/source/lfmtp2019/LifeLex.pgf"  // relative to archivepath


    val language = "LifeLexEng"    // name of the concrete grammar used for parsing
    val dpath = DPath(URI.http colon "mathhub.info") / "COMMA" / "GfBridge" / "lfmtp2019"


    // The sentences used as input
    def getSentence() : String = {
        scala.io.StdIn.readLine("Please enter a sentence: ")
    }


    // effectively the `main` function
    def run() : Unit = {
        // The GF server is started with the working directory `archivepath`.
        // If necessary, a port can also be specified.
        val server = new glf.GfServer(archivepath)

        // The parser uses the server to parse sentences.
        // The `pgfpath` specifies where the pgf grammar is.
        // `pgfpath` has to be relative to the server's working directory.
        val parser = new ServerGfParser(server, pgfpath)

        // The GfMmmtBridge uses the parser to translate sentences into
        // terms using an mmt theory (dpath ? "Example1")
        val bridge = new GlfExtension(parser, language, dpath ? "LifeLex", Some(dpath ? "LifeLexSemantics"))
        Run.controller.extman.addExtension(bridge)


        // The knowledge is represented as a set of different possible sets of facts about the world.
        // For example, (A and not B) or C would be represented as {{(A, true), (B, false)}, {(C, true)}}
        // It is updated with new knowledge from every sentence.
        var knowledge : Set[Set[(Term, Boolean)]] = Set(Set())
        while (true) {
            val input = getSentence()
            println("You said '" + input + "'")
            val trees = bridge.gf2mmt(input, "Stmt")   // parse with the gf category 'Stmt'

            println("I got the following interpretations:")
            for (tree <- trees) {
                println(bridge.present(tree))
            }

            // `bridge.present` can be used to get a nicer string representation of terms.
            println("I know already: " + knowledge.map(_.map(t => bridge.present(t._1) + (if (t._2) "ᵗ" else "ᶠ"))))
        }
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
