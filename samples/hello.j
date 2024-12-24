.class public hello
.super java/lang/Object
.method public <init>()V
    aload_0
    invokenonvirtual java/lang/Object/<init>()V
    return
.end method
.method public static main([Ljava/lang/String;)V
    .limit stack 4
    .limit locals 3
	ldc "4.0s"
	invokestatic java/lang/Float/parseFloat(Ljava/lang/String;)F
	fstore 1
	getstatic java/lang/System/out Ljava/io/PrintStream;

	fload 1
	invokevirtual java/io/PrintStream/println(F)V
    return
.end method
