# Dam Language
The Dam language is a basic programming language with limited features.

Similar to C++ and Java, this language doesn't require any sort of whitespace. Each statement is terminated with a semicolon and blocks are wrapped in curly braces. Similar to python, the Dam language is dynamically typed, allowing for more flexibility from our variables. Despite being dynamically types, the main focus of this language was to catch as many type errors as possible at compile time as opposed to run time.

Anyone with Java installed can use this language. To run this language, you can download everything from the src and lib directories. Then after creating a sample .dam file, you can run it which will compile to java bytecode. Then you can use the `javap` command with the created class file to execute the bytecode. 

# Features

`let`  
The "let" keyword allows you to declare variables: `let x = 3.0;`  
This stores the value 3.0 into x. You can also declare a variable without a value: `let y;`  

`print`  
You can print with the "print" keyword: `print "Hello, World!";`  
This will allow you to print strings and variables.  

`read`  
You can get input from the user with the "read" keyword: `read var;`  
When reading input from a user, you must declare a variable before hand. Then placing the variable as the argument will read that input as a string stored in that variable.  

`if`  
You can test conditions with the "if" keyword:  
```
let x = 3.0;
if (x > 0) {
   print "true"; 
}
```  
Additionally, the Dam Language allows for truthyness:
```
let x = 3.0;
if (x) {
    print "truthy";
}
```  
Truthy values are non-zero numbers and non-empty strings. 

`while`  
The "while" keyword allows users to perform loops:  
```
let x = 5;
while (x > 0) {
    print "positive";
    x = x - 1;
}
```  
Similar to if statements, while loops allow for truthy values. For loops are currently not implemented.

Functions are also not currently implemented but they are next on the plan.

Type Casting in this language:

`str`  
You can convert variables into strings with the "str" keyword: `let str_x = str(x);`  

`bool`  
You can convert vairables into booleans with the "bool" keyword: `let bool_y = bool(y);`  

`double`  
You can convert variable into doubles when applicable with the "double" keyword: `let double_z = double(z);`  

# Operations

This language allows for the basic arithmetic operations, but not exponentiation currently. 

You can use all the equality tests the same way as every other language.

The not operator is `!` instead of `not` as in python. 

You can also use the logical operators `and` and `or` for boolean operations. Currently you can still use them with truthy values, I am not sure if I want to take this out or not.
