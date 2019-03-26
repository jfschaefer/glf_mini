package info.kwarc.glf

import scala.sys.process.Process
import scalaj.http.Http
import net.liftweb.json._  //https://alvinalexander.com/scala/scala-lift-json-array-list-strings-example

import scala.collection.mutable.ListBuffer


class GfServer(location : String, port : Int = 41296) {
    val process = Process(List("gf", "--server=" + port, "--document-root=" + location))
    process.run

    def getRequest(pgfPath : String, params : Map[String, String]) : List[String] = {
        var request = Http("http://localhost:" + port + "/" + pgfPath)
        for (param <- params) {
            request = request.param(param._1, param._2)
        }
        val response = request.asString
        // println(response.body)
        val json = JsonParser.parse(response.body)

        // TODO: The following is a bad hack. Understand json lib and fix it!
        //     (problem can be seen with `println((json \\ "trees").children)`)
        // (json \\ "trees").children.map(tree => tree.values.toString.drop(5).dropRight(1))
        val trees = (json\\"trees").children.map(tree => tree.values)
        if (trees.isEmpty) {
            List()
        } else {
            trees.head.toString.drop(5).dropRight(1).split(", ").toList
        }
    }
}


class ServerGfParser(server : GfServer, pgfPath : String) extends GfParser {
    // override def linearize(expr: GfExpr, language: String, cat : String): String = ???

    override def parse(sentence: String, language: String, cat : String): List[GfExpr] = {
        val str_trees = server.getRequest(pgfPath, Map("command" -> "parse", "input" -> sentence, "cat" -> cat))
        str_trees.map(parseTree)
    }


    def parseTree(str : String) : GfExpr = {
        // TODO: This method should probably be moved somewhere else

        var head_end = 0
        while (head_end < str.length && str.charAt(head_end) != ' ') {
            head_end += 1
        }

        val head = str.take(head_end)

        var args = ListBuffer[GfExpr]()

        // parse args
        var i = head_end + 1
        while (i < str.length) {
            if (str.charAt(i) == '(') {   // argument in parenthesis
                i += 1
                val start = i
                var bracketcount = 1
                while (bracketcount != 0) {
                    if (str.charAt(i) == '(') {
                        bracketcount += 1
                    } else if (str.charAt(i) == ')') {
                        bracketcount -= 1
                    }
                    i += 1
                }
                args.append(parseTree(str.substring(start, i-1)))
                i += 1
            } else {    // argument not in parenthesis
                val start = i
                while (i < str.length && str.charAt(i) != ' ') {
                    i += 1
                }
                args.append(parseTree(str.substring(start, i)))
                i += 1
            }
        }

        GfFun(head, args.toList)
    }
}
