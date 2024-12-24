from pathlib import Path
from sys import argv

classes = {
    "Stmt" : {
        "Block"         : ["List<Stmt> statements"],
        "Expression"    : ["Expr expression"],
        "If"            : ["Expr condition", "Stmt thenBranch", "Stmt elseBranch"],
        "Print"         : ["Expr expression"],
        "Let"           : ["Token name", "Expr initializer"],
        "While"         : ["Expr condition", "Stmt body"],
    },
    "Expr" : {
        "Binary"        : ["Expr left", "Token operator", "Expr right"],
        "Grouping"      : ["Expr expression"],
        "Literal"       : ["Object value"],
        "Logical"       : ["Expr left", "Token operator", "Expr right"],
        "Variable"      : ["Token name"],
        "Unary"         : ["Token operator", "Expr right"],
        "Assign"        : ["Token name", "Expr right"],
    },
}

DEFAULT_DIR=Path("../src/damlang")

tdir = DEFAULT_DIR
if len(argv) == 2:
    tdir = Path(argv[1])
    if not tdir.exists():
        print(f"{argv[1]} does not exist. Exiting.")
        exit(1)

for abstract_class in classes:
    lower = abstract_class.lower()

    tfile = tdir / f"{abstract_class}.java"
    with open(tfile, "w") as f:
        f.write(
"""package damlang;

import java.util.List;

abstract class """ + abstract_class + """ {
    interface Visitor<T> {
""")

        for clazz in classes[abstract_class]:
            f.write(
f"        T visit{clazz}{abstract_class}({clazz} {lower});\n"
            )

        f.write("""    }

    abstract <T> T accept(Visitor<T> visitor);

""")

        for clazz in classes[abstract_class]:
            props = classes[abstract_class][clazz]
            f.write(f"""
    static class {clazz} extends {abstract_class} {{
        {clazz}({", ".join(props)}) {{
""")
            for type_var in props:
                prop_name = type_var.split(" ")[1]
                f.write(f"            this.{prop_name} = {prop_name};\n")
            f.write(f"""        }}

        @Override
        <T> T accept(Visitor<T> visitor) {{
            return visitor.visit{clazz}{abstract_class}(this);
        }}

""")
            for type_var in props:
                f.write(f"        {type_var};\n")

            # End the subclass
            f.write("    }\n")

        # End the abstract class
        f.write("}") 



