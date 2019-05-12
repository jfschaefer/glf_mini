package info.kwarc.glf

import info.kwarc.glf
import info.kwarc.mmt.api.DPath
import info.kwarc.mmt.api.frontend.Run
import info.kwarc.mmt.api.utils.URI

object Main extends App {
    override def main(args : Array[String]) = {
        if (args.length != 7) {
            println("Error: Expected 7 command line arguments")
        }
        // The `archivepath` specifies, where mmt theories are stored locally.
        val archivepath = if (args(0).charAt(0) != '/') System.getProperty("user.dir") + "/" + args(0) else args(0)

        /*
           The pgf file contains the GF grammars. It can be generated with `gf -make LifeLexEng.gf`.
           For technical reasons, only a relative path can be provided.
           The following path is relative to the `archivepath` above.
           (If you haven't done so yet, you should clone the repository into the mmt content directory)
       */
        val pgfpath = args(1)   // relative to archivepath
        val language = args(2)  // name of the concrete grammar used for parsing
        val startcat = args(3)
        val dpath = DPath(URI(args(4)))
        val lang_theory = dpath ? args(5)
        val sem_view = dpath ? args(6)

        println("Starting GLF with the following settings:")
        println("  - archive path:                " + archivepath)
        println("  - grammar file:                " + pgfpath)
        println("  - language name:               " + language)
        println("  - start category:              " + startcat)
        println("  - language theory:             " + lang_theory)
        println("  - semantics construction view: " + sem_view)


        // The sentences used as input
        def getSentence(): String = {
            scala.io.StdIn.readLine("Please enter a sentence: ")
        }


        // effectively the `main` function
        def run(): Unit = {
            // The GF server is started with the working directory `archivepath`.
            // If necessary, a port can also be specified.
            val server = new glf.GfServer(archivepath)

            // The parser uses the server to parse sentences.
            // The `pgfpath` specifies where the pgf grammar is.
            // `pgfpath` has to be relative to the server's working directory.
            val parser = new ServerGfParser(server, pgfpath)

            // The GfMmmtBridge uses the parser to translate sentences into
            // terms using an mmt theory
            val bridge = new GlfExtension(parser, language, lang_theory, Some(sem_view))
            Run.controller.extman.addExtension(bridge)

            while (true) {
                val input = getSentence()
                println("You said '" + input + "'")
                val trees = bridge.gf2mmt(input, startcat, true) // parse with the gf category 'Stmt'

                if (trees.isEmpty)
                    println("I failed to parse that.")
                else {
                    println("I got the following interpretations:")
                    for (tree <- trees) {
                        println(bridge.present(tree))
                    }
                }
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
}
