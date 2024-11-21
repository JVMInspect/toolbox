package land.src.toolbox.jvm

import land.src.toolbox.jvm.oop.InstanceKlass
import land.src.toolbox.jvm.oop.Klass

class VMClasses(val scope: Scope) {

    val java_lang_Object: InstanceKlass by lazy { scope.universe.instanceKlass("java/lang/Object")!! }
    val java_lang_Class: InstanceKlass by lazy { scope.universe.instanceKlass("java/lang/Class")!! }
    val java_lang_String: InstanceKlass by lazy { scope.universe.instanceKlass("java/lang/String")!! }

    val intPrimitiveArray: Klass by lazy { scope.universe.klass("[I")!! }
    val bytePrimitiveArray: Klass by lazy { scope.universe.klass("[B")!! }
    val charPrimitiveArray: Klass by lazy { scope.universe.klass("[C")!! }
    val shortPrimitiveArray: Klass by lazy { scope.universe.klass("[S")!! }
    val longPrimitiveArray: Klass by lazy { scope.universe.klass("[J")!! }
    val floatPrimitiveArray: Klass by lazy { scope.universe.klass("[F")!! }
    val doublePrimitiveArray: Klass by lazy { scope.universe.klass("[D")!! }
    val booleanPrimitiveArray: Klass by lazy { scope.universe.klass("[Z")!! }

    val objectArray: Klass by lazy { scope.universe.klass("[Ljava/lang/Object;")!! }
    val stringArray: Klass by lazy { scope.universe.klass("[Ljava/lang/String;")!! }
    val classArray: Klass by lazy { scope.universe.klass("[Ljava/lang/Class;")!! }

}