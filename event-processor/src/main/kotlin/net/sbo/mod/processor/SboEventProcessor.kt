package net.sbo.mod.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStreamWriter

private const val SBO_EVENT_ANNOTATION =
    "net.sbo.mod.utils.events.annotations.SboEvent"

private const val TEMPLATE_REGISTRAR = """package @packageName

object @fileName {
    fun register() {
@functionCalls
    }
}
"""

private const val TEMPLATE_REGISTRY = """package net.sbo.mod.utils.events

object SboEventGeneratedRegistry {
    fun registerAll() {
@functionCalls
    }
}
"""

private fun KSClassDeclaration.getClassHierarchy(): List<KSClassDeclaration> {
    val hierarchy = mutableListOf<KSClassDeclaration>()
    var current: KSClassDeclaration? = this

    while (current != null) {
        hierarchy.add(current)
        current = current.parentDeclaration as? KSClassDeclaration
    }

    return hierarchy.asReversed()
}

class SboEventProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val generatedObjects = mutableSetOf<String>()
    private val registrySources = mutableSetOf<KSFile>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(SBO_EVENT_ANNOTATION)

        val classSymbols = symbols.filterIsInstance<KSFunctionDeclaration>()
            .groupBy { it.parentDeclaration as? KSClassDeclaration }

        val deferredSymbols = mutableListOf<KSAnnotated>()

        classSymbols.forEach { (clazz, functions) ->
            if (clazz == null) {
                functions.forEach {
                    logger.error("@SboEvent can only be used inside objects", it)
                }
                return@forEach
            }

            if (clazz.classKind != ClassKind.OBJECT) {
                logger.error(
                    "@SboEvent can only be used inside objects: ${clazz.simpleName.asString()}",
                    clazz
                )
                return@forEach
            }

            val packageName = clazz.packageName.asString()
            val hierarchy = clazz.getClassHierarchy()

            val generatedClassName = "SboEvent_" + hierarchy.joinToString("_") {
                val name = it.simpleName.asString()
                "${name.length}_$name"
            }

            val fileName = "${generatedClassName}_SboEventRegister"
            val registrarName = "$packageName.$fileName"

            // Already generated in an earlier processing round.
            if (registrarName in generatedObjects) return@forEach

            val instanceRef = hierarchy.joinToString(".") {
                it.simpleName.asString()
            }

            val functionCalls = mutableListOf<String>()
            var hasInvalidFunctions = false
            var hasUnresolvedTypes = false

            functions.forEach { fn ->
                if (fn.parameters.size != 1) {
                    logger.error(
                        "@SboEvent functions must have exactly one parameter",
                        fn
                    )
                    hasInvalidFunctions = true
                    return@forEach
                }

                val paramType = fn.parameters.single().type.resolve()

                if (paramType.isError) {
                    hasUnresolvedTypes = true
                    return@forEach
                }

                val qualifiedName = paramType.declaration.qualifiedName?.asString()

                if (qualifiedName == null) {
                    logger.error(
                        "Cannot resolve type for ${fn.simpleName.asString()}",
                        fn
                    )
                    hasInvalidFunctions = true
                    return@forEach
                }

                val priority = fn.annotations.firstOrNull {
                    it.shortName.asString() == "SboEvent" &&
                        it.annotationType.resolve().declaration.qualifiedName?.asString() ==
                            SBO_EVENT_ANNOTATION
                }?.arguments?.find {
                    it.name?.getShortName() == "priority"
                }?.value?.toString()?.toIntOrNull() ?: 1

                functionCalls.add(
                    "net.sbo.mod.utils.events.SBOEvent.on($qualifiedName::class, $priority) " +
                        "{ e -> $instanceRef.${fn.simpleName.asString()}(e) }"
                )
            }

            if (hasUnresolvedTypes) {
                deferredSymbols.addAll(functions)
                return@forEach
            }

            if (hasInvalidFunctions) return@forEach

            val dependencies = functions.mapNotNullTo(mutableSetOf()) {
                it.containingFile
            }.toTypedArray()

            if (dependencies.isEmpty()) {
                logger.error("Cannot determine source dependencies for $registrarName", clazz)
                return@forEach
            }

            val file = codeGenerator.createNewFile(
                Dependencies(aggregating = false, sources = dependencies),
                packageName,
                fileName
            )

            OutputStreamWriter(file, Charsets.UTF_8).use { writer ->
                writer.write(
                    TEMPLATE_REGISTRAR
                        .replace("@packageName", packageName)
                        .replace("@fileName", fileName)
                        .replace(
                            "@functionCalls",
                            functionCalls.joinToString("\n") { "        $it" }
                        )
                )
            }

            // Only mark the registrar as generated after successful writing.
            generatedObjects.add(registrarName)
            registrySources.addAll(dependencies)
        }

        return deferredSymbols
    }

    override fun finish() {
        if (generatedObjects.isEmpty()) return

        val registryFile = codeGenerator.createNewFile(
            Dependencies(
                aggregating = true,
                sources = registrySources.toTypedArray()
            ),
            "net.sbo.mod.utils.events",
            "SboEventGeneratedRegistry"
        )

        OutputStreamWriter(registryFile, Charsets.UTF_8).use { writer ->
            writer.write(
                TEMPLATE_REGISTRY.replace(
                    "@functionCalls",
                    generatedObjects.sorted().joinToString("\n") {
                        "        $it.register()"
                    }
                )
            )
        }
    }
}

