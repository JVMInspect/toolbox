package land.src.jvm.api.oop

interface ClassLoaderData {
    val klasses: Klass?
    val next: ClassLoaderData?
}