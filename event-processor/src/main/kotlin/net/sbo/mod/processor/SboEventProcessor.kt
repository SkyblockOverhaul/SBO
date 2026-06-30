package net.sbo.mod.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStreamWriter

// @functionCalls will have 2 tab indent at .replace phase when replacing @ parameters

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

class SboEventProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("net.sbo.mod.utils.events.annotations.SboEvent")
        val generatedObjects = mutableSetOf<String>()

        val classSymbols = symbols.filterIsInstance<KSFunctionDeclaration>()
            .groupBy { it.parentDeclaration as? KSClassDeclaration }

        classSymbols.forEach { (clazz, functions) ->
            if (clazz == null) return@forEach
            val packageName = clazz.packageName.asString()
            val className = clazz.simpleName.asString()

            val isCompanionObject = clazz.classKind == ClassKind.OBJECT &&
                clazz.parentDeclaration is KSClassDeclaration

            val generatedClassName = if (isCompanionObject) {
                val enclosingClass = clazz.parentDeclaration as KSClassDeclaration
                "${enclosingClass.simpleName.asString()}_Companion"
            } else {
                className
            }

            val fileName = "${generatedClassName}_SboEventRegister"

            generatedObjects.add("$packageName.$fileName")

            val isObject = clazz.classKind == ClassKind.OBJECT
            if (!isObject) {
                logger.error("@SboEvent can only be used inside objects: ${clazz.simpleName.asString()}", clazz)
                return@forEach
            }

            val instanceRef = if (isCompanionObject) {
                val enclosingClass = clazz.parentDeclaration as KSClassDeclaration
                "${enclosingClass.simpleName.asString()}.Companion"
            } else {
                className
            }

            logger.info("Generating EventRegister for $packageName.$className with functions: ${functions.joinToString { it.simpleName.asString() }}")

            // Only collect KSFile objects that are not null
            val dependencies = functions.mapNotNull { it.containingFile }.distinct().toTypedArray<KSFile>()
            val file = codeGenerator.createNewFile(
                Dependencies(aggregating = true, sources = dependencies),
                packageName,
                fileName
            )

            OutputStreamWriter(file).use { writer ->
                val functionCalls = functions.joinToString("\n") { fn ->
                    val param = fn.parameters.firstOrNull()
                    if (null == param || 1 != fn.parameters.size) {
                        logger.error(
                            "@SboEvent functions must have exactly one parameter",
                            fn
                        )
                        return@joinToString ""
                    }
                    val paramType = param.type.resolve().declaration.qualifiedName?.asString()
                    val priority = fn.annotations.find { it.annotationType.resolve().declaration.qualifiedName?.asString() == "net.sbo.mod.utils.events.annotations.SboEvent" }
                        ?.arguments?.find { it.name?.getShortName() == "priority" }
                        ?.value?.toString()?.toIntOrNull() ?: 1
                    if (paramType == null) {
                        logger.error("Cannot resolve type for ${fn.simpleName.asString()}")
                        return@joinToString "// Cannot resolve type for ${fn.simpleName.asString()}"
                    } else {
                        "net.sbo.mod.utils.events.SBOEvent.on($paramType::class, $priority) { e -> $instanceRef.${fn.simpleName.asString()}(e) }"
                    }
                }

                writer.write(TEMPLATE_REGISTRAR
                    .replace("@packageName", packageName)
                    .replace("@fileName", fileName)
                    .replace("@functionCalls", functionCalls.lines().joinToString("\n") { "\t\t$it" })
                )
            }
        }

        if (generatedObjects.isNotEmpty()) {
            val allDependencies: List<KSFile> = classSymbols.keys.mapNotNull { it?.containingFile }
            val registryFile = codeGenerator.createNewFile(
                Dependencies(aggregating = true, sources = allDependencies.toTypedArray()),
                "net.sbo.mod.utils.events",
                "SboEventGeneratedRegistry"
            )

            OutputStreamWriter(registryFile).use { writer ->
                writer.write(TEMPLATE_REGISTRY
                    .replace("@functionCalls", generatedObjects.joinToString("\n") { "\t\t$it.register()" })
                )
            }
        }
        return emptyList()
    }
}
