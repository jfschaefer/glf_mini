package info.kwarc.glf

trait GfParser {
    def parse(sentence : String, language : String, cat : String) : List[GfExpr]
    // def linearize(expr : GfExpr, language : String, cat : String) : String
}
