# Creates language theory and semantics construction stub from GF abstract syntax.
# Note: This is a prototype! It only supports a subset of GF abstract syntaxes.

import fileinput


def tokenizer(string):
    string += " \n"  # hack to avoid running out of string indices too early
    tokens = []
    pos = 0
    linecounter = 1
    while pos < len(string):
        if string[pos].isspace():
            if string[pos] == "\n":
                linecounter += 1
            pos += 1
        elif string[pos].isalnum():
            token = string[pos]
            pos += 1
            while string[pos].isalnum():
                token += string[pos]
                pos += 1
            tokens += [(token, "id", linecounter)]
        elif string[pos] in "->*(){}=;,:":
            token = string[pos]
            pos += 1
            while string[pos] in "->*(){}=;,:":
                token += string[pos]
                pos += 1
            # handle comments:
            if token.startswith("--"):
                while string[pos] != "\n":
                    pos += 1
            elif token.startswith("{-") and "-}" not in token:
                while not (string[pos] != "-" and string[pos+1] != "}"):
                    if pos == len(string)-2:
                        break
                    pos += 1
            else:
                tokens += [(token, "op", linecounter)]
        else:
            raise Exception("Unexpected character in line " + str(linecounter) + ": " + repr(string[pos]))
    return tokens


def pop_op(tokens, pos, op):
    if tokens[pos][0] != op:
        raise Exception(f"Line {tokens[pos][2]}: Expected '{op}', but found '{tokens[pos][0]}'")
    return pos + 1

def process_tokens(tokens):
    if tokens[0][0] != "abstract":
        raise Exception("Line 1: Expected keyword 'abstract'")
    if tokens[1][1] != "id":
        raise Exception(f"Line {tokens[0][2]}: Expected name of abstract syntax")
    name = tokens[1][0]
    pop_op(tokens, 2, "=")

    pos = 3
    imports = []
    while tokens[pos][0] != "{":
        if tokens[pos][1] != "id":
            raise Exception(f"Line {tokens[pos-1][2]}: Expected '{{'")
        imports += [tokens[pos][0]]
        pos += 1
        if tokens[pos][0] not in [",","**"]:
            raise Exception(f"Line {tokens[pos-1][2]}: Expected '{{'")
        pos += 1
    pos += 1

    catfun = None
    cats = []
    funs = []
    while tokens[pos][0] != "}":
        if tokens[pos][0] == "cat":
            catfun = "cat"
            pos += 1
        elif tokens[pos][0] == "fun":
            catfun = "fun"
            pos += 1
        elif tokens[pos][1] == "op":
            raise Exception(f"Line {tokens[pos][2]}: Unexpected token: '{tokens[pos][0]}'")
        else:
            n = tokens[pos][0]
            pos += 1
            if catfun == "cat":
                pos = pop_op(tokens, pos, ";")
                cats += [n]
            elif catfun == "fun":
                pos = pop_op(tokens, pos, ":")
                t = []
                while tokens[pos][1] == "id":
                    t += [tokens[pos][0]]
                    pos += 1
                    if tokens[pos][0] == "->":
                        pos += 1
                pos = pop_op(tokens, pos, ";")
                funs += [(n, t)]
            else:
                raise Exception(f"I don't know if '{n}' is a cat or a fun")
    pos = pop_op(tokens, pos, "}")
    if pos != len(tokens):
        raise Exception(f"Line {tokens[pos][2]}: Didn't expect any more tokens")
    return (name, imports, cats, funs)

def generateMMT(args):
    name, imports, cats, funs = args
    jDD = "\u2759"
    jMD = "\u275a"
    jraa = "\u27f6"
    blank = "_"

    # language theory
    print("theory " + name + " : ?ur:LF =")
    for import_ in imports:
        print("    include ?" + import_ + " " + jDD)
    if imports and cats: print()
    for cat in cats:
        print("    " + cat + " : type " + jDD)
    if (imports or cats) and funs: print()
    for fun in funs:
        print("    " + fun[0] + " : " + (" "+jraa+" ").join(fun[1]) + " " + jDD)
    print(jMD)

    print()
    # semantics construction stub:
    print("view " + name + "Semantics : ?" + name + " -> " + blank + " =")
    for import_ in imports:
        print("    include ?" + import_ + "Semantics " + jDD)
    if imports and cats: print()
    for cat in cats:
        print("    " + cat + " = " + blank + " " + jDD)
    if (imports or cats) and funs: print()
    for fun in funs:
        print("    " + fun[0] + " = " + blank + " " + jDD)
    print(jMD)
    

string = ""
for line in fileinput.input():
    if fileinput.isfirstline():
        if string:
            generateMMT(process_tokens(tokenizer(string)))
            print("\n\n")
        string = ""
    string += line

if string:
    generateMMT(process_tokens(tokenizer(string)))

